package com.caseopener.newtwo

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Optional FCM push configuration. Keep the SERVICE ACCOUNT JSON exclusively
 * on the Python API host. The Android values are Firebase public client config.
 *
 * With no Firebase project configured the app still builds and game/WebSocket
 * notifications continue to work. Closed-app push needs Firebase configured
 * on both the APK build and game server.
 */
class CaseOpenerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PushManager.initialize(this)
    }
}

internal object PushManager {
    private const val PREFS = "caseopener_push_v1"
    private const val TOKEN = "fcm_token"

    fun configured() = BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
        BuildConfig.FIREBASE_SENDER_ID.isNotBlank() &&
        BuildConfig.FIREBASE_APPLICATION_ID.isNotBlank() &&
        BuildConfig.FIREBASE_API_KEY.isNotBlank()

    fun initialize(context: Context) {
        // Must exist before background FCM notification payload can be shown.
        DeviceAlerts.ensureChannel(context)
        if (!configured()) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                    .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> saveToken(context, token) }
                .addOnFailureListener { e ->
                    Log.w("CaseOpenerPush", "FCM token unavailable", e)
                }
        } catch (e: Exception) {
            Log.w("CaseOpenerPush", "Firebase not configured correctly", e)
        }
    }

    fun saveToken(context: Context, token: String) {
        if (token.length in 32..4096) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(TOKEN, token).apply()
        }
    }

    fun token(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN, null)

    fun refreshToken(context: Context, onToken: (String) -> Unit) {
        token(context)?.let(onToken)
        if (!configured()) return
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { fresh ->
                    saveToken(context, fresh)
                    onToken(fresh)
                }
        } catch (_:Exception) {
            // WebSocket still delivers notifications while the app is running.
        }
    }
}

class CaseOpenerFirebaseService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushManager.saveToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val payload=message.notification
        val title=payload?.title ?: message.data["title"] ?: return
        val body=payload?.body ?: message.data["body"] ?: return
        // In the foreground WebSocket drives the live notification; a push
        // arrival is a fallback when the socket isn't currently active.
        if (AppVisibility.visible && AppVisibility.websocketConnected) return
        val id=message.data["notice_id"]?.toLongOrNull()
            ?: System.currentTimeMillis()
        DeviceAlerts.notify(
            applicationContext,
            GameNotice(id, message.data["kind"] ?: "push",
                title,body,System.currentTimeMillis()/1000,false)
        )
    }
}

internal object AppVisibility {
    @Volatile var visible: Boolean = false
    @Volatile var websocketConnected: Boolean = false
}
