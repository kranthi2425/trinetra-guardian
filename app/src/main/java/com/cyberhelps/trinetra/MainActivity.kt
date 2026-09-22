package com.cyberhelps.trinetra

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.*
import kotlin.math.roundToInt

private val Navy = Color(0xFF07111F)
private val Card = Color(0xFF101E32)
private val Cyan = Color(0xFF27D7FF)
private val Blue = Color(0xFF386BFF)
private val Muted = Color(0xFFA9B8CC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TrinetraTheme { TrinetraApp() } }
    }
}

@Composable private fun TrinetraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Blue, background = Navy, surface = Card, onBackground = Color.White, onSurface = Color.White), content = content)
}

enum class Tab(val label: String) { HOME("Home"), SHIELD("APK Shield"), SOS("SOS"), USAGE("Usage"), LEARN("Shield") }

@Composable fun TrinetraApp() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    Scaffold(
        containerColor = Navy,
        topBar = { Header() },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF091728)) {
                Tab.entries.forEach { item ->
                    val icon = when(item) { Tab.HOME -> Icons.Default.Home; Tab.SHIELD -> Icons.Default.Security; Tab.SOS -> Icons.Default.Sos; Tab.USAGE -> Icons.Default.QueryStats; Tab.LEARN -> Icons.Default.VerifiedUser }
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(icon, item.label) }, label = { Text(item.label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Cyan, selectedTextColor = Cyan, indicatorColor = Card))
                }
            }
        }
    ) { padding -> Box(Modifier.padding(padding).fillMaxSize()) { when(tab) { Tab.HOME -> Dashboard { tab = it }; Tab.SHIELD -> ApkShield(); Tab.SOS -> SosScreen(); Tab.USAGE -> UsageScreen(); Tab.LEARN -> ArrestShield() } } }
}

@Composable private fun Header() {
    Surface(color = Navy) { Row(Modifier.fillMaxWidth().padding(20.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Cyan, Blue)), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Visibility, null, tint = Navy) }
        Spacer(Modifier.width(12.dp)); Column { Text("TRINETRA", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 20.sp); Text("YOUR DIGITAL GUARDIAN", color = Cyan, fontSize = 10.sp, letterSpacing = 1.sp) }
        Spacer(Modifier.weight(1f)); Icon(Icons.Default.Notifications, "Alerts", tint = Muted)
    } }
}

@Composable private fun Dashboard(open: (Tab) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
        item { Text("Good morning", color = Muted); Text("Your family is protected", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        item { GradientCard { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("SECURITY HEALTH", color = Cyan, fontSize = 12.sp); Text("92", fontSize = 48.sp, fontWeight = FontWeight.Black); Text("All core protections active", color = Muted) }; Icon(Icons.Default.Shield, null, tint = Cyan, modifier = Modifier.size(72.dp)) } } }
        item { Text("Guardian tools", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        items(listOf(
            Triple(Tab.SHIELD, "APK Shield", "Scan installed apps for known indicators and risky permission combinations"),
            Triple(Tab.SOS, "Emergency SOS", "Prepare a location, battery and timestamp alert for your parent"),
            Triple(Tab.USAGE, "Screen-time insights", "Read-only app usage with device-controlled permission"),
            Triple(Tab.LEARN, "Digital Arrest Shield", "Recognise coercion scams before they take control")
        )) { (target, title, subtitle) -> ToolCard(title, subtitle) { open(target) } }
        item { Notice("Privacy-first by design", "No message reading, no Accessibility control and no background surveillance. Analysis stays on this device.") }
    }
}

@Composable private fun ApkShield() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 0) }
    var results by remember { mutableStateOf<List<AppRisk>>(emptyList()) }
    var scanned by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenTitle("APK Shield", "On-device package and permission risk scan") }
        item { Notice("Safety note", "A flag is a triage signal, not proof. Permission patterns can belong to legitimate parental-control, accessibility, antivirus, device-management or OEM apps. Do not remove suspected monitoring software before making a safety and evidence plan.") }
        item { Button(onClick = { scanning = true; scanned = false; scope.launch { results = withContext(Dispatchers.Default) { ShieldScanner(context).scan { a,b -> progress = a to b } }; scanning = false; scanned = true } }, enabled = !scanning, modifier = Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Default.Radar, null); Spacer(Modifier.width(8.dp)); Text(if(scanning) "Scanning ${progress.first}/${progress.second}" else "Scan this device") } }
        if (scanning) item { LinearProgressIndicator(progress = { if(progress.second == 0) 0f else progress.first.toFloat()/progress.second }, Modifier.fillMaxWidth()) }
        if (scanned) item { val risky = results.count { it.level != RiskLevel.SAFE }; Text("${results.size} apps checked • $risky need review", color = Cyan, fontWeight = FontWeight.Bold) }
        items(results.filter { it.level != RiskLevel.SAFE }.ifEmpty { if(scanned) results.take(10) else emptyList() }) { RiskCard(it) }
        if (scanned && results.none { it.level != RiskLevel.SAFE }) item { Notice("No elevated findings", "No known IOC or strong risky combination was found. This does not prove the device is clean.") }
    }
}

@Composable private fun RiskCard(risk: AppRisk) {
    val color = when(risk.level) { RiskLevel.SAFE -> Color(0xFF55D88A); RiskLevel.SUSPICIOUS -> Color(0xFFFFC857); RiskLevel.HIGH_RISK -> Color(0xFFFF7B54); RiskLevel.MALICIOUS -> Color(0xFFFF4D6D) }
    Surface(color = Card, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) {
        Row { Column(Modifier.weight(1f)) { Text(risk.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(risk.packageName, color = Muted, fontSize = 11.sp) }; Surface(color = color.copy(.15f), shape = CircleShape) { Text(risk.level.label, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(10.dp, 5.dp)) } }
        if(risk.reasons.isNotEmpty()) { Spacer(Modifier.height(8.dp)); risk.reasons.forEach { Text("• $it", color = Muted, fontSize = 12.sp) } }; risk.unavailableSignals.forEach { Text("• $it", color = Color(0xFFFFC857), fontSize = 12.sp) }; risk.limitations.distinct().forEach { Text("• $it", color = Muted, fontSize = 11.sp) }
    } }
}

@Composable private fun SosScreen() {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { status = if(it.values.any { ok -> ok }) "Location permission ready" else "Location unavailable - SOS can still be sent" }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("Emergency SOS", "You review and send the SMS. TRINETRA never sends silently.") }
        item { OutlinedTextField(value=phone, onValueChange={ phone=it.filter { c -> c.isDigit() || c=='+' } }, label={Text("Parent phone number")}, leadingIcon={Icon(Icons.Default.Phone,null)}, modifier=Modifier.fillMaxWidth(), singleLine=true) }
        item { Box(Modifier.size(190.dp).background(Brush.radialGradient(listOf(Color(0xFFFF4D6D), Color(0xFF901F3A))), CircleShape), contentAlignment=Alignment.Center) { Button(onClick={ prepareSos(context, phone) { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }; status="Opening SMS composer" }, colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent), modifier=Modifier.fillMaxSize(), shape=CircleShape) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(Icons.Default.Sos,null,modifier=Modifier.size(54.dp)); Text("SEND SOS",fontWeight=FontWeight.Black,fontSize=24.sp) } } } }
        item { Text(status, color=Muted) }
        item { Notice("Included in the alert", "Latest available device location, battery percentage and local timestamp. If location permission is missing, TRINETRA asks first.") }
    }
}

private fun prepareSos(context: Context, phone: String, askLocation: () -> Unit) {
    if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { askLocation(); return }
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val location = runCatching { lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time } }.getOrNull()
    val battery = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val place = location?.let { "https://maps.google.com/?q=${it.latitude},${it.longitude}" } ?: "Location unavailable"
    val body = "TRINETRA SOS: I may need help. Location: $place | Battery: $battery% | Time: ${DateFormat.getDateTimeInstance().format(Date())}"
    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}")).putExtra("sms_body", body))
}

@Composable private fun UsageScreen() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val hasAccess = remember(tick) { hasUsageAccess(context) }
    val stats = remember(tick, hasAccess) { if(hasAccess) loadUsage(context) else emptyList() }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp), verticalArrangement=Arrangement.spacedBy(12.dp), contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("Screen-time", "Today's read-only usage, computed locally") }
        if(!hasAccess) item { Notice("Usage access required", "Android controls this special permission. TRINETRA reads totals only after you enable access.") }
        if(!hasAccess) item { Button(onClick={ context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier=Modifier.fillMaxWidth()) { Text("Open usage access settings") } }
        else item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("Top apps",fontWeight=FontWeight.Bold); TextButton(onClick={tick++}) { Text("Refresh") } } }
        items(stats) { (name, ms) -> Surface(color=Card,shape=RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Apps,null,tint=Cyan); Spacer(Modifier.width(12.dp)); Text(name,Modifier.weight(1f),maxLines=1); Text(formatDuration(ms),color=Muted) } } }
        if(hasAccess && stats.isEmpty()) item { Notice("No usage yet", "Android has not returned activity for today. Use a few apps, then refresh.") }
    }
}

private fun hasUsageAccess(context: Context): Boolean { val ops=context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager; return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)==AppOpsManager.MODE_ALLOWED }
private fun loadUsage(context: Context): List<Pair<String,Long>> { val now=System.currentTimeMillis(); val cal=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0)}; val usm=context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager; val pm=context.packageManager; return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,cal.timeInMillis,now).filter{it.totalTimeInForeground>0}.sortedByDescending{it.totalTimeInForeground}.take(20).map{ runCatching{pm.getApplicationLabel(pm.getApplicationInfo(it.packageName,0)).toString()}.getOrDefault(it.packageName) to it.totalTimeInForeground } }
private fun formatDuration(ms:Long):String { val mins=(ms/60000.0).roundToInt(); return if(mins>=60) "${mins/60}h ${mins%60}m" else "${mins}m" }

@Composable private fun ArrestShield() {
    val checks = remember { mutableStateListOf(false,false,false,false,false) }
    val items = listOf("End the call or video meeting. Real officials do not demand continuous camera access.","Do not transfer money, crypto, gift cards or 'safe account' deposits.","Verify independently using an official website or phone number - never the caller's link.","Tell a parent, teacher or trusted adult. Secrecy and isolation are scam pressure tactics.","Save call details, numbers and payment requests without engaging further.")
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
        item { ScreenTitle("Digital Arrest Shield","Stop. Disconnect. Verify independently.") }
        item { GradientCard { Column { Icon(Icons.Default.GppBad,null,tint=Cyan,modifier=Modifier.size(48.dp)); Spacer(Modifier.height(10.dp)); Text("There is no legal process called 'digital arrest'",fontSize=21.sp,fontWeight=FontWeight.Black); Text("Scammers impersonate police, courts, banks or agencies to keep victims on video and force urgent payments.",color=Muted) } } }
        item { Text("If this is happening now",fontSize=18.sp,fontWeight=FontWeight.Bold) }
        items(items.indices.toList()) { i -> Surface(color=Card,shape=RoundedCornerShape(16.dp),onClick={checks[i]=!checks[i]}) { Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) { Checkbox(checks[i],{checks[i]=it}); Spacer(Modifier.width(8.dp)); Text(items[i],fontSize=13.sp) } } }
        item { Notice("India cybercrime help", "For urgent financial cyber fraud, call 1930 and report at cybercrime.gov.in. In immediate physical danger, call local emergency services.") }
    }
}

@Composable private fun ScreenTitle(title:String,subtitle:String){ Column(Modifier.fillMaxWidth()) { Text(title,fontSize=27.sp,fontWeight=FontWeight.Black); Text(subtitle,color=Muted,fontSize=13.sp) } }
@Composable private fun GradientCard(content:@Composable ColumnScope.()->Unit){ Card(colors=CardDefaults.cardColors(containerColor=Color.Transparent),shape=RoundedCornerShape(24.dp),modifier=Modifier.background(Brush.linearGradient(listOf(Color(0xFF123557),Color(0xFF17244A))),RoundedCornerShape(24.dp))){Column(Modifier.padding(20.dp),content=content)} }
@Composable private fun ToolCard(title:String,subtitle:String,onClick:()->Unit){ Surface(color=Card,shape=RoundedCornerShape(18.dp),onClick=onClick){Row(Modifier.fillMaxWidth().padding(17.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(Cyan.copy(.12f),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Shield,null,tint=Cyan)};Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(subtitle,color=Muted,fontSize=12.sp,maxLines=2)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}} }
@Composable private fun Notice(title:String,body:String){ Surface(color=Color(0xFF0C2940),shape=RoundedCornerShape(16.dp)){Row(Modifier.fillMaxWidth().padding(15.dp)){Icon(Icons.Default.Info,null,tint=Cyan);Spacer(Modifier.width(10.dp));Column{Text(title,fontWeight=FontWeight.Bold);Text(body,color=Muted,fontSize=12.sp)}}} }
