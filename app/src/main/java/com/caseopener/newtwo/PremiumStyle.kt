package com.caseopener.newtwo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.math.min

internal val DarkBg = Color(0xFF08141D)
internal val DarkPanel = Color(0xFF122431)
internal val LighterPanel = Color(0xFF1A3441)
internal val NeonViolet = Color(0xFF65E3D4)
internal val NeonBlue = Color(0xFF75B8FA)
internal val NeonGold = Color(0xFFF5C777)
internal val PaleText = Color(0xFFA9BECA)
internal val SoftWhite = Color(0xFFF2F8FC)
internal val WarningRed = Color(0xFFFF8490)

internal val premiumScheme = darkColorScheme(
    primary = NeonGold,
    secondary = NeonViolet,
    tertiary = NeonBlue,
    background = DarkBg,
    surface = DarkPanel,
    onPrimary = DarkBg,
    onSecondary = SoftWhite,
    onBackground = SoftWhite,
    onSurface = SoftWhite
)

internal fun rarityHue(name: String): Color = when(name.lowercase()) {
    "легендарный", "legendary", "элитный" -> NeonGold
    "мифический", "mythic", "сверхредкий" -> Color(0xFFFF58BF)
    "эпический", "epic" -> NeonViolet
    "редкий", "rare" -> NeonBlue
    else -> Color(0xFFB4C3D1)
}

internal fun progressLevel(openings: Int): Int = openings.coerceAtLeast(0) / 20 + 1
internal fun progressFraction(openings: Int): Float = (openings.coerceAtLeast(0) % 20) / 20f

@Composable
internal fun NeonPanel(
    modifier: Modifier = Modifier,
    accent: Color = NeonViolet,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(21.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF18313B), DarkPanel)
                )
            )
            .border(1.dp, accent.copy(alpha=0.23f), RoundedCornerShape(21.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
internal fun SectionHeading(
    name: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(20.dp)
            .clip(RoundedCornerShape(5.dp)).background(NeonViolet))
        Spacer(Modifier.width(9.dp))
        Text(name, color = SoftWhite, fontSize = 18.sp,
            fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines=2)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal=4.dp)) {
                Text(action+" ›", color=NeonViolet, fontSize=12.sp, maxLines=1)
            }
        }
    }
}

@Composable
internal fun GoldButton(
    text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick=onClick, enabled=enabled, modifier=modifier.heightIn(min=49.dp),
        shape=RoundedCornerShape(14.dp),
        border=BorderStroke(1.dp, if(enabled) NeonGold.copy(alpha=0.45f) else PaleText.copy(alpha=0.2f)),
        colors=ButtonDefaults.buttonColors(
            containerColor=if(enabled) Color(0xFF665026) else Color(0xFF293940),
            contentColor=if(enabled) NeonGold else PaleText,
            disabledContainerColor=Color(0xFF293940),
            disabledContentColor=PaleText
        )
    ) { Text(text, fontWeight=FontWeight.ExtraBold, maxLines=1, fontSize=13.sp) }
}

@Composable
internal fun GoldPrice(value: Int, size: Int = 16) {
    Surface(
        color=Color(0xFF3B3020), shape=RoundedCornerShape(11.dp),
        border=BorderStroke(1.dp, NeonGold.copy(alpha=0.8f))
    ) {
        Text("🪙 "+value.toString(), color=NeonGold, fontWeight=FontWeight.Black,
            fontSize=size.sp, modifier=Modifier.padding(horizontal=10.dp, vertical=6.dp))
    }
}

@Composable
internal fun GameArt(
    imagePath: String, emoji: String,
    modifier: Modifier = Modifier,
    caseId: String = ""
) {
    val validUrl = mediaUrl(imagePath)
    if (validUrl != null) {
        AsyncImage(
            model=ImageRequest.Builder(LocalContext.current)
                .data(validUrl)
                .diskCacheKey(validUrl)
                .memoryCacheKey(validUrl)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription=null,
            modifier=modifier.clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF233E45)),
            contentScale=ContentScale.Crop
        )
        return
    }
    // The APK ships no case inventory or case artwork. The server owns all
    // cases/skins, including their image URLs; emoji is a neutral placeholder.
    Box(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1F4B58),Color(0xFF10232E)))),
        contentAlignment=Alignment.Center
    ) {
        Text(emoji, fontSize=52.sp, color=SoftWhite)
    }
}

@Composable
internal fun RarityTag(name: String) {
    val tint=rarityHue(name)
    Surface(
        color=tint.copy(alpha=0.12f),
        shape=RoundedCornerShape(8.dp),
        border=BorderStroke(1.dp,tint.copy(alpha=0.66f))
    ) {
        Text(name, color=tint, fontWeight=FontWeight.Bold,
            fontSize=10.sp, maxLines=1,
            modifier=Modifier.padding(horizontal=6.dp,vertical=4.dp))
    }
}
