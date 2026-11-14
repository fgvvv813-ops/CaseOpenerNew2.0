package com.caseopener.newtwo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun BulkSaleDialog(client:GameClient,onClose:()->Unit) {
    val count=client.bulkSaleTotal
    if(count<=0) return
    val completed=client.bulkSaleCompleted
    val done=client.bulkSaleFinished
    val interrupted=client.bulkSaleInterrupted
    Dialog(
        onDismissRequest={if(done)onClose()},
        properties=DialogProperties(usePlatformDefaultWidth=false)
    ) {
        Surface(
            modifier=Modifier.fillMaxWidth(.72f).fillMaxHeight(.75f),
            color=DarkBg,shape=RoundedCornerShape(5.dp),
            border=BorderStroke(2.dp,if(interrupted)WarningRed else NeonBlue)
        ) {
            Column(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF304A67),Color(0xFF11243A)))
                ).padding(16.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    when {
                        interrupted -> "ПРОДАЖА ОСТАНОВЛЕНА"
                        done -> "ПРОДАЖА ЗАВЕРШЕНА"
                        else -> "ПРОДАЖА ИНВЕНТАРЯ"
                    },
                    color=if(interrupted)WarningRed else SoftWhite,
                    fontSize=18.sp,fontWeight=FontWeight.Black
                )
                Text("Продано предметов: "+completed+" / "+count,
                    color=SoftWhite,fontSize=14.sp,fontWeight=FontWeight.Bold)
                LinearProgressIndicator(
                    progress={completed.toFloat()/count},
                    modifier=Modifier.fillMaxWidth().height(7.dp),
                    color=Color(0xFF86C26C),trackColor=Color(0xFF14243A)
                )
                Text("Подтверждённая выручка: ◈ "+client.bulkSaleSoldValue,
                    color=NeonGold,fontSize=14.sp,fontWeight=FontWeight.Black)
                Text(
                    when {
                        interrupted -> "Не все продажи подтверждены. Сервер мог обработать последнее действие — сверь баланс и инвентарь, прежде чем повторять продажу."
                        done -> "Все выбранные предметы проданы. Баланс обновлён по данным игрового сервера."
                        else -> "Продажи выполняются по одной. Не закрывай приложение до завершения."
                    },color=if(interrupted)WarningRed else PaleText,
                    fontSize=12.sp,modifier=Modifier.weight(1f)
                )
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,
                    verticalAlignment=Alignment.CenterVertically) {
                    if(done) {
                        Button(
                            onClick=onClose,shape=RoundedCornerShape(4.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF588B36))
                        ) {Text("ПОНЯТНО",fontSize=12.sp,fontWeight=FontWeight.Black)}
                    } else {
                        CircularProgressIndicator(modifier=Modifier.size(24.dp),
                            color=NeonGold,strokeWidth=2.dp)
                    }
                }
            }
        }
    }
}
