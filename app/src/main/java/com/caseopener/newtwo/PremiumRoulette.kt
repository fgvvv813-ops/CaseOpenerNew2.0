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
    var finished by remember(reward.id) {mutableStateOf(false)}
    LaunchedEffect(reward.id,reducedMotion) {
        scroll.scrollToItem(0)
        if (reducedMotion) {
            delay(180)
            scroll.scrollToItem(35)
        } else {
            // Smooth movement in three phases. The server already chose
            // the reward; this animation never controls game outcomes.
            scroll.animateScrollToItem(10)
            delay(120)
            scroll.animateScrollToItem(25)
            delay(180)
            scroll.animateScrollToItem(35)
        }
        finished=true
    }
    Dialog(onDismissRequest={if(finished)onClose()}) {
        Surface(
            modifier=Modifier.fillMaxWidth().widthIn(max=540.dp),
            color=DarkBg,
            border=BorderStroke(1.dp,NeonViolet),
            shape=RoundedCornerShape(25.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(vertical=22.dp),
                horizontalAlignment=Alignment.CenterHorizontally
            ) {
                Text(if(finished)"ТВОЯ НАГРАДА!" else "ОТКРЫТИЕ КЕЙСА…",
                    color=if(finished)NeonGold else SoftWhite,
                    fontWeight=FontWeight.Black,fontSize=19.sp,
                    textAlign=TextAlign.Center)
                Text("▼",color=NeonGold,fontSize=22.sp)
                Box(
                    Modifier.fillMaxWidth().height(166.dp)
                        .background(Brush.verticalGradient(
                            listOf(Color(0xFF183740),Color(0xFF122833))))
                        .border(1.dp,NeonViolet.copy(alpha=0.75f))
                ) {
                    LazyRow(
                        state=scroll,userScrollEnabled=false,
                        modifier=Modifier.fillMaxSize(),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        contentPadding=PaddingValues(horizontal=11.dp)
                    ) {
                        itemsIndexed(sequence) { index,skin->
                            val hue=rarityHue(skin.rarity)
                            Surface(
                                color=if(index==36)Color(0xFF285560) else DarkPanel,
                                border=BorderStroke(1.dp,hue),
                                shape=RoundedCornerShape(13.dp),
                                modifier=Modifier.width(119.dp).height(137.dp)
                            ) {
                                Column(Modifier.padding(6.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally) {
                                    GameArt(skin.imageUrl,skin.emoji,
                                        Modifier.fillMaxWidth().height(89.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(skin.name,maxLines=2,
                                        overflow=TextOverflow.Ellipsis,
                                        color=SoftWhite,fontSize=10.sp,
                                        textAlign=TextAlign.Center)
                                }
                            }
                        }
                    }
                    Box(
                        Modifier.align(Alignment.Center).width(3.dp).fillMaxHeight()
                            .background(NeonGold.copy(alpha=0.65f))
                    )
                }
                Text("▲",color=NeonGold,fontSize=22.sp)
                if(finished) {
                    Spacer(Modifier.height(5.dp))
                    Text(reward.name,modifier=Modifier.padding(horizontal=12.dp),
                        color=rarityHue(reward.rarity),fontWeight=FontWeight.Black,
                        fontSize=19.sp,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    RarityTag(reward.rarity)
                    Spacer(Modifier.height(8.dp))
                    GoldPrice(reward.value,15)
                    Spacer(Modifier.height(17.dp))
                    GoldButton(
                        text="ЗАБРАТЬ НАГРАДУ",
                        onClick=onClose,
                        modifier=Modifier.fillMaxWidth().padding(horizontal=23.dp)
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(
                        modifier=Modifier.size(24.dp),
                        strokeWidth=2.dp,color=NeonViolet
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("Рулетка вращается…",color=PaleText,fontSize=12.sp)
                }
            }
        }
    }
}
