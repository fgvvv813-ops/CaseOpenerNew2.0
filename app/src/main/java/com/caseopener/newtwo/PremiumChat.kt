package com.caseopener.newtwo

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

@Composable
internal fun PremiumChat(
    client: GameClient,
    modifier: Modifier,
    padding: Dp,
    onPlayer: (String) -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    var secondsLeft by remember { mutableIntStateOf(0) }
    val count = draft.trim().codePointCount(0, draft.trim().length)
    val canSend = client.status == "Онлайн" && !client.chatSending &&
        secondsLeft == 0 && count in 3..350
    DisposableEffect(client) {
        client.openChat()
        onDispose { client.closeChat() }
    }
    LaunchedEffect(client.status) {
        if (client.status == "Онлайн") client.openChat()
    }
    LaunchedEffect(client.chatCooldownUntil) {
        do {
            val remaining = (client.chatCooldownUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            secondsLeft = ceil(remaining / 1000.0).toInt()
            if (remaining > 0) delay(180)
        } while (remaining > 0)
    }
    LaunchedEffect(client.chatSentNonce) {
        if (client.chatSentNonce > 0) draft = ""
    }
    LaunchedEffect(client.chatMessages.lastOrNull()?.id) {
        if (client.chatMessages.isNotEmpty() && listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }
    Column(modifier.fillMaxWidth().padding(horizontal=padding)) {
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("💬 Общий чат",fontSize=22.sp,fontWeight=FontWeight.Black,
                    color=SoftWhite)
                Text("Один чат для всех игроков · сообщения в реальном времени",
                    fontSize=11.sp,color=PaleText,maxLines=2)
            }
            Surface(color=Color(0xFF1D3C31),
                border=BorderStroke(1.dp,Color(0xFF4EEB9C)),
                shape=RoundedCornerShape(10.dp)) {
                Text(if(client.status=="Онлайн")"● Онлайн" else "○ Офлайн",
                    fontSize=11.sp,
                    color=if(client.status=="Онлайн")Color(0xFF6AEEB1) else PaleText,
                    modifier=Modifier.padding(horizontal=8.dp,vertical=7.dp))
            }
        }
        Text("Нажми на ник игрока, чтобы посмотреть его профиль.",
            fontSize=11.sp,color=PaleText,
            modifier=Modifier.padding(bottom=7.dp))
        NeonPanel(modifier=Modifier.weight(1f).fillMaxWidth(),accent=NeonViolet) {
            if (client.chatMessages.isEmpty()) {
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        if(client.chatLoading) {
                            CircularProgressIndicator(color=NeonViolet,modifier=Modifier.size(32.dp))
                            Spacer(Modifier.height(9.dp))
                        } else {
                            Text("✦",color=NeonViolet,fontSize=38.sp)
                        }
                        Text(if(client.chatLoading)"Загрузка переписки…" else
                            "Сообщений пока нет. Напиши первым!",
                            color=PaleText,fontSize=12.sp,textAlign=TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier=Modifier.fillMaxSize(),
                    state=listState,
                    reverseLayout=true,
                    verticalArrangement=Arrangement.spacedBy(9.dp),
                    contentPadding=PaddingValues(vertical=4.dp)
                ) {
                    items(client.chatMessages.asReversed(),key={it.id}) {message ->
                        ChatBubble(
                            message=message,
                            own=message.userId==client.state?.userId,
                            onUser={onPlayer(message.userId)}
                        )
                    }
                    if (client.chatHasMore) {
                        item {
                            Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                                OutlinedButton(
                                    onClick={client.requestChatHistory(true)},
                                    enabled=!client.chatLoading && client.status=="Онлайн",
                                    border=BorderStroke(1.dp,NeonViolet),
                                    shape=RoundedCornerShape(12.dp)
                                ) {
                                    Text(if(client.chatLoading)"Загрузка…" else
                                        "Загрузить более ранние сообщения",
                                        color=NeonViolet,fontSize=11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        client.chatError?.let {
            Text(it,color=WarningRed,fontSize=11.sp,modifier=Modifier.padding(top=5.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(top=8.dp),
            verticalAlignment=Alignment.Bottom,
            horizontalArrangement=Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value=draft,
                onValueChange={newText->
                    if (newText.codePointCount(0,newText.length) <= 350) draft=newText
                },
                modifier=Modifier.weight(1f),
                placeholder={Text("Напиши сообщение…",color=PaleText,fontSize=13.sp)},
                minLines=1,maxLines=4,
                enabled=client.status=="Онлайн" && !client.chatSending,
                shape=RoundedCornerShape(16.dp),
                colors=OutlinedTextFieldDefaults.colors(
                    focusedBorderColor=NeonViolet,
                    unfocusedBorderColor=Color(0xFF38616C)
                )
            )
            GoldButton(
                text=if(client.chatSending)"…" else "➤",
                onClick={client.sendChat(draft)},
                enabled=canSend,
                modifier=Modifier.width(61.dp).padding(bottom=4.dp)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top=5.dp,bottom=8.dp),
            horizontalArrangement=Arrangement.SpaceBetween) {
            Text(if(secondsLeft>0) "Следующее сообщение через $secondsLeft с"
                 else if(client.chatSending) "Отправляем…"
                 else "Между сообщениями — 5 секунд",
                fontSize=10.sp,color=if(secondsLeft>0)NeonGold else PaleText)
            Text("$count / 350",fontSize=10.sp,
                color=if(count<3 && draft.isNotBlank())WarningRed else PaleText)
        }
    }
}

@Composable
private fun ChatBubble(message:ChatMessage,own:Boolean,onUser:()->Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement=if(own)Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier=Modifier.widthIn(max=340.dp).fillMaxWidth(0.88f),
            shape=RoundedCornerShape(
                topStart=16.dp,topEnd=16.dp,
                bottomEnd=if(own)4.dp else 16.dp,
                bottomStart=if(own)16.dp else 4.dp
            ),
            color=if(own)Color(0xFF204452) else Color(0xFF1B303C),
            border=BorderStroke(1.dp,
                if(own)NeonViolet.copy(alpha=0.7f) else Color(0xFF325460))
        ) {
            Column(Modifier.padding(horizontal=12.dp,vertical=9.dp),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(
                        if(own)"Вы" else message.username,
                        color=if(own)NeonGold else NeonBlue,
                        fontSize=12.sp,fontWeight=FontWeight.Black,
                        maxLines=1,overflow=TextOverflow.Ellipsis,
                        modifier=Modifier.weight(1f)
                            .clickable(enabled=message.userId.isNotBlank(),onClick=onUser)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        remember(message.createdAt) {
                            SimpleDateFormat("HH:mm",Locale.getDefault())
                                .format(Date(message.createdAt*1000L))
                        },
                        color=PaleText,fontSize=10.sp
                    )
                }
                Text(message.body,color=SoftWhite,fontSize=13.sp,
                    lineHeight=18.sp)
            }
        }
    }
}

@Composable
internal fun PremiumPlayerProfile(
    client: GameClient,
    modifier: Modifier,
    padding: Dp,
    onBack: () -> Unit,
    onTrade: (String) -> Unit
) {
    val p=client.selectedProfile
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=padding,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp)
    ) {
        TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)) {
            Text("‹ Назад в общий чат",color=NeonViolet)
        }
        SectionHeading("Профиль игрока")
        when {
            client.profileLoading -> {
                Box(Modifier.fillMaxWidth().padding(40.dp),contentAlignment=Alignment.Center) {
                    CircularProgressIndicator(color=NeonViolet)
                }
            }
            client.profileError!=null -> {
                Text(client.profileError.orEmpty(),color=WarningRed)
                GoldButton(text="Повторить загрузку",
                    onClick={client.retryPlayerProfile()},modifier=Modifier.fillMaxWidth())
            }
            p==null -> Text("Не удалось получить данные профиля",color=PaleText)
            else -> {
                NeonPanel(Modifier.fillMaxWidth(),accent=NeonViolet) {
                    Text("👤 "+p.username,color=SoftWhite,
                        fontSize=24.sp,fontWeight=FontWeight.Black)
                    Spacer(Modifier.height(13.dp))
                    ProfileInfoLine("🪙 Монеты",p.balance.toString())
                    ProfileInfoLine("▣ Открыто кейсов",p.openings.toString())
                    ProfileInfoLine("★ Уровень",progressLevel(p.openings).toString())
                    ProfileInfoLine(
                        "📅 Регистрация",
                        remember(p.registeredAt) {
                            if(p.registeredAt>0)
                                SimpleDateFormat("dd.MM.yyyy",Locale.getDefault())
                                    .format(Date(p.registeredAt*1000L))
                            else "Неизвестно"
                        }
                    )
                }
                if(p.id!=client.state?.userId) {
                    GoldButton(
                        text="⇄ Предложить трейд",
                        onClick={onTrade(p.id)},
                        modifier=Modifier.fillMaxWidth(),
                        enabled=client.status=="Онлайн"
                    )
                }
                SectionHeading("Лучший дроп")
                val best=p.bestDrop
                if(best==null) {
                    NeonPanel(modifier=Modifier.fillMaxWidth()) {
                        Text("Игрок пока не открывал кейсы",color=PaleText)
                    }
                } else {
                    NeonPanel(modifier=Modifier.fillMaxWidth(),accent=rarityHue(best.rarity)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            GameArt(best.imageUrl,best.emoji,
                                modifier=Modifier.size(104.dp),caseId=best.caseId)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(best.name,fontSize=17.sp,fontWeight=FontWeight.Black,
                                    color=SoftWhite)
                                Spacer(Modifier.height(7.dp))
                                RarityTag(best.rarity)
                                Spacer(Modifier.height(8.dp))
                                GoldPrice(best.value,15)
                            }
                        }
                    }
                }
                Text("Профили показывают игровые данные аккаунта.",
                    color=PaleText,fontSize=11.sp)
            }
        }
    }
}

@Composable
private fun ProfileInfoLine(label:String,value:String) {
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,color=PaleText,fontSize=13.sp)
        Text(value,color=SoftWhite,fontWeight=FontWeight.Bold,fontSize=14.sp)
    }
}
