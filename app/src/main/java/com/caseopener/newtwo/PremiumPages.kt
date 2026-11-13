package com.caseopener.newtwo

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.util.Locale
import kotlin.math.max

@Composable
internal fun PremiumHome(
    client: GameClient,
    modifier: Modifier,
    padding: androidx.compose.ui.unit.Dp,
    onOpenCases: () -> Unit,
    onCase: (String) -> Unit,
    onProfile: () -> Unit,
    onNotes: () -> Unit
) {
    val current=client.state
    val cases=client.cases
    LazyColumn(
        modifier.fillMaxWidth().padding(horizontal=padding),
        verticalArrangement=Arrangement.spacedBy(14.dp),
        contentPadding=PaddingValues(top=8.dp,bottom=24.dp)
    ) {
        item {
            BoxWithConstraints(
                Modifier.fillMaxWidth().heightIn(min=200.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF214953),Color(0xFF14323E),Color(0xFF2A2F30)))
                    )
                    .border(1.dp,NeonViolet.copy(alpha=0.28f),RoundedCornerShape(23.dp))
            ) {
                val compact=maxWidth < 430.dp
                Row(Modifier.fillMaxWidth().padding(if(compact)18.dp else 24.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(if(compact)1f else 1.2f)) {
                        Text("Твоя коллекция начинается здесь",fontSize=if(compact)22.sp else 27.sp,
                            color=SoftWhite,fontWeight=FontWeight.Black,maxLines=2)
                        Text("CASEOPENER 2.0",color=NeonGold,
                            fontSize=if(compact)13.sp else 16.sp,
                            fontWeight=FontWeight.Black,maxLines=2)
                        Spacer(Modifier.height(8.dp))
                        Text("Открывай коллекции, собирай редкие предметы и обменивайся с игроками.",
                            color=SoftWhite,fontSize=12.sp,maxLines=4)
                        Spacer(Modifier.height(13.dp))
                        GoldButton(text="Перейти к кейсам  ›",onClick=onOpenCases,
                            modifier=Modifier.fillMaxWidth())
                    }
                    if(!compact) {
                        Spacer(Modifier.width(15.dp))
                        val featured=cases.firstOrNull()
                        GameArt(
                            imagePath=featured?.imageUrl.orEmpty(),
                            emoji=featured?.emoji ?: "✦",
                            modifier=Modifier.weight(0.86f).height(176.dp),
                            caseId=featured?.id.orEmpty()
                        )
                    }
                }
            }
        }
        item { SectionHeading("Популярные кейсы",action="Смотреть все",onAction=onOpenCases) }
        item {
            if(cases.isEmpty()) {
                Text("Кейсы ещё не добавлены",color=PaleText,fontSize=13.sp)
            } else {
                LazyRow(
                    contentPadding=PaddingValues(bottom=5.dp),
                    horizontalArrangement=Arrangement.spacedBy(10.dp)
                ) {
                    items(cases.take(8),key={it.id}) { gameCase ->
                        HomeCaseCard(gameCase) {onCase(gameCase.id)}
                    }
                }
            }
        }
        item { SectionHeading("Последние награды",action="Мой профиль",onAction=onProfile) }
        item {
            if(current?.inventory.isNullOrEmpty()) {
                NeonPanel(modifier=Modifier.fillMaxWidth()) {
                    Text("Пока нет наград",fontWeight=FontWeight.Bold)
                    Text("Открой свой первый кейс, чтобы начать коллекцию.",
                        color=PaleText,fontSize=12.sp)
                }
            } else {
                LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp),
                    contentPadding=PaddingValues(bottom=4.dp)) {
                    items(current!!.inventory.take(8),key={it.id}) { skin ->
                        RewardPreview(skin)
                    }
                }
            }
        }
        item { SectionHeading("Быстрая статистика") }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                HomeStat("▣","Кейсы",(current?.openings?:0).toString(),
                    NeonViolet,Modifier.weight(1f))
                HomeStat("♛","Скины",(current?.inventory?.size?:0).toString(),
                    NeonGold,Modifier.weight(1f))
                val legendary=current?.inventory?.count {
                    it.rarity.equals("легендарный",ignoreCase=true)
                } ?: 0
                HomeStat("✦","Легенды",legendary.toString(),
                    NeonBlue,Modifier.weight(1f))
            }
        }

    }
}

@Composable
private fun HomeCaseCard(gameCase:GameCase,onClick:()->Unit) {
    Surface(
        color=DarkPanel,
        shape=RoundedCornerShape(18.dp),
        border=BorderStroke(1.dp,when(gameCase.id.lowercase()) {
            "neon"->NeonBlue
            "legend"->NeonGold
            else->NeonViolet
        }.copy(alpha=0.8f)),
        modifier=Modifier.width(171.dp).clickable(onClick=onClick)
    ) {
        Column(Modifier.padding(9.dp)) {
            GameArt(gameCase.imageUrl,gameCase.emoji,
                Modifier.fillMaxWidth().height(119.dp),caseId=gameCase.id)
            Spacer(Modifier.height(8.dp))
            Text(gameCase.name,fontSize=15.sp,fontWeight=FontWeight.Black,
                maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(gameCase.subtitle,fontSize=10.sp,color=PaleText,
                maxLines=1,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            GoldPrice(gameCase.cost,13)
        }
    }
}

@Composable
private fun RewardPreview(skin:OwnedSkin) {
    val hue=rarityHue(skin.rarity)
    Surface(
        modifier=Modifier.width(133.dp),
        color=DarkPanel,shape=RoundedCornerShape(15.dp),
        border=BorderStroke(1.dp,hue)
    ) {
        Column(Modifier.padding(8.dp)) {
            GameArt(skin.imageUrl,skin.emoji,Modifier.fillMaxWidth().height(77.dp))
            Spacer(Modifier.height(6.dp))
            Text(skin.name,fontSize=11.sp,color=SoftWhite,maxLines=2,
                overflow=TextOverflow.Ellipsis,minLines=2)
            Text(skin.rarity,color=hue,fontSize=10.sp,maxLines=1)
        }
    }
}

@Composable
private fun HomeStat(symbol:String,label:String,value:String,color:Color,modifier:Modifier) {
    Surface(color=DarkPanel,shape=RoundedCornerShape(14.dp),
        border=BorderStroke(1.dp,color.copy(alpha=0.6f)),modifier=modifier) {
        Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(symbol,fontSize=20.sp,color=color)
            Text(value,fontSize=17.sp,fontWeight=FontWeight.Black)
            Text(label,color=PaleText,fontSize=10.sp,maxLines=1)
        }
    }
}

@Composable
internal fun PremiumCases(
    client:GameClient, modifier:Modifier,
    padding: androidx.compose.ui.unit.Dp,
    onCase:(String)->Unit
) {
    var filter by rememberSaveable {mutableStateOf("Все")}
    val playerBalance=client.state?.balance ?: 0
    val raw=client.cases
    val results=remember(raw,filter,playerBalance) {
        when(filter) {
            "Популярные"->raw.sortedByDescending {it.skins.size}
            "Легендарные"->raw.filter { case ->
                case.skins.any { it.rarity.equals("легендарный",ignoreCase=true) }
            }
            "Доступные"->raw.filter {it.cost<=playerBalance}
            else->raw
        }
    }
    LazyVerticalGrid(
        columns=GridCells.Adaptive(minSize=156.dp),
        modifier=modifier.fillMaxWidth().padding(horizontal=padding),
        horizontalArrangement=Arrangement.spacedBy(11.dp),
        verticalArrangement=Arrangement.spacedBy(11.dp),
        contentPadding=PaddingValues(top=8.dp,bottom=25.dp)
    ) {
        item(span={GridItemSpan(maxLineSpan)}) {
            Column(Modifier.fillMaxWidth()) {
                Text("▣  Все кейсы",fontSize=26.sp,fontWeight=FontWeight.Black)
                Text("Выбери коллекцию, чтобы посмотреть все возможные награды.",
                    color=PaleText,fontSize=12.sp)
                Spacer(Modifier.height(11.dp))
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    items(listOf("Все","Популярные","Легендарные","Доступные")){option->
                        FilterChip(
                            selected=filter==option,
                            onClick={filter=option},
                            label={Text(option,fontSize=12.sp,
                                fontWeight=if(filter==option)FontWeight.Bold else FontWeight.Normal)},
                            shape=RoundedCornerShape(11.dp),
                            colors=FilterChipDefaults.filterChipColors(
                                selectedContainerColor=Color(0xFF3D3C2B),
                                selectedLabelColor=NeonGold,
                                containerColor=DarkPanel,
                                labelColor=PaleText
                            ),
                            border=FilterChipDefaults.filterChipBorder(
                                enabled=true,selected=filter==option,
                                borderColor=Color(0xFF355560),
                                selectedBorderColor=NeonGold
                            )
                        )
                    }
                }
            }
        }
        items(results,key={it.id}) { case ->
            Surface(
                modifier=Modifier.fillMaxWidth().clickable { onCase(case.id) },
                color=DarkPanel,
                shape=RoundedCornerShape(20.dp),
                border=BorderStroke(1.dp, when {
                    case.id.contains("legend",ignoreCase=true)->NeonGold
                    case.id.contains("neon",ignoreCase=true)->NeonBlue
                    else->NeonViolet
                }.copy(alpha=0.76f))
            ) {
                Column(Modifier.padding(10.dp)) {
                    GameArt(case.imageUrl,case.emoji,
                        Modifier.fillMaxWidth().height(134.dp),case.id)
                    Spacer(Modifier.height(8.dp))
                    Text(case.name,fontSize=17.sp,fontWeight=FontWeight.Black,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(case.subtitle,fontSize=11.sp,color=PaleText,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Spacer(Modifier.height(9.dp))
                    GoldPrice(case.cost,14)
                }
            }
        }
        if(results.isEmpty()) {
            item(span={GridItemSpan(maxLineSpan)}) {
                Text("По этому фильтру кейсов пока нет.",
                    modifier=Modifier.fillMaxWidth().padding(vertical=45.dp),
                    color=PaleText,textAlign=TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun PremiumCaseDetail(
    client:GameClient,
    gameCase:GameCase,
    modifier:Modifier,
    padding: androidx.compose.ui.unit.Dp,
    onBack:()->Unit
) {
    val totalWeight=gameCase.skins.sumOf { it.weight.coerceAtLeast(0) }.coerceAtLeast(1)
    LazyVerticalGrid(
        columns=GridCells.Adaptive(minSize=142.dp),
        modifier=modifier.fillMaxWidth().padding(horizontal=padding),
        horizontalArrangement=Arrangement.spacedBy(10.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),
        contentPadding=PaddingValues(top=1.dp,bottom=25.dp)
    ) {
        item(span={GridItemSpan(maxLineSpan)}) {
            Column {
                TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)) {
                    Text("‹  Назад к кейсам",color=PaleText)
                }
                NeonPanel(
                    modifier=Modifier.fillMaxWidth(),
                    accent=if(gameCase.id.contains("legend",ignoreCase=true))NeonGold else NeonViolet
                ) {
                    GameArt(gameCase.imageUrl,gameCase.emoji,
                        Modifier.fillMaxWidth().height(188.dp),gameCase.id)
                    Spacer(Modifier.height(8.dp))
                    Text(gameCase.name,fontWeight=FontWeight.Black,fontSize=25.sp,
                        color=SoftWhite)
                    Text(gameCase.subtitle,color=PaleText,fontSize=13.sp)
                    Spacer(Modifier.height(8.dp))
                    GoldPrice(gameCase.cost,17)
                    Spacer(Modifier.height(12.dp))
                    GoldButton(
                        text=if(client.opening)"ОТКРЫВАЕМ…" else "✦  ОТКРЫТЬ КЕЙС  »",
                        modifier=Modifier.fillMaxWidth(),
                        enabled=client.status=="Онлайн" && !client.opening &&
                            !client.selling && gameCase.skins.isNotEmpty() &&
                            (client.state?.balance?:0)>=gameCase.cost,
                        onClick={client.openCase(gameCase.id)}
                    )
                    if((client.state?.balance?:0)<gameCase.cost) {
                        Text("Недостаточно монет. Продай ненужные скины в инвентаре.",
                            color=NeonGold,fontSize=11.sp,
                            modifier=Modifier.padding(top=8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                SectionHeading("Возможные скины и награды")
                Text("Шансы указаны для каждого предмета.",color=PaleText,fontSize=12.sp)
            }
        }
        items(gameCase.skins,key={it.id}) {skin->
            SkinRewardTile(skin, totalWeight)
        }
        if(gameCase.skins.isEmpty()) {
            item(span={GridItemSpan(maxLineSpan)}) {
                Text("Пока в кейсе нет доступных скинов.",
                    modifier=Modifier.fillMaxWidth().padding(30.dp),
                    color=PaleText,textAlign=TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SkinRewardTile(skin:GameSkin,totalWeight:Int) {
    val hue=rarityHue(skin.rarity)
    Surface(
        modifier=Modifier.fillMaxWidth(),
        color=DarkPanel,
        shape=RoundedCornerShape(17.dp),
        border=BorderStroke(1.dp,hue.copy(alpha=0.85f))
    ) {
        Column(Modifier.padding(9.dp)) {
            GameArt(skin.imageUrl,skin.emoji,
                Modifier.fillMaxWidth().height(115.dp))
            Spacer(Modifier.height(7.dp))
            Text(skin.name,fontWeight=FontWeight.Bold,fontSize=12.sp,
                maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis)
            RarityTag(skin.rarity)
            Spacer(Modifier.height(7.dp))
            Text("🪙 "+skin.value,fontWeight=FontWeight.Bold,
                color=NeonGold,fontSize=12.sp)
            Text(String.format(Locale.US,"Шанс: %.2f%%",
                    100.0*skin.weight.coerceAtLeast(0)/totalWeight),
                color=PaleText,fontSize=11.sp)
        }
    }
}

@Composable
internal fun PremiumInventory(
    client:GameClient,
    modifier:Modifier,
    padding: androidx.compose.ui.unit.Dp
) {
    var query by rememberSaveable {mutableStateOf("")}
    var filter by rememberSaveable {mutableStateOf("Все")}
    var sort by rememberSaveable {mutableStateOf("Новые")}
    var page by rememberSaveable {mutableIntStateOf(0)}
    val raw=client.state?.inventory.orEmpty()
    val results=remember(raw,query,filter,sort) {
        val filtered=raw.filter { skin->
            val match=skin.name.contains(query.trim(),ignoreCase=true)
            match && when(filter) {
                "Редкие" -> skin.rarity.equals("редкий",ignoreCase=true)
                "Эпические" -> skin.rarity.equals("эпический",ignoreCase=true)
                "Легендарные" -> skin.rarity.equals("легендарный",ignoreCase=true)
                else ->true
            }
        }
        when(sort) {
            "Цена ↓"->filtered.sortedByDescending {it.value}
            "Цена ↑"->filtered.sortedBy {it.value}
            "Редкость"->filtered.sortedByDescending {
                when(it.rarity.lowercase()) {
                    "легендарный"->5; "мифический"->4
                    "эпический"->3; "редкий"->2; else->1
                }
            }
            else->filtered
        }
    }
    val pages=(results.size+17)/18
    val current=page.coerceIn(0,(pages-1).coerceAtLeast(0))
    val itemsOnPage=results.drop(current*18).take(18)
    LaunchedEffect(query,filter,sort) {page=0}
    LazyVerticalGrid(
        columns=GridCells.Adaptive(minSize=151.dp),
        modifier=modifier.fillMaxWidth().padding(horizontal=padding),
        horizontalArrangement=Arrangement.spacedBy(10.dp),
        verticalArrangement=Arrangement.spacedBy(11.dp),
        contentPadding=PaddingValues(top=6.dp,bottom=25.dp)
    ) {
        item(span={GridItemSpan(maxLineSpan)}) {
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                SectionHeading("Инвентарь")
                Text("Всего предметов: "+raw.size,color=PaleText,fontSize=12.sp)
                OutlinedTextField(
                    value=query,onValueChange={query=it.take(80)},
                    placeholder={Text("⌕  Поиск по инвентарю…",color=PaleText)},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true,shape=RoundedCornerShape(13.dp)
                )
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    items(listOf("Все","Редкие","Эпические","Легендарные")){option->
                        FilterChip(
                            selected=filter==option,onClick={filter=option},
                            label={Text(option,fontSize=11.sp)},
                            shape=RoundedCornerShape(12.dp)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Сортировка",color=PaleText,fontSize=11.sp,
                        modifier=Modifier.weight(1f))
                    var menu by remember {mutableStateOf(false)}
                    Box {
                        OutlinedButton(onClick={menu=true},
                            contentPadding=PaddingValues(horizontal=10.dp,vertical=4.dp)) {
                            Text(sort+" ▾",fontSize=11.sp)
                        }
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                            listOf("Новые","Редкость","Цена ↓","Цена ↑").forEach {option->
                                DropdownMenuItem(text={Text(option)},onClick={sort=option;menu=false})
                            }
                        }
                    }
                }
            }
        }
        items(itemsOnPage,key={it.id}) {skin->
            InventoryTile(skin,busy=client.status!="Онлайн" || client.selling || client.opening,
                onSell={client.sellItem(skin)})
        }
        if(itemsOnPage.isEmpty()) {
            item(span={GridItemSpan(maxLineSpan)}) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical=45.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ) {
                    Text("◆",fontSize=44.sp,color=NeonViolet)
                    Text("Скины не найдены",color=PaleText)
                }
            }
        }
        if(pages>1) {
            item(span={GridItemSpan(maxLineSpan)}) {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.Center,
                    verticalAlignment=Alignment.CenterVertically) {
                    OutlinedButton(onClick={page=(current-1).coerceAtLeast(0)},enabled=current>0) {
                        Text("‹")
                    }
                    Spacer(Modifier.width(10.dp))
                    Text((current+1).toString()+" / "+pages.toString(),
                        color=NeonGold,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick={page=(current+1).coerceAtMost(pages-1)},enabled=current<pages-1) {
                        Text("›")
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryTile(skin:OwnedSkin,busy:Boolean,onSell:()->Unit) {
    val hue=rarityHue(skin.rarity)
    Surface(
        color=DarkPanel,shape=RoundedCornerShape(18.dp),
        border=BorderStroke(1.dp,hue.copy(alpha=0.79f)),
        modifier=Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(9.dp)) {
            GameArt(skin.imageUrl,skin.emoji,
                Modifier.fillMaxWidth().height(112.dp))
            Spacer(Modifier.height(7.dp))
            RarityTag(skin.rarity)
            Spacer(Modifier.height(6.dp))
            Text(skin.name,fontWeight=FontWeight.Bold,fontSize=13.sp,
                minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text("🪙 "+skin.value,color=NeonGold,fontSize=12.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            OutlinedButton(
                onClick=onSell,enabled=!busy,
                modifier=Modifier.fillMaxWidth().heightIn(min=38.dp),
                contentPadding=PaddingValues(horizontal=5.dp,vertical=3.dp),
                shape=RoundedCornerShape(10.dp),
                border=BorderStroke(1.dp,NeonGold)
            ) {
                Text("Продать",color=if(!busy)NeonGold else PaleText,fontSize=11.sp)
            }
        }
    }
}

@Composable
internal fun PremiumProfile(
    client:GameClient,
    modifier:Modifier,
    padding: androidx.compose.ui.unit.Dp,
    onLogout:()->Unit,
    onSettings:()->Unit,
    onNotes:()->Unit
) {
    val player=client.state ?: return
    val legendary=player.inventory.count {
        it.rarity.equals("легендарный",ignoreCase=true)
    }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=padding,vertical=9.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ) {
        SectionHeading("Профиль")
        NeonPanel(
            modifier=Modifier.fillMaxWidth(),
            accent=NeonViolet
        ) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter=androidx.compose.ui.res.painterResource(R.drawable.ic_launcher),
                    contentDescription=null,
                    modifier=Modifier.size(83.dp).clip(RoundedCornerShape(25.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(player.username,fontSize=23.sp,fontWeight=FontWeight.Black,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("●  "+if(client.status=="Онлайн")"Онлайн" else "Нет связи",
                        color=if(client.status=="Онлайн")Color(0xFF59E687) else NeonGold,
                        fontSize=12.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Уровень "+progressLevel(player.openings),
                        fontWeight=FontWeight.Bold,fontSize=13.sp)
                    LinearProgressIndicator(
                        progress={progressFraction(player.openings)},
                        modifier=Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(9.dp)),
                        color=NeonViolet,trackColor=Color(0xFF453659)
                    )
                    Text((player.openings%20).toString()+" / 20 открытий до следующего уровня",
                        color=PaleText,fontSize=10.sp)
                }
            }
            Spacer(Modifier.height(15.dp))
            GoldPrice(player.balance,16)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ProfileNumber("▣",player.openings.toString(),"Открыто",Modifier.weight(1f))
                ProfileNumber("♛",player.inventory.size.toString(),"Предметы",Modifier.weight(1f))
                ProfileNumber("✦",legendary.toString(),"Легенды",Modifier.weight(1f))
            }
        }
        SectionHeading("Моя коллекция")
        NeonPanel(modifier=Modifier.fillMaxWidth(),accent=NeonGold) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Стоимость всех скинов",color=PaleText,fontSize=13.sp)
                Text("🪙 "+player.inventory.sumOf {it.value},
                    fontWeight=FontWeight.Bold,color=NeonGold)
            }
            Spacer(Modifier.height(9.dp))
            Text("☁  Прогресс автоматически сохраняется в твоём аккаунте.",
                color=PaleText,fontSize=12.sp)
        }
        OutlinedButton(
            onClick=onSettings,modifier=Modifier.fillMaxWidth().heightIn(min=47.dp),
            shape=RoundedCornerShape(13.dp)
        ) {Text("⚙  Настройки приложения")}
        OutlinedButton(
            onClick=onNotes,modifier=Modifier.fillMaxWidth().heightIn(min=47.dp),
            shape=RoundedCornerShape(13.dp)
        ) {Text("✦  Что нового?")}
        OutlinedButton(
            onClick=onLogout,modifier=Modifier.fillMaxWidth().heightIn(min=49.dp),
            shape=RoundedCornerShape(13.dp),
            border=BorderStroke(1.dp,WarningRed)
        ) {Text("⇥  Выйти из аккаунта",color=WarningRed,fontWeight=FontWeight.Bold)}
        Text("CaseOpener New 2.0 • "+BuildConfig.VERSION_NAME,
            modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center,
            color=PaleText,fontSize=11.sp)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ProfileNumber(icon:String,value:String,label:String,modifier:Modifier) {
    Surface(
        color=Color(0xFF30213D),shape=RoundedCornerShape(12.dp),
        modifier=modifier
    ) {
        Column(Modifier.padding(horizontal=3.dp,vertical=10.dp),
            horizontalAlignment=Alignment.CenterHorizontally) {
            Text(icon,fontSize=20.sp,color=NeonGold)
            Text(value,fontSize=15.sp,fontWeight=FontWeight.Black)
            Text(label,fontSize=10.sp,color=PaleText,maxLines=1)
        }
    }
}
