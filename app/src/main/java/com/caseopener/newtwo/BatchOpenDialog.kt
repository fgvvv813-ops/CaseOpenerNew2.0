package com.caseopener.newtwo

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun BatchOpenDialog(client:GameClient, reducedMotion:Boolean, onClose:()->Unit) {
    val target=client.batchOpenTarget
    if(target !in 2..5)return
    val drops=client.batchOpenRewards
    val done=client.batchOpenDone
    val interrupted=client.batchOpenInterrupted
    val total=drops.sumOf{it.value.toLong()}
    Dialog(
        onDismissRequest={if(done)onClose()},
        properties=DialogProperties(usePlatformDefaultWidth=false)
    ) {
        Surface(
            modifier=Modifier.fillMaxWidth(.94f).fillMaxHeight(.83f),
            color=DarkBg,shape=RoundedCornerShape(5.dp),
            border=BorderStroke(2.dp,Color(0xFF6384A4))
        ) {
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF2B4664),Color(0xFF132438))))
                    .padding(10.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)
            ) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ОТКРЫТИЕ КЕЙСОВ ×"+target,
                            fontSize=16.sp,fontWeight=FontWeight.Black,color=SoftWhite)
                        Text(
                            if(interrupted)"СЕРИЯ ПРЕРВАНА — ПРОВЕРЬ ИНВЕНТАРЬ"
                            else if(done)"ВСЕ НАГРАДЫ ПОДТВЕРЖДЕНЫ СЕРВЕРОМ"
                            else "ПОСЛЕДОВАТЕЛЬНОЕ ОТКРЫТИЕ",
                            color=if(interrupted)WarningRed else NeonBlue,fontSize=10.sp
                        )
                    }
                    Text(drops.size.toString()+" / "+target,
                        fontSize=17.sp,fontWeight=FontWeight.Black,color=NeonGold)
                }
                LinearProgressIndicator(
                    progress={drops.size.toFloat()/target},
                    modifier=Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color=Color(0xFF84B15E),trackColor=Color(0xFF101C2B)
                )
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(7.dp)
                ) {
                    repeat(target){index->
                        val item=drops.getOrNull(index)
                        val hue=item?.let{rarityHue(it.rarity)}?:Color(0xFF4C6E90)
                        Surface(
                            modifier=Modifier.weight(1f).fillMaxHeight(),
                            color=Color(0xFF263D54),shape=RoundedCornerShape(4.dp),
                            border=BorderStroke(1.dp,hue)
                        ) {
                            Crossfade(
                                targetState=item,
                                animationSpec=if(reducedMotion)snap() else tween(220),
                                modifier=Modifier.fillMaxSize(),
                                label="Подтверждённый предмет"
                            ) { shown ->
                                if(shown==null) {
                                    Column(Modifier.align(Alignment.Center),
                                        horizontalAlignment=Alignment.CenterHorizontally) {
                                        Text("◈",fontSize=28.sp,color=PaleText)
                                        Text(if(!done && index==drops.size)"ОТКРЫВАЕМ" else "ОЖИДАНИЕ",
                                            fontSize=9.sp,color=PaleText)
                                    }
                                } else {
                                    Column(Modifier.fillMaxSize()) {
                                        GameArt(shown.imageUrl,shown.emoji,
                                            Modifier.weight(1f).fillMaxWidth().padding(5.dp))
                                        Column(Modifier.fillMaxWidth().background(Color(0xFF132337))
                                            .padding(horizontal=5.dp,vertical=4.dp)) {
                                            Text(shown.name,fontSize=10.sp,color=SoftWhite,
                                                fontWeight=FontWeight.Bold,maxLines=1,
                                                overflow=TextOverflow.Ellipsis)
                                            Row(Modifier.fillMaxWidth(),
                                                verticalAlignment=Alignment.CenterVertically) {
                                                Text(shown.rarity,fontSize=9.sp,color=rarityHue(shown.rarity),
                                                    modifier=Modifier.weight(1f),
                                                    maxLines=1,overflow=TextOverflow.Ellipsis)
                                                Text("◈ "+shown.value,fontSize=10.sp,
                                                    color=NeonGold,fontWeight=FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(37.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    Text(
                        if(interrupted)"Соединение или баланс изменились. Проверь инвентарь."
                        else if(done)"Сумма полученных предметов: ◈ "+total
                        else "Ожидание подтверждения сервера…",
                        modifier=Modifier.weight(1f),fontSize=11.sp,
                        color=if(interrupted)WarningRed else PaleText,maxLines=2
                    )
                    if(done) {
                        Button(
                            onClick=onClose,shape=RoundedCornerShape(4.dp),
                            modifier=Modifier.height(35.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF588B36)),
                            contentPadding=PaddingValues(horizontal=15.dp)
                        ) {
                            Text("ЗАБРАТЬ ВСЁ",fontSize=11.sp,fontWeight=FontWeight.Black)
                        }
                    } else {
                        CircularProgressIndicator(modifier=Modifier.size(23.dp),
                            color=NeonGold,strokeWidth=2.dp)
                    }
                }
            }
        }
    }
}
