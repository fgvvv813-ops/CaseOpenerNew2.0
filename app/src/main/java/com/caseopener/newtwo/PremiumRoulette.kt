package com.caseopener.newtwo

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
internal fun PremiumRoulette(
    reward:OwnedSkin,
    gameCase:GameCase?,
    reducedMotion:Boolean,
    onClose:()->Unit
) {
    val sequence=remember(reward.id) {
        val pool=gameCase?.skins.orEmpty().ifEmpty {
            listOf(GameSkin("default","Награда","Обычный",1,1,"✦",""))
        }
        val target=GameSkin("reward",reward.name,reward.rarity,
            reward.value,1,reward.emoji,reward.imageUrl)
        List(44) { index ->
            if(index==36) target else pool[Random.nextInt(pool.size)]
        }
    }
    val scroll=rememberLazyListState()
    var finished by remember(reward.id){mutableStateOf(false)}
    LaunchedEffect(reward.id,reducedMotion) {
        scroll.scrollToItem(0)
        if(reducedMotion) {
            delay(180)
            scroll.scrollToItem(35)
        } else {
            // Purely cosmetic: the server has already committed the actual drop.
            scroll.animateScrollToItem(11)
            delay(90)
            scroll.animateScrollToItem(23)
            delay(110)
            scroll.animateScrollToItem(35)
        }
        finished=true
    }
    Dialog(
        onDismissRequest={if(finished)onClose()},
        properties=DialogProperties(usePlatformDefaultWidth=false)
    ) {
        Surface(
            modifier=Modifier.fillMaxWidth(.94f).fillMaxHeight(.79f),
            shape=RoundedCornerShape(7.dp),
            color=DarkBg,
            border=BorderStroke(2.dp,NeonBlue.copy(alpha=.7f))
        ) {
            Row(Modifier.fillMaxSize().padding(10.dp),
                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(.72f).fillMaxHeight(),
                    verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("ОТКРЫТИЕ КЕЙСА",fontSize=17.sp,fontWeight=FontWeight.Black,
                            modifier=Modifier.weight(1f))
                        Text(gameCase?.name.orEmpty(),fontSize=11.sp,color=PaleText)
                    }
                    Box(Modifier.fillMaxWidth().weight(1f)
                        .border(1.dp,NeonBlue.copy(alpha=.6f),RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(
                            Color(0xFF2E4662),Color(0xFF111F32)))),contentAlignment=Alignment.Center) {
                        LazyRow(
                            state=scroll,userScrollEnabled=false,
                            modifier=Modifier.fillMaxSize(),
                            verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.spacedBy(6.dp),
                            contentPadding=PaddingValues(horizontal=6.dp)
                        ) {
                            itemsIndexed(sequence){index,skin->
                                val hue=rarityHue(skin.rarity)
                                Surface(
                                    modifier=Modifier.width(102.dp).fillMaxHeight(.88f),
                                    shape=RoundedCornerShape(4.dp),
                                    color=if(index==36)Color(0xFF395578) else DarkPanel,
                                    border=BorderStroke(1.dp,hue)
                                ) {
                                    Column(Modifier.padding(5.dp),
                                        horizontalAlignment=Alignment.CenterHorizontally) {
                                        GameArt(skin.imageUrl,skin.emoji,
                                            Modifier.weight(1f).fillMaxWidth())
                                        Text(skin.name,maxLines=1,overflow=TextOverflow.Ellipsis,
                                            fontSize=10.sp,color=SoftWhite)
                                    }
                                }
                            }
                        }
                        Box(Modifier.align(Alignment.Center).width(3.dp).fillMaxHeight()
                            .background(NeonGold))
                        Text("▼",fontSize=17.sp,color=NeonGold,
                            modifier=Modifier.align(Alignment.TopCenter))
                    }
                    Text(if(finished)"НАГРАДА ПОЛУЧЕНА" else "ПОДБИРАЕМ ПРЕДМЕТ…",
                        color=if(finished)NeonGold else PaleText,fontSize=11.sp,
                        fontWeight=FontWeight.Bold)
                }
                Column(
                    Modifier.weight(.28f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(6.dp)
                ) {
                    Text(if(finished)"ВАШ ПРЕДМЕТ" else "ОЖИДАНИЕ",
                        fontSize=13.sp,color=NeonGold,fontWeight=FontWeight.Black,
                        textAlign=TextAlign.Center)
                    if(finished) {
                        GameArt(reward.imageUrl,reward.emoji,
                            Modifier.fillMaxWidth().height(72.dp))
                        Text(reward.name,fontWeight=FontWeight.Black,fontSize=12.sp,
                            maxLines=2,textAlign=TextAlign.Center,
                            color=rarityHue(reward.rarity))
                        Text(reward.rarity,fontSize=11.sp,color=PaleText)
                        Text("◈ "+reward.value,fontSize=13.sp,
                            fontWeight=FontWeight.Black,color=NeonGold)
                        Button(
                            onClick=onClose,modifier=Modifier.fillMaxWidth().height(39.dp),
                            shape=RoundedCornerShape(4.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF558B36)),
                            contentPadding=PaddingValues(horizontal=4.dp)
                        ) {
                            Text("ЗАБРАТЬ",fontSize=12.sp,fontWeight=FontWeight.Black)
                        }
                    } else {
                        Spacer(Modifier.height(25.dp))
                        CircularProgressIndicator(modifier=Modifier.size(24.dp),
                            color=NeonGold,strokeWidth=2.dp)
                    }
                }
            }
        }
    }
}
