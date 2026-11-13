package com.caseopener.newtwo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val Background = Color(0xFF0A0713)
private val CardColor = Color(0xFF1B1429)
private val Violet = Color(0xFFC177FF)
private val Gold = Color(0xFFFFD079)
private val SecondaryText = Color(0xFFBEB5CB)
private val Danger = Color(0xFFFFB3C2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CaseOpenerApp() }
    }
}

@Composable
private fun CaseOpenerApp() {
    val context = LocalContext.current
    val client = remember { GameClient(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var destination by rememberSaveable { mutableStateOf("home") }
    var selectedCaseId by rememberSaveable { mutableStateOf("") }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(client) { onDispose { client.close() } }

    val palette = darkColorScheme(
        primary=Violet, secondary=Gold, background=Background, surface=CardColor,
        onSurface=Color.White, onBackground=Color.White
    )
    MaterialTheme(colorScheme=palette) {
        Box(Modifier.fillMaxSize().background(Background).statusBarsPadding().navigationBarsPadding()) {
            if (client.token == null) {
                LoginScreen(client)
            } else if (client.state == null) {
                ConnectScreen(
                    status=client.status,
                    message=client.error,
                    onRetry={ client.reconnect() },
                    onLogout={scope.launch {client.logout(); destination="home"}}
                )
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 710.dp
                    val padding = if(maxWidth < 370.dp) 12.dp else 17.dp
                    Column(
                        Modifier.fillMaxSize().widthIn(max=1120.dp).align(Alignment.TopCenter)
                    ) {
                        GameHeader(client, padding, onProfile={destination="profile"})
                        if (client.status != "Онлайн") {
                            Text(
                                "Нет связи с сервером — данные сохранятся, действия временно недоступны",
                                color=Gold, fontSize=11.sp, textAlign=TextAlign.Center,
                                modifier=Modifier.fillMaxWidth().padding(horizontal=padding,vertical=4.dp)
                            )
                        }
                        if (client.error != null) {
                            Text(
                                client.error.orEmpty(),
                                color=Danger, fontSize=12.sp,
                                modifier=Modifier.fillMaxWidth().padding(horizontal=padding,vertical=6.dp)
                            )
                        }
                        when (destination) {
                            "home" -> HomeScreen(
                                client=client,
                                modifier=Modifier.weight(1f),
                                padding=padding,
                                onCases={destination="cases"},
                                onCase={selectedCaseId=it; destination="detail"},
                                onNotes={showNotes=true}
                            )
                            "cases" -> CasesScreen(
                                cases=client.cases,
                                modifier=Modifier.weight(1f),
                                padding=padding,
                                onCase={selectedCaseId=it; destination="detail"}
                            )
                            "detail" -> {
                                val gameCase=client.cases.firstOrNull { it.id==selectedCaseId }
                                if (gameCase == null) {
                                    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                            Text("Этот кейс больше недоступен",color=SecondaryText)
                                            TextButton(onClick={destination="cases"}) { Text("Все кейсы") }
                                        }
                                    }
                                } else {
                                    CaseDetailScreen(
                                        gameCase=gameCase,
                                        client=client,
                                        modifier=Modifier.weight(1f),
                                        padding=padding,
                                        onBack={destination="cases"}
                                    )
                                }
                            }
                            "inventory" -> InventoryScreen(
                                client=client,
                                modifier=Modifier.weight(1f),
                                padding=padding,
                                wide=wide
                            )
                            else -> ProfileScreen(
                                client=client,
                                modifier=Modifier.weight(1f),
                                padding=padding,
                                onLogout={scope.launch {client.logout();destination="home"}}
                            )
                        }
                        GameNavigation(
                            destination=destination,
                            padding=padding,
                            onNavigate={destination=it}
                        )
                    }
                }
                val reward=client.reward
                if (reward!=null) {
                    val sourceCase=client.cases.firstOrNull { it.id==client.rewardCaseId }
                    RouletteDialog(reward=reward,caseInfo=sourceCase,
                        onDone={client.acknowledgeReward()})
                }
            }
            if(showNotes) {
                ReleaseNotesDialog(onDismiss={showNotes=false})
            }
        }
    }
}

@Composable
private fun LoginScreen(client: GameClient) {
    val scope=rememberCoroutineScope()
    var registration by rememberSaveable {mutableStateOf(false)}
    var username by rememberSaveable {mutableStateOf("")}
    var password by remember { mutableStateOf("") }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=20.dp,vertical=28.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Image(
                painter=painterResource(R.drawable.ic_launcher),
                contentDescription="CaseOpener",
                modifier=Modifier.size(90.dp).clip(RoundedCornerShape(25.dp))
            )
            Spacer(Modifier.height(15.dp))
            Text("CASEOPENER",fontSize=27.sp,fontWeight=FontWeight.Black,color=Color.White)
            Text("NEW 2.0",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Violet)
            Spacer(Modifier.height(30.dp))
            Card(
                modifier=Modifier.widthIn(max=470.dp).fillMaxWidth(),
                colors=CardDefaults.cardColors(containerColor=CardColor),
                shape=RoundedCornerShape(24.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement=Arrangement.spacedBy(14.dp)
                ) {
                    Text(if(registration) "Создать аккаунт" else "Войти в игру",
                        fontSize=23.sp,fontWeight=FontWeight.Black)
                    Text("Весь прогресс хранится в аккаунте. Войди, чтобы открывать кейсы и собирать скины.",
                        fontSize=13.sp,color=SecondaryText)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected=!registration,onClick={registration=false},
                            label={Text("Вход")})
                        FilterChip(selected=registration,onClick={registration=true},
                            label={Text("Регистрация")})
                    }
                    OutlinedTextField(
                        value=username,onValueChange={username=it.take(24)},
                        label={Text("Логин")},
                        singleLine=true,modifier=Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value=password,onValueChange={password=it.take(128)},
                        label={Text("Пароль")},
                        singleLine=true,visualTransformation=PasswordVisualTransformation(),
                        modifier=Modifier.fillMaxWidth()
                    )
                    if (client.error!=null) {
                        Text(client.error.orEmpty(),color=Danger,fontSize=13.sp)
                    }
                    Button(
                        enabled=!client.busy && username.trim().length in 3..24 && password.length>=8,
                        onClick={scope.launch {client.authenticate(registration,username,password)}},
                        modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),
                        shape=RoundedCornerShape(15.dp)
                    ) {
                        Text(if(client.busy) "Подключение…" else if(registration) "Зарегистрироваться" else "Войти",
                            fontWeight=FontWeight.Bold)
                    }
                    Text("Вход обязателен для игры. Используй тестовый пароль до включения защищённого HTTPS-соединения.",
                        fontSize=11.sp,color=Gold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ConnectScreen(status:String,message:String?,onRetry:()->Unit,onLogout:()->Unit) {
    Column(
        Modifier.fillMaxSize().padding(26.dp),
        verticalArrangement=Arrangement.Center,
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color=Violet)
        Spacer(Modifier.height(22.dp))
        Text(status,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
        Text("Подключаемся к твоему аккаунту и загружаем коллекцию",
            color=SecondaryText,textAlign=TextAlign.Center,fontSize=13.sp)
        if(message!=null) {
            Spacer(Modifier.height(10.dp))
            Text(message,color=Danger,fontSize=13.sp,textAlign=TextAlign.Center)
        }
        Spacer(Modifier.height(15.dp))
        OutlinedButton(onClick=onRetry) {Text("Повторить подключение")}
        TextButton(onClick=onLogout) {Text("Выйти из аккаунта")}
    }
}

@Composable
private fun GameHeader(client: GameClient, padding: androidx.compose.ui.unit.Dp, onProfile:()->Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal=padding,vertical=11.dp),
        verticalAlignment=Alignment.CenterVertically
    ) {
        Image(
            painter=painterResource(R.drawable.ic_launcher),
            contentDescription="Логотип",
            modifier=Modifier.size(43.dp).clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("CASEOPENER",fontSize=20.sp,fontWeight=FontWeight.Black,
                color=Color.White,maxLines=1)
            Text("NEW 2.0  •  v"+BuildConfig.VERSION_NAME,
                fontSize=10.sp,color=Violet)
        }
        Surface(
            shape=RoundedCornerShape(12.dp),color=CardColor,
            modifier=Modifier.clickable(onClick=onProfile)
        ) {
            Text("🪙 ${client.state?.balance ?: 0}",
                modifier=Modifier.padding(horizontal=12.dp,vertical=10.dp),
                fontWeight=FontWeight.Bold,color=Gold,maxLines=1)
        }
    }
}

@Composable
private fun GameNavigation(destination:String,padding: androidx.compose.ui.unit.Dp,onNavigate:(String)->Unit) {
    NavigationBar(
        containerColor=CardColor,
        modifier=Modifier.fillMaxWidth().heightIn(min=62.dp)
    ) {
        val tabs=listOf(
            Triple("home","⌂","Главная"),
            Triple("cases","▣","Кейсы"),
            Triple("inventory","◆","Инвентарь"),
            Triple("profile","♙","Профиль")
        )
        tabs.forEach { (key,icon,label)->
            NavigationBarItem(
                selected=destination==key || (destination=="detail"&&key=="cases"),
                onClick={onNavigate(key)},
                icon={Text(icon,fontSize=21.sp,fontWeight=FontWeight.Bold)},
                label={Text(label,fontSize=10.sp,maxLines=1)},
                alwaysShowLabel=true,
                colors=NavigationBarItemDefaults.colors(
                    selectedIconColor=Background,selectedTextColor=Violet,
                    indicatorColor=Violet,unselectedIconColor=SecondaryText,
                    unselectedTextColor=SecondaryText
                )
            )
        }
    }
}

@Composable
private fun Artwork(imageUrl:String,emoji:String,modifier:Modifier=Modifier) {
    val uri=mediaUrl(imageUrl)
    if(uri==null) {
        Box(
            modifier.clip(RoundedCornerShape(17.dp)).background(
                Brush.verticalGradient(listOf(Color(0xFF502174),Color(0xFF1D102D)))
            ),
            contentAlignment=Alignment.Center
        ) { Text(emoji,fontSize=53.sp) }
    } else {
        AsyncImage(
            model=uri,
            contentDescription=null,
            modifier=modifier.clip(RoundedCornerShape(17.dp))
                .background(Color(0xFF39204E)),
            contentScale=ContentScale.Crop
        )
    }
}

@Composable
private fun HomeScreen(
    client: GameClient,modifier:Modifier,padding: androidx.compose.ui.unit.Dp,
    onCases:()->Unit,onCase:(String)->Unit,onNotes:()->Unit
) {
    LazyColumn(
        modifier.fillMaxWidth().padding(horizontal=padding),
        verticalArrangement=Arrangement.spacedBy(15.dp),
        contentPadding=PaddingValues(bottom=20.dp,top=8.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                    .background(Brush.horizontalGradient(listOf(
                        Color(0xFF46216A),Color(0xFF251333))))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("ТВОЯ КОЛЛЕКЦИЯ",color=Color.White,fontWeight=FontWeight.Black,fontSize=24.sp)
                    Text("Открывай кейсы, собирай редкие скины и находи легендарные награды.",
                        color=SecondaryText,fontSize=13.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick=onCases,shape=RoundedCornerShape(14.dp)) {
                        Text("Смотреть кейсы",fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                Text("ПОПУЛЯРНЫЕ КЕЙСЫ",fontWeight=FontWeight.Black,fontSize=17.sp)
                TextButton(onClick=onCases) {Text("Все")}
            }
        }
        if(client.cases.isEmpty()) item { Text("Пока нет доступных кейсов",color=SecondaryText) }
        items(client.cases.take(4),key={it.id}) { gameCase->
            CaseListTile(gameCase,onClick={onCase(gameCase.id)})
        }
        item {
            OutlinedButton(onClick=onNotes,modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(14.dp)) {
                Text("Что нового в версии "+BuildConfig.VERSION_NAME+"?")
            }
        }
    }
}

@Composable
private fun CaseListTile(gameCase:GameCase,onClick:()->Unit) {
    Card(
        modifier=Modifier.fillMaxWidth().clickable(onClick=onClick),
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=CardColor)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment=Alignment.CenterVertically
        ) {
            Artwork(gameCase.imageUrl,gameCase.emoji,Modifier.size(87.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(gameCase.name,fontWeight=FontWeight.Black,fontSize=17.sp,
                    maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(gameCase.subtitle,color=SecondaryText,fontSize=11.sp,maxLines=2)
                Spacer(Modifier.height(6.dp))
                Text("${gameCase.cost} 🪙",color=Gold,fontWeight=FontWeight.Bold)
            }
            Text("›",color=Violet,fontSize=29.sp)
        }
    }
}

@Composable
private fun CasesScreen(cases:List<GameCase>,modifier:Modifier,
                        padding: androidx.compose.ui.unit.Dp,onCase:(String)->Unit) {
    LazyVerticalGrid(
        columns=GridCells.Adaptive(minSize=170.dp),
        modifier=modifier.fillMaxWidth().padding(horizontal=padding),
        horizontalArrangement=Arrangement.spacedBy(12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp),
        contentPadding=PaddingValues(top=10.dp,bottom=20.dp)
    ) {
        item(span={GridItemSpan(maxLineSpan)}) {
            Column {
                Text("КЕЙСЫ",fontSize=22.sp,fontWeight=FontWeight.Black)
                Text("Выбери кейс и посмотри все возможные награды",color=SecondaryText,fontSize=12.sp)
            }
        }
        items(cases,key={it.id}) { gameCase ->
            Card(
                colors=CardDefaults.cardColors(containerColor=CardColor),
                shape=RoundedCornerShape(19.dp),
                modifier=Modifier.fillMaxWidth().clickable { onCase(gameCase.id) }
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Artwork(gameCase.imageUrl,gameCase.emoji,
                        Modifier.fillMaxWidth().height(135.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(gameCase.name,fontWeight=FontWeight.Black,fontSize=17.sp,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text(gameCase.subtitle,fontSize=11.sp,color=SecondaryText,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("${gameCase.cost} 🪙",fontWeight=FontWeight.Bold,color=Gold)
                }
            }
        }
        if(cases.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {
            Text("Доступных кейсов пока нет",color=SecondaryText)
        }
    }
}

@Composable
private fun CaseDetailScreen(
    gameCase:GameCase,client:GameClient,modifier:Modifier,
    padding: androidx.compose.ui.unit.Dp,onBack:()->Unit
) {
    val total=gameCase.skins.sumOf {it.weight}.coerceAtLeast(1)
    LazyVerticalGrid(
        columns=GridCells.Adaptive(minSize=145.dp),
        modifier=modifier.fillMaxWidth().padding(horizontal=padding),
        horizontalArrangement=Arrangement.spacedBy(10.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),
        contentPadding=PaddingValues(top=5.dp,bottom=28.dp)
    ) {
        item(span={GridItemSpan(maxLineSpan)}) {
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick=onBack) {Text("‹  Назад к кейсам")}
                Artwork(gameCase.imageUrl,gameCase.emoji,
                    Modifier.fillMaxWidth().height(190.dp))
                Spacer(Modifier.height(13.dp))
                Text(gameCase.name,fontSize=25.sp,fontWeight=FontWeight.Black)
                Text(gameCase.subtitle,color=SecondaryText,fontSize=13.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick={client.openCase(gameCase.id)},
                    enabled=client.status=="Онлайн" && !client.opening && !client.selling &&
                            client.state!=null && client.state!!.balance>=gameCase.cost &&
                            gameCase.skins.isNotEmpty(),
                    modifier=Modifier.fillMaxWidth().heightIn(min=53.dp),
                    shape=RoundedCornerShape(15.dp)
                ) {
                    Text(if(client.opening) "Открываем…" else "ОТКРЫТЬ ЗА ${gameCase.cost} 🪙",
                        fontWeight=FontWeight.Black)
                }
                if(client.state!=null && client.state!!.balance<gameCase.cost) {
                    Text("Недостаточно монет",color=Gold,fontSize=12.sp,
                        modifier=Modifier.padding(top=6.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("ВОЗМОЖНЫЕ НАГРАДЫ",fontSize=17.sp,fontWeight=FontWeight.Black)
                Text("Шансы указаны для каждого скина",fontSize=12.sp,color=SecondaryText)
            }
        }
        items(gameCase.skins,key={it.id}) { skin ->
            Card(
                modifier=Modifier.fillMaxWidth(),
                colors=CardDefaults.cardColors(containerColor=CardColor),
                shape=RoundedCornerShape(15.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Artwork(skin.imageUrl,skin.emoji,Modifier.fillMaxWidth().height(99.dp))
                    Spacer(Modifier.height(7.dp))
                    Text(skin.name,fontWeight=FontWeight.Bold,maxLines=2,
                        fontSize=12.sp,overflow=TextOverflow.Ellipsis,
                        minLines=2)
                    Text(skin.rarity,fontSize=11.sp,color=rarityTint(skin.rarity))
                    Text("${skin.value} 🪙",fontSize=12.sp,color=Gold)
                    Text("%.2f%%".format(java.util.Locale.US,skin.weight * 100.0 / total),
                        fontSize=11.sp,color=SecondaryText)
                }
            }
        }
        if(gameCase.skins.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {
            Text("В кейсе пока нет доступных предметов",color=SecondaryText)
        }
    }
}

private fun rarityTint(rarity:String):Color=when(rarity) {
    "Легендарный"->Gold
    "Мифический"->Color(0xFFFF82B5)
    "Эпический"->Violet
    "Редкий"->Color(0xFF71B8FF)
    else->SecondaryText
}

@Composable
private fun RouletteDialog(reward:OwnedSkin,caseInfo:GameCase?,onDone:()->Unit) {
    val pool=caseInfo?.skins.orEmpty()
    val row=remember(reward.id) {
        val choices=if(pool.isEmpty()) listOf(
            GameSkin("other","Награда","Редкий",1,1,"✦","")
        ) else pool
        val filler=List(36){choices[Random.nextInt(choices.size)]}
        val result=GameSkin("won",reward.name,reward.rarity,reward.value,1,
            reward.emoji,reward.imageUrl)
        filler+result+List(3){choices[Random.nextInt(choices.size)]}
    }
    val listState=rememberLazyListState()
    var rolling by remember(reward.id) { mutableStateOf(true) }
    LaunchedEffect(reward.id) {
        // The prize is generated on the server before the animation begins.
        for (index in 0..35) {
            listState.scrollToItem(index)
            delay(22L + (index*index/11).toLong())
        }
        rolling=false
    }
    Dialog(onDismissRequest={if(!rolling)onDone()}) {
        Card(
            modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(24.dp),
            colors=CardDefaults.cardColors(containerColor=CardColor)
        ) {
            Column(Modifier.padding(vertical=20.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(if(rolling) "ОТКРЫТИЕ КЕЙСА" else "ТВОЯ НАГРАДА!",
                    color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("▼",color=Gold,fontSize=24.sp)
                LazyRow(
                    state=listState,
                    userScrollEnabled=false,
                    modifier=Modifier.fillMaxWidth().height(141.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp),
                    contentPadding=PaddingValues(horizontal=12.dp)
                ) {
                    items(row.size) { index ->
                        val skin=row[index]
                        Card(
                            modifier=Modifier.width(119.dp).fillMaxHeight(),
                            colors=CardDefaults.cardColors(containerColor=
                                if(index==36)Color(0xFF43255C) else Color(0xFF2A1B3A)),
                            shape=RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Artwork(skin.imageUrl,skin.emoji,Modifier.size(83.dp))
                                Text(skin.name,fontSize=10.sp,maxLines=2,
                                    color=Color.White,textAlign=TextAlign.Center)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if(rolling) {
                    CircularProgressIndicator(color=Violet,modifier=Modifier.size(26.dp))
                    Text("Рулетка вращается…",color=SecondaryText,fontSize=12.sp)
                } else {
                    Text(reward.name,fontSize=17.sp,color=rarityTint(reward.rarity),
                        fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                    Text("${reward.rarity}  •  ${reward.value} 🪙",
                        color=Gold,fontSize=13.sp)
                    Spacer(Modifier.height(15.dp))
                    Button(onClick=onDone,shape=RoundedCornerShape(14.dp)) {
                        Text("ЗАБРАТЬ НАГРАДУ",fontWeight=FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryScreen(client:GameClient,modifier:Modifier,
                            padding: androidx.compose.ui.unit.Dp,wide:Boolean) {
    val inventory=client.state?.inventory.orEmpty()
    LazyVerticalGrid(
        columns=GridCells.Adaptive(minSize=if(wide)260.dp else 315.dp),
        modifier=modifier.fillMaxWidth().padding(horizontal=padding),
        horizontalArrangement=Arrangement.spacedBy(10.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),
        contentPadding=PaddingValues(top=10.dp,bottom=20.dp)
    ) {
        item(span={GridItemSpan(maxLineSpan)}) {
            Column {
                Text("ИНВЕНТАРЬ",fontWeight=FontWeight.Black,fontSize=22.sp)
                Text("${inventory.size} предметов в твоей коллекции",color=SecondaryText,fontSize=12.sp)
            }
        }
        items(inventory,key={it.id}) { item ->
            Card(
                colors=CardDefaults.cardColors(containerColor=CardColor),
                shape=RoundedCornerShape(17.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(11.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Artwork(item.imageUrl,item.emoji,Modifier.size(64.dp))
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name,fontWeight=FontWeight.Bold,fontSize=13.sp,
                            maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(item.rarity,fontSize=11.sp,color=rarityTint(item.rarity))
                        Text("${item.value} 🪙",fontSize=12.sp,color=Gold)
                    }
                    OutlinedButton(
                        onClick={client.sellItem(item)},
                        enabled=client.status=="Онлайн" && !client.selling && !client.opening,
                        contentPadding=PaddingValues(horizontal=9.dp)
                    ) {Text("Продать",fontSize=11.sp)}
                }
            }
        }
        if(inventory.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) {
            Box(Modifier.fillMaxWidth().padding(vertical=50.dp),contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("✦",fontSize=50.sp,color=Violet)
                    Text("Пока нет скинов",color=SecondaryText)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(client:GameClient,modifier:Modifier,
                          padding: androidx.compose.ui.unit.Dp,onLogout:()->Unit) {
    val profile=client.state ?: return
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=padding,vertical=10.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max=680.dp)) {
            Text("МОЙ ПРОФИЛЬ",fontSize=23.sp,fontWeight=FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            Card(
                colors=CardDefaults.cardColors(containerColor=CardColor),
                shape=RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(21.dp),
                    verticalArrangement=Arrangement.spacedBy(17.dp)) {
                    Text("👤 ${profile.username}",fontSize=21.sp,fontWeight=FontWeight.Black)
                    HorizontalDivider(color=Color(0xFF392449))
                    ProfileLine("🪙 Монеты",profile.balance.toString())
                    ProfileLine("🎯 Открыто кейсов",profile.openings.toString())
                    ProfileLine("◆ Скины",profile.inventory.size.toString())
                    Text("☁ Прогресс сохраняется автоматически",fontSize=12.sp,color=Violet)
                }
            }
            Spacer(Modifier.height(15.dp))
            OutlinedButton(
                onClick=onLogout,
                modifier=Modifier.fillMaxWidth().heightIn(min=51.dp),
                shape=RoundedCornerShape(14.dp)
            ) {Text("Выйти из аккаунта")}
            Spacer(Modifier.height(16.dp))
            Text("CaseOpener New 2.0 • версия "+BuildConfig.VERSION_NAME,
                color=SecondaryText,fontSize=12.sp)
        }
    }
}

@Composable
private fun ProfileLine(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,color=SecondaryText,fontSize=14.sp)
        Text(value,color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)
    }
}

@Composable
private fun ReleaseNotesDialog(onDismiss:()->Unit) {
    AlertDialog(
        onDismissRequest=onDismiss,
        containerColor=CardColor,
        shape=RoundedCornerShape(25.dp),
        title={
            Column {
                Text("Что нового?",fontWeight=FontWeight.Black)
                Text("Версия "+BuildConfig.VERSION_NAME,color=Violet,fontSize=12.sp)
            }
        },
        text={
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("ДОБАВЛЕНО",fontWeight=FontWeight.Bold,color=Violet)
                Text("• Полноценная онлайн-игра с обязательным входом в аккаунт.")
                Text("• Отдельные экраны главной, кейсов, инвентаря и профиля.")
                Text("• Просмотр скинов внутри кейса и анимация рулетки.")
                Text("УЛУЧШЕНО",fontWeight=FontWeight.Bold,color=Gold)
                Text("• Кейсы и изображения обновляются прямо во время игры.")
                Text("• Награды и прогресс сохраняются автоматически.")
            }
        },
        confirmButton={Button(onClick=onDismiss){Text("Понятно")}}
    )
}
