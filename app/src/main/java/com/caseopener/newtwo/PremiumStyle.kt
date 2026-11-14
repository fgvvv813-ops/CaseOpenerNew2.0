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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
import kotlin.math.max
import kotlin.math.min

internal val DarkBg = Color(0xFF0D1B2B)
internal val DarkPanel = Color(0xFF1C344F)
internal val LighterPanel = Color(0xFF335472)
internal val NeonViolet = Color(0xFF68B7E7)
internal val NeonBlue = Color(0xFF9DCBF4)
internal val NeonGold = Color(0xFFF4C35D)
internal val PaleText = Color(0xFFB3C7DA)
internal val SoftWhite = Color(0xFFF4F7FC)
internal val WarningRed = Color(0xFFFF7186)

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
    "мифический", "mythic", "сверхредкий" -> Color(0xFFE84D63)
    "эпический", "epic" -> Color(0xFFB56DE1)
    "редкий", "rare" -> NeonBlue
    else -> Color(0xFFB4C3D1)
}

internal fun progressLevel(openings: Int): Int = openings.coerceAtLeast(0) / 20 + 1
internal fun progressFraction(openings: Int): Float = (openings.coerceAtLeast(0) % 20) / 20f

// Low-contrast CS-inspired hexagonal texture: decorative only, never intercepts taps.
@Composable
internal fun HexPatternBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius=20.dp.toPx()
        val stepX=radius*3f
        val stepY=radius*1.73205f
        val rows=(size.height/stepY).toInt()+2
        val columns=(size.width/stepX).toInt()+2
        for(row in 0..rows) for(column in 0..columns) {
            val cx=column*stepX+(if(row%2==0) 0f else radius*1.5f)
            val cy=row*stepY
            val vertices=(0..5).map { index ->
                val angle=Math.PI/3.0*index
                Offset(cx+(radius.toDouble()*cos(angle)).toFloat(),cy+(radius.toDouble()*sin(angle)).toFloat())
            }
            for(i in 0..5) drawLine(
                color=Color(0xFF7AA1C4).copy(alpha=0.24f),
                start=vertices[i],end=vertices[(i+1)%6],strokeWidth=1.dp.toPx()
            )
        }
    }
}

@Composable
internal fun NeonPanel(
    modifier: Modifier = Modifier,
    accent: Color = NeonViolet,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF273A52), DarkPanel)
                )
            )
            .border(1.dp, accent.copy(alpha=0.38f), RoundedCornerShape(12.dp))
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
        shape=RoundedCornerShape(8.dp),
        border=BorderStroke(1.dp, if(enabled) NeonGold.copy(alpha=0.45f) else PaleText.copy(alpha=0.2f)),
        colors=ButtonDefaults.buttonColors(
            containerColor=if(enabled) Color(0xFF476C34) else Color(0xFF293940),
            contentColor=if(enabled) SoftWhite else PaleText,
            disabledContainerColor=Color(0xFF263449),
            disabledContentColor=PaleText
        )
    ) { Text(text, fontWeight=FontWeight.ExtraBold, maxLines=1, fontSize=13.sp) }
}

@Composable
internal fun GoldPrice(value: Int, size: Int = 16) {
    Surface(
        color=Color(0xFF3B3427), shape=RoundedCornerShape(8.dp),
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
            modifier=modifier.clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF667789)),
            contentScale=ContentScale.Fit
        )
        return
    }
    // The APK ships no case inventory or case artwork. The server owns all
    // cases/skins, including their image URLs; emoji is a neutral placeholder.
    Box(
        modifier.clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF687788),Color(0xFF374C65)))),
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
