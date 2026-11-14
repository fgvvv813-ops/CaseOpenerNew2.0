package com.caseopener.newtwo

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// The original case-opening game's landscape hierarchy: a slim player HUD,
// horizontal case showcase, dark navy metallic panels, and compact bottom tabs.
// All inventory, currency, case art and outcomes are still sourced from GameClient.
private val FrameBorder = Color(0xFF426080)
private val FrameInterior = Color(0xFF1C304A)
private val ActionGreen = Color(0xFF558B36)

@Composable
internal fun LandscapeHeader(
    client: GameClient,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit
) {
    val state=client.state
    Row(
        Modifier.fillMaxWidth().height(52.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xFF314F6D),Color(0xFF14253B))))
            .border(BorderStroke(1.dp,FrameBorder))
            .padding(horizontal=12.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.ic_launcher),null,
            Modifier.size(36.dp).clip(RoundedCornerShape(5.dp)).clickable(onClick=onProfile))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.widthIn(min=112.dp,max=168.dp).clickable(onClick=onProfile)) {
            Text(state?.username.orEmpty(),fontSize=14.sp,fontWeight=FontWeight.Black,
                maxLines=1,overflow=TextOverflow.Ellipsis,color=SoftWhite)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("LVL "+progressLevel(state?.openings?:0),fontSize=10.sp,color=NeonGold)
                Spacer(Modifier.width(6.dp))
                LinearProgressIndicator(
                    progress={progressFraction(state?.openings?:0)},
                    modifier=Modifier.width(65.dp).height(5.dp).clip(RoundedCornerShape(2.dp)),
                    color=Color(0xFF53B8EE),trackColor=Color(0xFF091523)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text("CASE OPENER",fontWeight=FontWeight.Black,fontSize=18.sp,
            color=SoftWhite,maxLines=1,modifier=Modifier.weight(1f))
        Text("●",fontSize=9.sp,color=if(client.status=="Онлайн")Color(0xFF76D75A) else WarningRed)
        Spacer(Modifier.width(10.dp))
        Surface(
            color=Color(0xFF233B50),border=BorderStroke(1.dp,NeonGold.copy(alpha=.65f)),
            shape=RoundedCornerShape(5.dp)
        ) {
            Text("◈  "+(state?.balance?:0).toString(),fontSize=14.sp,
                color=NeonGold,fontWeight=FontWeight.Black,
                modifier=Modifier.padding(horizontal=13.dp,vertical=6.dp),maxLines=1)
        }
        Spacer(Modifier.width(9.dp))
        LandscapeHudButton("🔔",client.unreadNotices>0,onNotifications)
        Spacer(Modifier.width(6.dp))
        LandscapeHudButton("⚙",false,onSettings)
    }
}

@Composable
private fun LandscapeHudButton(label:String,alert:Boolean,onClick:()->Unit) {
    Box {
        Surface(
            color=Color(0xFF2B4663),border=BorderStroke(1.dp,FrameBorder),
            shape=RoundedCornerShape(5.dp),
            modifier=Modifier.size(34.dp).clickable(onClick=onClick)
        ) {Box(contentAlignment=Alignment.Center){Text(label,fontSize=17.sp)}}
        if(alert) Box(
            Modifier.size(7.dp).align(Alignment.TopEnd)
                .background(NeonGold, RoundedCornerShape(20.dp))
        )
    }
}

@Composable
internal fun LandscapeNavigation(
    selected:String,unread:Int,pending:Int,onSelect:(String)->Unit
) {
    val options=listOf(
        Triple("home","⌂","ГЛАВНАЯ"),
        Triple("cases","▣","КЕЙСЫ"),
        Triple("inventory","♧","ИНВЕНТАРЬ"),
        Triple("trades","⇄","ОБМЕНЫ"),
        Triple("chat","✉","ЧАТ"),
        Triple("menu","☰","МЕНЮ")
    )
    Row(
        Modifier.fillMaxWidth().height(51.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF294461),Color(0xFF132237))))
            .border(BorderStroke(1.dp,FrameBorder)),
        verticalAlignment=Alignment.CenterVertically
    ) {
        options.forEach { (key,symbol,title) ->
            val active=selected==key || (selected=="detail" && key=="cases") ||
                (selected=="player" && key=="chat") ||
                (selected=="profile" && key=="menu")
            val count=when(key) {"chat"->unread;"trades"->pending;else->0}
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if(active)Color(0xFF456B93) else Color.Transparent)
                    .clickable {onSelect(key)}
                    .padding(top=3.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.Center
            ) {
                Text(symbol+(if(count>0) " •" else ""),fontSize=20.sp,
                    color=if(active)NeonGold else SoftWhite,lineHeight=20.sp,
                    fontWeight=FontWeight.Bold)
                Text(title,fontSize=9.sp,maxLines=1,
                    fontWeight=if(active)FontWeight.Black else FontWeight.Medium,
                    color=if(active)SoftWhite else PaleText)
            }
        }
    }
}

@Composable
private fun LandscapePanel(
    modifier:Modifier=Modifier,
    accent:Color=FrameBorder,
    onClick:(()->Unit)?=null,
    content:@Composable ColumnScope.()->Unit
) {
    val base=modifier.clip(RoundedCornerShape(5.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF344B67),FrameInterior)))
        .border(1.dp,accent.copy(alpha=.85f),RoundedCornerShape(5.dp))
    Column(if(onClick!=null)base.clickable(onClick=onClick) else base,content=content)
}

@Composable
internal fun LandscapeHome(
    client:GameClient,
    modifier:Modifier=Modifier,
    onCases:()->Unit,
    onCase:(String)->Unit,
    onInventory:()->Unit,
    onTrades:()->Unit,
    onChat:()->Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal=12.dp,vertical=7.dp)) {
        val compact=maxHeight<245.dp
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("CASE OPENER",fontWeight=FontWeight.Black,color=SoftWhite,
                    fontSize=if(compact)16.sp else 19.sp,modifier=Modifier.weight(1f))
                Text("КОЛЛЕКЦИЯ КЕЙСОВ",color=PaleText,fontSize=10.sp)
                Spacer(Modifier.width(12.dp))
                Text("ВСЕ КЕЙСЫ  ›",Modifier.clickable(onClick=onCases),
                    color=NeonGold,fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            if(client.cases.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
                    Text("На сервере пока нет кейсов",color=PaleText)
                }
            } else {
                LazyRow(
                    modifier=Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement=Arrangement.spacedBy(9.dp)
                ) {
                    items(client.cases,key={it.id}) { gameCase ->
                        LandscapeCaseCard(gameCase,onClick={onCase(gameCase.id)})
                    }
                }
            }
            if(!compact) {
                Row(Modifier.fillMaxWidth().height(42.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    LandscapeShortcut("♧","МОЙ ИНВЕНТАРЬ",Modifier.weight(1f),onInventory)
                    LandscapeShortcut("⇄","ОБМЕНЫ",Modifier.weight(1f),onTrades)
                    LandscapeShortcut("✉","ОБЩИЙ ЧАТ",Modifier.weight(1f),onChat)
                }
            }
        }
    }
}

@Composable
private fun LandscapeShortcut(glyph:String,title:String,modifier:Modifier,onClick:()->Unit) {
    LandscapePanel(modifier=modifier,onClick=onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){
            Text(glyph,fontSize=23.sp,color=NeonGold)
            Spacer(Modifier.width(9.dp))
            Text(title,fontWeight=FontWeight.Bold,color=SoftWhite,fontSize=11.sp,maxLines=1)
        }
    }
}

@Composable
private fun LandscapeCaseCard(gameCase:GameCase,onClick:()->Unit) {
    LandscapePanel(Modifier.width(156.dp).fillMaxHeight(),onClick=onClick) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(5.dp)) {
            GameArt(gameCase.imageUrl,gameCase.emoji,Modifier.fillMaxSize(),gameCase.id)
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF122237))
            .padding(horizontal=8.dp,vertical=4.dp)) {
            Text(gameCase.name,fontSize=12.sp,fontWeight=FontWeight.Black,
                maxLines=1,overflow=TextOverflow.Ellipsis)
            Text("◈ "+gameCase.cost,fontSize=11.sp,color=NeonGold,
                fontWeight=FontWeight.Bold,maxLines=1)
        }
        Box(
            Modifier.fillMaxWidth().height(26.dp)
                .background(ActionGreen).clickable(onClick=onClick),
            contentAlignment=Alignment.Center
        ) {
            Text("ПОСМОТРЕТЬ",fontSize=10.sp,color=Color.White,fontWeight=FontWeight.Black)
        }
    }
}

@Composable
internal fun LandscapeCatalog(
    client:GameClient,modifier:Modifier,onCase:(String)->Unit
) {
    var filter by rememberSaveable {mutableStateOf("Все")}
    val balance=client.state?.balance?:0
    val cases=when(filter) {
        "Доступные"->client.cases.filter {it.cost<=balance}
        "Легендарные"->client.cases.filter {c->c.skins.any {it.rarity.equals("легендарный",true)}}
        else->client.cases
    }
    Column(modifier.fillMaxSize().padding(horizontal=12.dp,vertical=7.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("КЕЙСЫ",fontWeight=FontWeight.Black,fontSize=18.sp,
                modifier=Modifier.weight(1f))
            listOf("Все","Доступные","Легендарные").forEach {option->
                val active=filter==option
                Surface(
                    color=if(active)Color(0xFF46688B) else DarkPanel,
                    border=BorderStroke(1.dp,if(active)NeonGold else FrameBorder),
                    shape=RoundedCornerShape(4.dp),
                    modifier=Modifier.padding(start=5.dp).clickable {filter=option}
                ) {
                    Text(option,fontSize=10.sp,color=if(active)SoftWhite else PaleText,
                        modifier=Modifier.padding(horizontal=12.dp,vertical=5.dp))
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        if(cases.isEmpty()){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                Text("Кейсов в этой категории нет",color=PaleText)
            }
        } else {
            LazyVerticalGrid(
                columns=GridCells.Adaptive(144.dp),
                modifier=Modifier.fillMaxSize(),
                horizontalArrangement=Arrangement.spacedBy(9.dp),
                verticalArrangement=Arrangement.spacedBy(9.dp),
                contentPadding=PaddingValues(bottom=8.dp)
            ) {
                items(cases,key={it.id}) { gameCase->
                    LandscapePanel(Modifier.fillMaxWidth().height(158.dp),onClick={onCase(gameCase.id)}) {
                        GameArt(gameCase.imageUrl,gameCase.emoji,
                            Modifier.fillMaxWidth().weight(1f).padding(5.dp),gameCase.id)
                        Text(gameCase.name,fontSize=12.sp,fontWeight=FontWeight.Bold,
                            modifier=Modifier.padding(horizontal=8.dp),
                            maxLines=1,overflow=TextOverflow.Ellipsis)
                        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=5.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            Text("◈ "+gameCase.cost,fontSize=11.sp,color=NeonGold,
                                fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
                            Text("ОТКРЫТЬ ›",fontSize=10.sp,color=Color(0xFF9BDB81))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LandscapeCaseDetail(
    client:GameClient, gameCase:GameCase, modifier:Modifier,
    onBack:()->Unit
) {
    val totalWeight=gameCase.skins.sumOf{it.weight.coerceAtLeast(0)}.coerceAtLeast(1)
    Row(modifier.fillMaxSize().padding(9.dp),
        horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        LandscapePanel(Modifier.weight(.37f).fillMaxHeight(),accent=NeonGold) {
            Row(Modifier.fillMaxWidth().height(31.dp).padding(horizontal=7.dp),
                verticalAlignment=Alignment.CenterVertically){
                Text("‹ НАЗАД",color=NeonGold,fontSize=11.sp,
                    modifier=Modifier.clickable(onClick=onBack))
                Spacer(Modifier.width(8.dp))
                Text(gameCase.name,fontSize=14.sp,fontWeight=FontWeight.Black,
                    maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            }
            GameArt(gameCase.imageUrl,gameCase.emoji,
                Modifier.weight(1f).fillMaxWidth().padding(horizontal=7.dp),gameCase.id)
            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=5.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Text("◈ "+gameCase.cost,color=NeonGold,fontWeight=FontWeight.Black,
                    fontSize=13.sp,modifier=Modifier.weight(1f))
                Button(
                    onClick={client.openCase(gameCase.id)},
                    enabled=client.status=="Онлайн" && !client.opening && !client.selling &&
                        gameCase.skins.isNotEmpty() && (client.state?.balance?:0)>=gameCase.cost,
                    modifier=Modifier.height(34.dp),
                    contentPadding=PaddingValues(horizontal=12.dp),
                    shape=RoundedCornerShape(4.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=ActionGreen)
                ) {
                    Text(if(client.opening)"ОТКРЫВАЕМ…" else "ОТКРЫТЬ КЕЙС",
                        fontSize=11.sp,fontWeight=FontWeight.Black)
                }
            }
        }
        Column(Modifier.weight(.63f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(bottom=5.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Text("ВОЗМОЖНЫЕ НАГРАДЫ",fontSize=14.sp,fontWeight=FontWeight.Black,
                    modifier=Modifier.weight(1f))
                Text(gameCase.skins.size.toString()+" предметов",fontSize=10.sp,color=PaleText)
            }
            LazyVerticalGrid(
                columns=GridCells.Adaptive(114.dp),
                modifier=Modifier.fillMaxSize(),
                horizontalArrangement=Arrangement.spacedBy(7.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp)
            ) {
                items(gameCase.skins,key={it.id}) {skin->
                    val tint=rarityHue(skin.rarity)
                    LandscapePanel(Modifier.fillMaxWidth().height(141.dp),accent=tint) {
                        GameArt(skin.imageUrl,skin.emoji,
                            Modifier.fillMaxWidth().weight(1f).padding(5.dp))
                        Text(skin.name,modifier=Modifier.padding(horizontal=6.dp),
                            fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis,
                            fontWeight=FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().padding(horizontal=6.dp,vertical=4.dp)){
                            Text("◈ "+skin.value,fontSize=10.sp,color=NeonGold,
                                modifier=Modifier.weight(1f))
                            Text(String.format(java.util.Locale.US,"%.1f%%",
                                skin.weight.coerceAtLeast(0)*100.0/totalWeight),
                                fontSize=10.sp,color=tint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LandscapeMenu(
    modifier:Modifier=Modifier,
    onPage:(String)->Unit,
    onNotes:()->Unit
) {
    Column(modifier.fillMaxSize().padding(12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("МЕНЮ ИГРОКА",fontSize=18.sp,fontWeight=FontWeight.Black)
        Row(Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple("inventory","♧","ИНВЕНТАРЬ"),
                Triple("trades","⇄","ОБМЕНЫ"),
                Triple("profile","♟","ПРОФИЛЬ"),
                Triple("chat","✉","ЧАТ")
            ).forEach { (key,icon,label) ->
                LandscapePanel(Modifier.weight(1f).fillMaxHeight(),
                    accent=if(key=="trades")NeonGold else FrameBorder,onClick={onPage(key)}) {
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(icon,fontSize=40.sp,color=NeonGold)
                            Text(label,fontSize=13.sp,fontWeight=FontWeight.Black,
                                color=SoftWhite,textAlign=TextAlign.Center)
                        }
                    }
                }
            }
        }
        TextButton(onClick=onNotes) {
            Text("Что нового в версии "+BuildConfig.VERSION_NAME,
                fontSize=11.sp,color=PaleText)
        }
    }
}

@Composable
internal fun LandscapeLogin(client:GameClient) {
    val scope=rememberCoroutineScope()
    var create by rememberSaveable{mutableStateOf(false)}
    var username by rememberSaveable{mutableStateOf("")}
    var password by remember{mutableStateOf("")}
    var visible by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=10.dp),
        horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.weight(.52f).fillMaxHeight(),
            verticalArrangement=Arrangement.Center,
            horizontalAlignment=Alignment.Start
        ) {
            Text("CASE OPENER",fontSize=32.sp,fontWeight=FontWeight.Black,
                color=SoftWhite,maxLines=1)
            Text("N E W   2 . 0",fontSize=15.sp,color=NeonGold,
                fontWeight=FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text("КЕЙСЫ  •  СКИНЫ  •  ОБМЕНЫ",fontSize=12.sp,
                fontWeight=FontWeight.Bold,color=NeonBlue,maxLines=1)
            Spacer(Modifier.height(6.dp))
            Text("Коллекция, баланс и прогресс сохраняются в твоём аккаунте.",
                fontSize=12.sp,color=PaleText,maxLines=3)
        }
        LandscapePanel(Modifier.weight(.48f).fillMaxHeight()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal=12.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(6.dp)
            ) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf(false,true).forEach {mode->
                        val active=create==mode
                        Surface(
                            color=if(active)Color(0xFF4A7198) else DarkPanel,
                            border=BorderStroke(1.dp,if(active)NeonGold else FrameBorder),
                            shape=RoundedCornerShape(4.dp),
                            modifier=Modifier.weight(1f).clickable {create=mode}
                        ) {
                            Text(if(mode)"РЕГИСТРАЦИЯ" else "ВХОД",fontSize=11.sp,
                                color=SoftWhite,textAlign=TextAlign.Center,
                                modifier=Modifier.padding(7.dp))
                        }
                    }
                }
                OutlinedTextField(
                    value=username,onValueChange={username=it.take(24)},label={Text("Логин")},
                    modifier=Modifier.fillMaxWidth(),singleLine=true,
                    shape=RoundedCornerShape(5.dp)
                )
                OutlinedTextField(
                    value=password,onValueChange={password=it.take(128)},
                    label={Text("Пароль")},singleLine=true,modifier=Modifier.fillMaxWidth(),
                    visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon={TextButton(onClick={visible=!visible}){
                        Text(if(visible)"Скрыть" else "Показать",fontSize=10.sp)
                    }},
                    shape=RoundedCornerShape(5.dp)
                )
                client.error?.let {Text(it,color=WarningRed,fontSize=11.sp)}
                Button(
                    onClick={scope.launch{client.authenticate(create,username,password)}},
                    enabled=!client.busy && username.trim().length in 3..24 && password.length>=8,
                    colors=ButtonDefaults.buttonColors(containerColor=ActionGreen),
                    shape=RoundedCornerShape(4.dp),modifier=Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(if(client.busy)"ПОДКЛЮЧЕНИЕ…" else if(create)"СОЗДАТЬ АККАУНТ" else "ВОЙТИ",
                        fontWeight=FontWeight.Black,fontSize=12.sp)
                }
                Text("Тестовый сервер пока без HTTPS. Используй отдельный пароль.",
                    fontSize=10.sp,color=NeonGold)
            }
        }
    }
}


@Composable
internal fun LandscapeInventory(client:GameClient,modifier:Modifier=Modifier) {
    var query by rememberSaveable{mutableStateOf("")}
    var filter by rememberSaveable{mutableStateOf("Все")}
    var sort by rememberSaveable{mutableStateOf("Новые")}
    var page by rememberSaveable{mutableIntStateOf(0)}
    val raw=client.state?.inventory.orEmpty()
    val filtered=remember(raw,query,filter,sort) {
        val matches=raw.filter { skin ->
            skin.name.contains(query.trim(),ignoreCase=true) && when(filter) {
                "Редкие"->skin.rarity.equals("редкий",true)
                "Эпические"->skin.rarity.equals("эпический",true)
                "Легендарные"->skin.rarity.equals("легендарный",true)
                else->true
            }
        }
        when(sort) {
            "Цена ↓"->matches.sortedByDescending{it.value}
            "Цена ↑"->matches.sortedBy{it.value}
            "Редкость"->matches.sortedByDescending{
                when(it.rarity.lowercase()) {
                    "легендарный"->5;"мифический"->4;"эпический"->3;"редкий"->2;else->1
                }
            }
            else->matches
        }
    }
    val pages=(filtered.size+23)/24
    val current=page.coerceIn(0,(pages-1).coerceAtLeast(0))
    val visible=filtered.drop(current*24).take(24)
    LaunchedEffect(query,filter,sort){page=0}
    Column(modifier.fillMaxSize().padding(horizontal=11.dp,vertical=6.dp)) {
        Row(
            Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(6.dp)
        ) {
            Text("ИНВЕНТАРЬ",fontSize=15.sp,fontWeight=FontWeight.Black)
            Text(raw.size.toString(),color=NeonGold,fontSize=11.sp)
            androidx.compose.foundation.text.BasicTextField(
                value=query,onValueChange={query=it.take(80)},singleLine=true,
                textStyle=androidx.compose.ui.text.TextStyle(color=SoftWhite,fontSize=12.sp),
                modifier=Modifier.weight(1f).height(31.dp).clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF15283D)).border(1.dp,FrameBorder,RoundedCornerShape(4.dp))
                    .padding(horizontal=8.dp,vertical=7.dp),
                decorationBox={inner->
                    Box {
                        if(query.isEmpty())Text("⌕ Найти скин…",color=PaleText,fontSize=12.sp)
                        inner()
                    }
                }
            )
            listOf("Все","Редкие","Эпические","Легендарные").forEach {option->
                val active=filter==option
                Surface(
                    color=if(active)Color(0xFF496D94) else DarkPanel,
                    border=BorderStroke(1.dp,if(active)NeonGold else FrameBorder),
                    shape=RoundedCornerShape(3.dp),
                    modifier=Modifier.clickable{filter=option}
                ) { Text(option,fontSize=10.sp,color=SoftWhite,
                    modifier=Modifier.padding(horizontal=8.dp,vertical=7.dp)) }
            }
            val sorts=listOf("Новые","Редкость","Цена ↓","Цена ↑")
            Text("⇅ "+sort,modifier=Modifier.clickable{
                sort=sorts[(sorts.indexOf(sort)+1)%sorts.size]
            }.padding(5.dp),fontSize=10.sp,color=NeonGold,maxLines=1)
        }
        Spacer(Modifier.height(7.dp))
        if(visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                Text("В инвентаре нет подходящих предметов",color=PaleText)
            }
        } else {
            LazyVerticalGrid(
                columns=GridCells.Adaptive(129.dp),
                modifier=Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(7.dp),
                verticalArrangement=Arrangement.spacedBy(7.dp),
                contentPadding=PaddingValues(bottom=7.dp)
            ) {
                items(visible,key={it.id}){skin->
                    val accent=rarityHue(skin.rarity)
                    LandscapePanel(Modifier.fillMaxWidth().height(143.dp),accent=accent) {
                        GameArt(skin.imageUrl,skin.emoji,
                            Modifier.weight(1f).fillMaxWidth().padding(4.dp))
                        Text(skin.name,fontSize=10.sp,fontWeight=FontWeight.Bold,
                            maxLines=1,overflow=TextOverflow.Ellipsis,
                            modifier=Modifier.padding(horizontal=6.dp))
                        Row(Modifier.fillMaxWidth().padding(4.dp),
                            verticalAlignment=Alignment.CenterVertically){
                            Text("◈ "+skin.value,color=NeonGold,fontSize=10.sp,
                                modifier=Modifier.weight(1f))
                            Surface(
                                color=Color(0xFF385777),shape=RoundedCornerShape(3.dp),
                                border=BorderStroke(1.dp,FrameBorder),
                                modifier=Modifier.clickable(
                                    enabled=client.status=="Онлайн" && !client.opening && !client.selling
                                ){client.sellItem(skin)}
                            ) {
                                Text("ПРОДАТЬ",color=SoftWhite,fontSize=9.sp,
                                    modifier=Modifier.padding(horizontal=6.dp,vertical=4.dp))
                            }
                        }
                    }
                }
            }
        }
        if(pages>1) {
            Row(Modifier.fillMaxWidth().height(29.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.Center){
                Text("‹ НАЗАД",color=if(current>0)NeonGold else PaleText,
                    fontSize=11.sp,modifier=Modifier.clickable(enabled=current>0){page=current-1})
                Spacer(Modifier.width(13.dp))
                Text((current+1).toString()+" / "+pages,fontSize=11.sp,color=SoftWhite)
                Spacer(Modifier.width(13.dp))
                Text("ДАЛЕЕ ›",color=if(current<pages-1)NeonGold else PaleText,
                    fontSize=11.sp,
                    modifier=Modifier.clickable(enabled=current<pages-1){page=current+1})
            }
        }
    }
}

@Composable
internal fun LandscapeProfile(
    client:GameClient,modifier:Modifier=Modifier,
    onLogout:()->Unit,onSettings:()->Unit,onNotes:()->Unit
) {
    val player=client.state?:return
    val legendary=player.inventory.count{it.rarity.equals("легендарный",true)}
    Row(modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        LandscapePanel(Modifier.weight(1f).fillMaxHeight(),accent=NeonBlue) {
            Row(Modifier.fillMaxSize().padding(14.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_launcher),null,
                    Modifier.size(86.dp).clip(RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(player.username,fontSize=20.sp,fontWeight=FontWeight.Black,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("УРОВЕНЬ "+progressLevel(player.openings),color=NeonGold,fontSize=12.sp)
                    LinearProgressIndicator(
                        progress={progressFraction(player.openings)},
                        modifier=Modifier.fillMaxWidth().height(6.dp),
                        color=NeonBlue,trackColor=DarkBg
                    )
                    Text("◈ "+player.balance,color=NeonGold,fontSize=15.sp,fontWeight=FontWeight.Black)
                }
            }
        }
        LandscapePanel(Modifier.weight(1f).fillMaxHeight(),accent=FrameBorder) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Text("СТАТИСТИКА",fontSize=15.sp,fontWeight=FontWeight.Black)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("Открыто кейсов: "+player.openings,color=SoftWhite,fontSize=12.sp)
                    Text("Предметов: "+player.inventory.size,color=SoftWhite,fontSize=12.sp)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("Легендарных: "+legendary,color=SoftWhite,fontSize=12.sp)
                    Text("Коллекция: ◈ "+player.inventory.sumOf{it.value},
                        color=NeonGold,fontSize=12.sp)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick=onSettings,modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)) {
                        Text("НАСТРОЙКИ",fontSize=10.sp)
                    }
                    OutlinedButton(onClick=onNotes,modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)) {
                        Text("ОБНОВЛЕНИЯ",fontSize=10.sp)
                    }
                    OutlinedButton(onClick=onLogout,modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=5.dp,vertical=0.dp)) {
                        Text("ВЫХОД",fontSize=10.sp,color=WarningRed)
                    }
                }
                Text("CaseOpener New 2.0 • "+BuildConfig.VERSION_NAME,
                    fontSize=10.sp,color=PaleText)
            }
        }
    }
}
