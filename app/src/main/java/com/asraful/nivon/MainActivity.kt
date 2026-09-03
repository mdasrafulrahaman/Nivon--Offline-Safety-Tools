package com.asraful.nivon

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.asraful.nivon.data.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("nivon_settings")
private val onboardedKey = booleanPreferencesKey("onboarded")
private val darkKey = booleanPreferencesKey("dark")
private val accent = Color(0xFFFF5B68)
private val navy = Color(0xFF101827)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !viewModel.isReady.value }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val icon = splashScreenView.iconView
            val scaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 8f)
            val scaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 8f)
            val alpha = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)

            scaleX.interpolator = AnticipateInterpolator()
            scaleY.interpolator = AnticipateInterpolator()
            scaleX.duration = 600L
            scaleY.duration = 600L
            alpha.duration = 600L

            scaleX.doOnEnd { splashScreenView.remove() }

            scaleX.start()
            scaleY.start()
            alpha.start()
        }

        enableEdgeToEdge()
        setContent { NivonApp() }
    }
}

class SplashViewModel : ViewModel() {
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000L) // Ensure splash is visible for a bit
            _isReady.value = true
        }
    }
}

@Composable
private fun NivonApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var onboarded by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val p = context.dataStore.data.first()
        onboarded = p[onboardedKey] ?: false
        dark = p[darkKey] ?: true
        loaded = true
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(navy), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }
        return
    }

    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(primary = accent, secondary = Color(0xFF8BD8FF), surface = Color(0xFF182234), background = navy)
        } else {
            lightColorScheme(primary = Color(0xFFC73249), secondary = Color(0xFF176B87))
        }
    ) {
        if (!onboarded) {
            Onboarding {
                scope.launch { context.dataStore.edit { it[onboardedKey] = true } }
                onboarded = true
            }
        } else {
            AppShell(
                dark = dark,
                setDark = { value ->
                    scope.launch { context.dataStore.edit { it[darkKey] = value } }
                    dark = value
                }
            )
        }
    }
}

@Composable
private fun Onboarding(done: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val content = listOf(
        "Be ready when it matters." to "Nivon keeps essential safety tools close at hand.",
        "Works offline" to "Contacts, safety card and first-aid guides stay useful without a signal.",
        "Your information stays on your device" to "No account. No ads. No tracking of your safety data.",
        "Build your safety profile" to "Add a trusted contact and medical details when you are ready."
    )
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(navy, Color(0xFF25385A)))).padding(28.dp)) {
        Text("Nivon", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.Shield, null, tint = accent, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(24.dp))
            Text(content[page].first, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(content[page].second, color = Color(0xFFD7E2F6), fontSize = 18.sp, lineHeight = 27.sp)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = done) { Text("Skip", color = Color.White) }
            Text("${page + 1} / 4", color = Color.White)
            Button(
                onClick = { if (page == 3) done() else page++ },
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(if (page == 3) "Get started" else "Next")
            }
        }
    }
}

@Composable
private fun AppShell(dark: Boolean, setDark: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val destinations = listOf(
        "home" to Icons.Default.Home,
        "firstaid" to Icons.Default.MedicalServices,
        "tools" to Icons.Default.Build,
        "profile" to Icons.Default.Person
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { (route, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            nav.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, route) },
                        label = { Text(route.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { Home(nav) }
            composable("firstaid") { FirstAid() }
            composable("tools") { Tools(nav) }
            composable("profile") { Profile(dark, setDark, nav) }
            composable("contacts") { Contacts() }
            composable("card") { SafetyCard() }
            composable("numbers") { Numbers() }
            composable("location") { LocationScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Screen(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) = Scaffold(
    topBar = {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold) },
            actions = actions
        )
    }
) { pad ->
    Column(
        Modifier
            .padding(pad)
            .padding(20.dp)
            .fillMaxSize(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Home(nav: androidx.navigation.NavHostController) = Screen("Nivon") {
    val context = LocalContext.current
    Text("Ready when you need it.", style = MaterialTheme.typography.titleMedium)
    AssistChip(
        onClick = {},
        label = { Text("Offline ready") },
        leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
    )
    Spacer(Modifier.height(16.dp))
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("EMERGENCY", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { dial(context, "112") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFC83243))
            ) {
                Icon(Icons.Default.Call, null)
                Spacer(Modifier.width(8.dp))
                Text("CALL 112", fontWeight = FontWeight.Bold)
            }
            Text("Calls open your phone dialer — you stay in control.", color = Color.White, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("Quick actions", fontWeight = FontWeight.Bold, fontSize = 20.sp)
    Spacer(Modifier.height(8.dp))
    val quick = listOf(
        "Emergency numbers" to "numbers",
        "Emergency contacts" to "contacts",
        "Safety card" to "card",
        "Send location" to "location",
        "First aid" to "firstaid"
    )
    LazyColumn {
        items(quick) { (label, route) ->
            ListItem(
                headlineContent = { Text(label) },
                supportingContent = { Text(if (route == "location") "Prepare a shareable link" else "Available offline") },
                leadingContent = { Icon(Icons.Default.ArrowForward, null) },
                modifier = Modifier.clickable { nav.navigate(route) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun Contacts() {
    val context = LocalContext.current
    val dao = remember { NivonDatabase.get(context).dao() }
    val contacts by dao.contacts().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var editor by remember { mutableStateOf<EmergencyContact?>(null) }
    var adding by remember { mutableStateOf(false) }

    Screen(
        "Emergency contacts",
        actions = {
            IconButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, "Add emergency contact")
            }
        }
    ) {
        if (contacts.isEmpty()) {
            Empty("You're not alone.", "Add someone you trust as an emergency contact.")
        } else {
            LazyColumn {
                items(contacts, { it.id }) { c ->
                    ListItem(
                        headlineContent = { Text(c.name + if (c.primaryContact) " · Primary" else "") },
                        supportingContent = { Text("${c.relationship}  ${c.phone}") },
                        leadingContent = { Icon(Icons.Default.Person, null) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { dial(context, c.phone) }) {
                                    Icon(Icons.Default.Call, "Dial")
                                }
                                IconButton(onClick = { editor = c }) {
                                    Icon(Icons.Default.Edit, "Edit")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
    if (adding || editor != null) {
        ContactDialog(
            editor,
            onDismiss = { adding = false; editor = null },
            onSave = { c ->
                scope.launch {
                    if (c.primaryContact) dao.clearPrimary()
                    if (c.id == 0L) dao.insert(c) else dao.update(c)
                }
                adding = false
                editor = null
            },
            onDelete = { c ->
                scope.launch { dao.delete(c) }
                editor = null
            }
        )
    }
}

@Composable
private fun ContactDialog(
    existing: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit,
    onDelete: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var relation by remember { mutableStateOf(existing?.relationship ?: "") }
    var primary by remember { mutableStateOf(existing?.primaryContact ?: false) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add contact" else "Edit contact") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    phone,
                    { phone = it },
                    label = { Text("Indian phone number") },
                    singleLine = true,
                    isError = error
                )
                OutlinedTextField(relation, { relation = it }, label = { Text("Relationship") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(primary, { primary = it })
                    Text("Primary SOS contact")
                }
                if (error) {
                    Text("Enter a valid 10-digit Indian mobile number.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = name.isBlank() || !isValidIndianPhone(phone)
                if (!error) {
                    onSave(
                        EmergencyContact(
                            existing?.id ?: 0,
                            name.trim(),
                            normalizeIndianPhone(phone),
                            relation.trim(),
                            primaryContact = primary
                        )
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = { onDelete(existing) }) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun SafetyCard() {
    val context = LocalContext.current
    val dao = remember { NivonDatabase.get(context).dao() }
    val profile by dao.profile().collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var edit by remember { mutableStateOf(false) }
    val p = profile ?: SafetyProfile()

    Screen(
        "My Safety Card",
        actions = {
            IconButton(onClick = { edit = true }) {
                Icon(Icons.Default.Edit, "Edit safety card")
            }
        }
    ) {
        if (p.fullName.isBlank() && !edit) {
            Empty("Your safety card isn't ready yet.", "Add only the medical information you are comfortable storing locally.")
        } else {
            Text("Stored locally on this device.", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(p.fullName.ifBlank { "My Safety Card" }, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Detail("Blood group", p.bloodGroup)
                    Detail("Allergies", p.allergies)
                    Detail("Conditions", p.conditions)
                    Detail("Medications", p.medications)
                    Detail("Notes", p.notes)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "This card is general personal information, not an official medical record.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    if (edit) {
        ProfileDialog(p, { edit = false }) { saved ->
            scope.launch { dao.saveProfile(saved) }
            edit = false
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    if (value.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value)
    }
}

@Composable
private fun ProfileDialog(p: SafetyProfile, dismiss: () -> Unit, save: (SafetyProfile) -> Unit) {
    var name by remember { mutableStateOf(p.fullName) }
    var blood by remember { mutableStateOf(p.bloodGroup) }
    var allergies by remember { mutableStateOf(p.allergies) }
    var conditions by remember { mutableStateOf(p.conditions) }
    var medications by remember { mutableStateOf(p.medications) }
    var notes by remember { mutableStateOf(p.notes) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Edit safety card") },
        text = {
            LazyColumn {
                item {
                    listOf(
                        "Full name" to name,
                        "Blood group" to blood,
                        "Allergies" to allergies,
                        "Medical conditions" to conditions,
                        "Current medications" to medications,
                        "Important notes" to notes
                    ).forEachIndexed { i, (label, value) ->
                        OutlinedTextField(
                            value,
                            { v ->
                                when (i) {
                                    0 -> name = v
                                    1 -> blood = v
                                    2 -> allergies = v
                                    3 -> conditions = v
                                    4 -> medications = v
                                    else -> notes = v
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                save(
                    SafetyProfile(
                        fullName = name,
                        bloodGroup = blood,
                        allergies = allergies,
                        conditions = conditions,
                        medications = medications,
                        notes = notes
                    )
                )
            }) {
                Text("Save")
            }
        }
    )
}

data class FirstAidGuide(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val titleBn: String,
    val stepsEn: String,
    val stepsHi: String,
    val stepsBn: String
)

private val detailedGuides = listOf(
    FirstAidGuide(
        id = "heart_attack",
        titleEn = "Heart Attack",
        titleHi = "दिल का दौरा (Heart Attack)",
        titleBn = "হার্ট অ্যাটাক (Heart Attack)",
        stepsEn = """
            1. Call 112 immediately.
            2. Have the person sit down, rest, and try to keep calm.
            3. Loosen any tight clothing.
            4. Ask if the person takes any chest pain medication, such as nitroglycerin, for a known heart condition, and help them take it.
            5. If the person is unconscious and not breathing, begin CPR.
        """.trimIndent(),
        stepsHi = """
            1. तुरंत 112 पर कॉल करें।
            2. व्यक्ति को बैठाएं, आराम करने दें और शांत रहने की कोशिश करें।
            3. तंग कपड़ों को ढीला करें।
            4. पूछें कि क्या व्यक्ति किसी ज्ञात हृदय रोग के लिए सीने में दर्द की दवा (जैसे नाइट्रोग्लिसरीन) लेता है, और उसे लेने में मदद करें।
            5. यदि व्यक्ति बेहोश है और सांस नहीं ले रहा है, तो CPR शुरू करें।
        """.trimIndent(),
        stepsBn = """
            1. অবিলম্বে ১১২ নম্বরে কল করুন।
            2. ব্যক্তিকে বসিয়ে দিন, বিশ্রাম নিতে দিন এবং শান্ত রাখার চেষ্টা করুন।
            3. টাইট পোশাক ঢিলা করে দিন।
            4. ব্যক্তি হৃদরোগের জন্য কোনো ওষুধ (যেমন নাইট্রোগ্লিসারিন) ব্যবহার করেন কিনা তা জিজ্ঞাসা করুন এবং নিতে সাহায্য করুন।
            5. যদি ব্যক্তি অচেতন হয়ে যায় এবং শ্বাস না নেয়, তবে CPR শুরু করুন।
        """.trimIndent()
    ),
    FirstAidGuide(
        id = "cpr",
        titleEn = "CPR (Cardiopulmonary Resuscitation)",
        titleHi = "सीपीआर (CPR)",
        titleBn = "সিপিআর (CPR)",
        stepsEn = """
            1. Check the scene for safety and the person for responsiveness.
            2. If unresponsive, call 112.
            3. Place the person on their back on a firm, flat surface.
            4. Give 30 chest compressions: Push hard and fast in the center of the chest (at least 2 inches deep, 100-120 compressions per minute).
            5. Give 2 rescue breaths: Tilt head back, lift chin, pinch nose, and blow into the mouth.
            6. Continue cycles of 30 compressions and 2 breaths until help arrives or an AED is available.
        """.trimIndent(),
        stepsHi = """
            1. सुरक्षा के लिए दृश्य की जाँच करें और व्यक्ति की प्रतिक्रिया देखें।
            2. यदि कोई प्रतिक्रिया न हो, तो 112 पर कॉल करें।
            3. व्यक्ति को एक सख्त, सपाट सतह पर पीठ के बल लिटाएं।
            4. 30 बार छाती को दबाएं: छाती के केंद्र में जोर से और तेजी से दबाएं (कम से कम 2 इंच गहरा, 100-120 प्रति मिनट)।
            5. 2 बार बचाव सांसें दें: सिर को पीछे झुकाएं, ठुड्डी उठाएं, नाक बंद करें और मुंह में फूंक मारें।
            6. मदद आने तक 30 दबाव और 2 सांसों का चक्र जारी रखें।
        """.trimIndent(),
        stepsBn = """
            1. চারপাশ নিরাপদ কিনা পরীক্ষা করুন এবং ব্যক্তির জ্ঞান আছে কিনা দেখুন।
            2. সাড়া না দিলে ১১২ নম্বরে কল করুন।
            3. ব্যক্তিকে শক্ত, সমতল জায়গায় পিঠের ওপর শুইয়ে দিন।
            4. ৩০ বার বুকে চাপ দিন: বুকের মাঝখানে জোরে এবং দ্রুত চাপ দিন (কমপক্ষে ২ ইঞ্চি গভীর, মিনিটে ১০০-১২০ বার)।
            5. ২ বার কৃত্রিম শ্বাস দিন: মাথা পেছনে হেলিয়ে থুতনি তুলুন, নাক চেপে ধরে মুখে ফুঁ দিন।
            6. সাহায্য না আসা পর্যন্ত ৩০ বার চাপ এবং ২ বার শ্বাসের প্রক্রিয়া চালিয়ে যান।
        """.trimIndent()
    ),
    FirstAidGuide(
        id = "bleeding",
        titleEn = "Severe Bleeding",
        titleHi = "गंभीर रक्तस्राव (Severe Bleeding)",
        titleBn = "প্রবল রক্তপাত (Severe Bleeding)",
        stepsEn = """
            1. Call 112 for life-threatening bleeding.
            2. Put on gloves if available.
            3. Apply direct pressure to the wound with a clean cloth or bandage.
            4. Do not remove the cloth if it becomes soaked; add more layers on top.
            5. If bleeding doesn't stop with direct pressure, consider using a tourniquet for limbs if trained.
            6. Keep the person warm and lying down to prevent shock.
        """.trimIndent(),
        stepsHi = """
            1. जानलेवा रक्तस्राव के लिए 112 पर कॉल करें।
            2. यदि उपलब्ध हो तो दस्ताने पहनें।
            3. साफ कपड़े या पट्टी से घाव पर सीधा दबाव डालें।
            4. यदि कपड़ा भीग जाए तो उसे न हटाएं; ऊपर से और परतें जोड़ें।
            5. यदि सीधे दबाव से रक्तस्राव नहीं रुकता है, तो प्रशिक्षित होने पर अंगों के लिए टॉर्निकेट (tourniquet) का उपयोग करें।
            6. सदमे से बचाने के लिए व्यक्ति को गर्म रखें और लेटा दें।
        """.trimIndent(),
        stepsBn = """
            1. জীবনঘাতী রক্তপাতের জন্য ১১২ নম্বরে কল করুন।
            2. সম্ভব হলে গ্লাভস পরে নিন।
            3. পরিষ্কার কাপড় বা ব্যান্ডেজ দিয়ে ক্ষতস্থানে সরাসরি চাপ দিন।
            4. কাপড় ভিজে গেলে তা সরাবেন না; ওপর দিয়ে আরও কাপড় যোগ করুন।
            5. সরাসরি চাপে রক্তপাত না কমলে, প্রশিক্ষণ থাকলে হাত বা পায়ের ক্ষেত্রে টর্নিকেট ব্যবহার করুন।
            6. শক প্রতিরোধ করতে ব্যক্তিকে শুইয়ে দিন এবং উষ্ণ রাখুন।
        """.trimIndent()
    ),
    FirstAidGuide(
        id = "choking",
        titleEn = "Choking",
        titleHi = "दम घुटना (Choking)",
        titleBn = "গলায় কিছু আটকে যাওয়া (Choking)",
        stepsEn = """
            1. Ask "Are you choking?" If they can cough or speak, encourage them to keep coughing.
            2. If they cannot breathe, cough, or speak, call 112.
            3. Give 5 back blows: Use the heel of your hand between the shoulder blades.
            4. Give 5 abdominal thrusts (Heimlich maneuver): Wrap arms around waist, make a fist above the navel, and pull inward and upward.
            5. Repeat 5 back blows and 5 thrusts until the object is forced out or the person becomes unconscious.
        """.trimIndent(),
        stepsHi = """
            1. पूछें "क्या आपका दम घुट रहा है?" यदि वे खाँस सकते हैं या बोल सकते हैं, तो उन्हें खाँसते रहने के लिए प्रोत्साहित करें।
            2. यदि वे सांस नहीं ले पा रहे हैं, खाँस नहीं पा रहे हैं या बोल नहीं पा रहे हैं, तो 112 पर कॉल करें।
            3. पीठ पर 5 बार थपथपाएं: अपने हाथ के निचले हिस्से का उपयोग कंधे की हड्डियों के बीच करें।
            4. 5 बार पेट के दबाव (Heimlich maneuver) दें: कमर के चारों ओर हाथ लपेटें, नाभि के ऊपर मुट्ठी बनाएं और अंदर और ऊपर की ओर खींचें।
            5. तब तक जारी रखें जब तक वस्तु बाहर न निकल जाए या व्यक्ति बेहोश न हो जाए।
        """.trimIndent(),
        stepsBn = """
            1. জিজ্ঞাসা করুন "আপনার কি দম বন্ধ হয়ে আসছে?" যদি তারা কাশতে বা কথা বলতে পারে, তবে তাদের কাশতে উৎসাহিত করুন।
            2. যদি তারা শ্বাস নিতে, কাশতে বা কথা বলতে না পারে, তবে ১১২ নম্বरे কল করুন।
            3. পিঠে ৫ বার জোরে থাপ্পড় দিন: দুই কাঁধের হাড়ের মাঝে হাতের তলা দিয়ে চাপ দিন।
            4. ৫ বার পেটে চাপ দিন (Heimlich maneuver): কোমরের চারপাশে হাত জড়িয়ে নাভির ঠিক ওপরে মুষ্টিবদ্ধ হাত দিয়ে ভেতরের দিকে ও ওপরে টানুন।
            5. আটকে থাকা জিনিস বের না হওয়া পর্যন্ত বা ব্যক্তি অচেতন না হওয়া পর্যন্ত এটি চালিয়ে যান।
        """.trimIndent()
    ),
    FirstAidGuide(
        id = "burns",
        titleEn = "Burns",
        titleHi = "जलना (Burns)",
        titleBn = "পুড়ে যাওয়া (Burns)",
        stepsEn = """
            1. Stop the burning process by removing the source of heat.
            2. Cool the burn with cool (not cold) running water for at least 10-20 minutes.
            3. Remove jewelry or tight clothing near the burned area before it swells.
            4. Cover the burn loosely with a sterile dressing or clean plastic wrap.
            5. Do not pop blisters or apply butter/ointments.
            6. Seek medical help for deep burns, burns on face/hands, or if the burn is larger than the person's palm.
        """.trimIndent(),
        stepsHi = """
            1. गर्मी के स्रोत को हटाकर जलने की प्रक्रिया को रोकें।
            2. कम से कम 10-20 मिनट के लिए ठंडे (बर्फ जैसा नहीं) बहते पानी से जलन को ठंडा करें।
            3. सूजन होने से पहले जले हुए क्षेत्र के पास के गहने या तंग कपड़े हटा दें।
            4. जलन को बाँझ ड्रेसिंग या साफ प्लास्टिक रैप से ढीला ढंकें।
            5. छालों को न फोड़ें और मक्खन या मलहम न लगाएं।
            6. गहरे घाव, चेहरे/हाथों के जलने या हथेली से बड़े घाव के लिए चिकित्सकीय मदद लें।
        """.trimIndent(),
        stepsBn = """
            1. তাপের উৎস সরিয়ে পুড়ে যাওয়ার প্রক্রিয়া বন্ধ করুন।
            2. কমপক্ষে ১০-২০ মিনিট ঠান্ডা (বরফ নয়) কলের জলের নিচে ধরে রাখুন।
            3. ফোলা শুরু হওয়ার আগেই গয়না বা টাইট পোশাক সরিয়ে ফেলুন।
            4. পরিষ্কার ড্রেসিং বা প্লাস্টিক র‍্যাপ দিয়ে হালকা করে ঢেকে দিন।
            5. ফোসকা গলাবেন না এবং মাখন বা মলম লাগাবেন না।
            6. গভীর পোড়া, মুখ বা হাতের পোড়া অথবা হাতের তালুর চেয়ে বড় পোড়ার ক্ষেত্রে ডাক্তার দেখান।
        """.trimIndent()
    ),
    FirstAidGuide(
        id = "seizure",
        titleEn = "Seizure",
        titleHi = "मिर्गी का दौरा (Seizure)",
        titleBn = "খিঁচুনি বা মৃগী (Seizure)",
        stepsEn = """
            1. Stay calm and time the seizure.
            2. Keep the person safe by moving away sharp objects.
            3. Place something soft under their head.
            4. Loosen tight clothing around the neck.
            5. After the shaking stops, roll the person onto their side (recovery position).
            6. Do not restrain the person or put anything in their mouth.
            7. Call 112 if the seizure lasts more than 5 minutes or if the person is injured.
        """.trimIndent(),
        stepsHi = """
            1. शांत रहें और दौरे का समय नोट करें।
            2. नुकीली चीजों को दूर हटाकर व्यक्ति को सुरक्षित रखें।
            3. उनके सिर के नीचे कुछ मुलायम रखें।
            4. गर्दन के पास के तंग कपड़ों को ढीला करें।
            5. कंपन रुकने के बाद, व्यक्ति को उनकी करवट पर लिटाएं (recovery position)।
            6. व्यक्ति को रोकने की कोशिश न करें और उनके मुंह में कुछ न डालें।
            7. यदि दौरा 5 मिनट से अधिक समय तक रहता है या व्यक्ति घायल है, तो 112 पर कॉल करें।
        """.trimIndent(),
        stepsBn = """
            1. শান্ত থাকুন এবং কতক্ষণ ধরে খিঁচুনি হচ্ছে তা খেয়াল করুন।
            2. ধারালো জিনিস সরিয়ে ব্যক্তিকে নিরাপদ রাখুন।
            3. মাথার নিচে নরম কিছু দিন।
            4. ঘাড়ের কাছের পোশাক ঢিলা করে দিন।
            5. খিঁচুনি বন্ধ হলে ব্যক্তিকে একদিকে কাত করে শুইয়ে দিন (recovery position)।
            6. ব্যক্তিকে চেপে ধরার চেষ্টা করবেন না এবং মুখে কিছু দেবেন না।
            7. যদি ৫ মিনিটের বেশি স্থায়ী হয় বা ব্যক্তি আহত হয়, তবে ১১২ নম্বরে কল করুন।
        """.trimIndent()
    ),
    FirstAidGuide(
        id = "fracture",
        titleEn = "Fractures",
        titleHi = "हड्डी टूटना (Fracture)",
        titleBn = "হাড় ভাঙা (Fracture)",
        stepsEn = """
            1. Do not try to realign the bone.
            2. Control any bleeding by applying pressure to the wound (not over the bone).
            3. Immobilize the injured area using a splint or sling if possible.
            4. Apply ice packs wrapped in a cloth to reduce swelling (20 mins on/off).
            5. Treat for shock: Keep the person lying down and warm.
            6. Call 112 or transport to the nearest hospital.
        """.trimIndent(),
        stepsHi = """
            1. हड्डी को सीधा करने की कोशिश न करें।
            2. घाव पर दबाव डालकर रक्तस्राव को नियंत्रित करें (हड्डी के ऊपर नहीं)।
            3. यदि संभव हो तो स्प्लिंट (splint) या स्लिंग का उपयोग करके घायल क्षेत्र को स्थिर करें।
            4. सूजन कम करने के लिए कपड़े में लिपटे आइस पैक लगाएं (20 मिनट के लिए)।
            5. सदमे का इलाज करें: व्यक्ति को लेटाकर और गर्म रखें।
            6. 112 पर कॉल करें या नजदीकी अस्पताल ले जाएं।
        """.trimIndent(),
        stepsBn = """
            1. হাড় সোজা করার চেষ্টা করবেন না।
            2. ক্ষতস্থানে চাপ দিয়ে রক্তপাত বন্ধ করুন (ভাঙা হাড়ের ওপর সরাসরি চাপ দেবেন না)।
            3. সম্ভব হলে স্প্লিন্ট বা স্লিং ব্যবহার করে জায়গাটি স্থির রাখুন।
            4. ফোলা কমাতে কাপড়ে মোড়ানো বরফ দিন (২০ মিনিট করে)।
            5. শক প্রতিরোধ করতে ব্যক্তিকে শুইয়ে দিন এবং উষ্ণ রাখুন।
            6. ১১২ নম্বরে কল করুন বা নিকটস্থ হাসপাতালে নিয়ে যান।
        """.trimIndent()
    ),
    FirstAidGuide(
        id = "poisoning",
        titleEn = "Poisoning",
        titleHi = "जहर (Poisoning)",
        titleBn = "বিষক্রিয়া (Poisoning)",
        stepsEn = """
            1. Try to identify what was swallowed and how much.
            2. Call 112 or a poison control center immediately.
            3. Do not induce vomiting unless told to do so by a professional.
            4. If the poison is on the skin or in eyes, flush with running water for 15-20 minutes.
            5. If unconscious but breathing, place in recovery position.
        """.trimIndent(),
        stepsHi = """
            1. यह पहचानने की कोशिश करें कि क्या और कितना निगला गया है।
            2. तुरंत 112 या जहर नियंत्रण केंद्र (poison control center) को कॉल करें।
            3. जब तक किसी पेशेवर द्वारा न कहा जाए, तब तक उल्टी न कराएं।
            4. यदि जहर त्वचा या आंखों पर है, तो 15-20 मिनट तक बहते पानी से धोएं।
            5. यदि बेहोश है लेकिन सांस ले रहा है, तो रिकवरी पोजीशन (recovery position) में रखें।
        """.trimIndent(),
        stepsBn = """
            1. কী বিষক্রিয়া হয়েছে এবং কতটা হয়েছে তা বোঝার চেষ্টা করুন।
            2. অবিলম্বে ১১২ নম্বরে বা বিষ নিয়ন্ত্রণ কেন্দ্রে কল করুন।
            3. বিশেষজ্ঞের পরামর্শ ছাড়া বমি করানোর চেষ্টা করবেন না।
            4. বিষ চামড়ায় বা চোখে লাগলে ১৫-২০ মিনিট ধরে জল দিয়ে ধুয়ে ফেলুন।
            5. যদি অচেতন থাকে কিন্তু শ্বাস চলে, তবে একদিকে কাত করে শুইয়ে দিন।
        """.trimIndent()
    )
)

@Composable
private fun FirstAid() {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FirstAidGuide?>(null) }
    val results = detailedGuides.filter {
        it.titleEn.contains(query, true) || it.titleHi.contains(query, true) || it.titleBn.contains(query, true)
    }

    Screen("First aid") {
        Text(
            "General education only — not a substitute for professional medical care.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search offline guides") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn {
            items(results) { g ->
                ListItem(
                    headlineContent = { Text(g.titleEn) },
                    supportingContent = { Text(g.titleBn, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.clickable { selected = g }
                )
                HorizontalDivider()
            }
        }
    }
    selected?.let { g ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = {
                Column {
                    Text(g.titleEn, fontWeight = FontWeight.Bold)
                    Text(g.titleHi, style = MaterialTheme.typography.bodyMedium)
                    Text(g.titleBn, style = MaterialTheme.typography.bodyMedium)
                }
            },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    item {
                        Text("English", fontWeight = FontWeight.Bold, color = accent)
                        Text(g.stepsEn)
                        Spacer(Modifier.height(12.dp))
                        Text("हिंदी", fontWeight = FontWeight.Bold, color = accent)
                        Text(g.stepsHi)
                        Spacer(Modifier.height(12.dp))
                        Text("বাংলা", fontWeight = FontWeight.Bold, color = accent)
                        Text(g.stepsBn)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Emergency warning: Call 112 immediately for severe or life-threatening symptoms.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun Tools(nav: androidx.navigation.NavHostController) = Screen("Emergency tools") {
    val context = LocalContext.current
    var torch by remember { mutableStateOf(false) }

    val camera = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId = remember {
        camera.cameraIdList.firstOrNull {
            camera.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }
    Column {
        ToolButton("Flashlight", if (torch) "On" else "Turn on", Icons.Default.FlashlightOn) {
            if (cameraId == null) {
                toast(context, "Flashlight unavailable on this device.")
            } else {
                try {
                    camera.setTorchMode(cameraId, !torch)
                    torch = !torch
                    haptic(context)
                } catch (_: Exception) {
                    toast(context, "Flashlight unavailable.")
                }
            }
        }
        ToolButton("Share location", "Open location tool", Icons.Default.LocationOn) {
            nav.navigate("location")
        }
    }
}

@Composable
private fun ToolButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    action: () -> Unit
) {
    Card(Modifier
        .fillMaxWidth()
        .padding(vertical = 5.dp)
        .clickable(onClick = action)) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, null) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null) }
        )
    }
}

@Composable
private fun LocationScreen() {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var message by remember { mutableStateOf("Location has not been requested.") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            fetchLocation(context, {
                location = it
                message = "Location ready. You choose where to share it."
            }, { message = it })
        } else {
            message = "Location access is off. Enable it to prepare a shareable location."
        }
    }
    Screen("Share my location") {
        Text(message)
        Spacer(Modifier.height(18.dp))
        location?.let {
            Text("${it.latitude}, ${it.longitude}\nAccuracy: ${it.accuracy.toInt()} m")
        }
        Button(
            onClick = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get current location")
        }
        location?.let { l ->
            OutlinedButton(
                onClick = {
                    share(
                        context,
                        "My current location: https://maps.google.com/?q=${l.latitude},${l.longitude}"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share location")
            }
        }
    }
}

@Composable
private fun Numbers() = Screen("Emergency numbers") {
    val context = LocalContext.current
    Text(
        "Availability can vary by state, network and location. Verify local services before relying on them.",
        style = MaterialTheme.typography.bodySmall
    )
    LazyColumn {
        items(
            listOf(
                "112" to "National Emergency (All-in-one)",
                "100" to "Police",
                "101" to "Fire",
                "102" to "Ambulance (General)",
                "108" to "Ambulance (Disaster/Emergency)",
                "1091" to "Women Helpline (Police)",
                "181" to "Women Helpline (Domestic Abuse)",
                "1098" to "Child Helpline",
                "1073" to "Road Accident Helpline",
                "1930" to "Cyber Crime (Financial Fraud)",
                "139" to "Railway Security/Enquiry",
                "1066" to "Anti-Poison (AIIMS)",
                "1078" to "Disaster Management (NDRF)",
                "14567" to "Senior Citizen Helpline",
                "1912" to "Electricity Complaint"
            )
        ) { (n, label) ->
            ListItem(
                headlineContent = { Text(n) },
                supportingContent = { Text(label) },
                trailingContent = {
                    IconButton(onClick = { dial(context, n) }) {
                        Icon(Icons.Default.Call, "Dial $n")
                    }
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun Profile(
    dark: Boolean,
    setDark: (Boolean) -> Unit,
    nav: androidx.navigation.NavHostController
) = Screen("Profile & settings") {
    val context = LocalContext.current
    Text("Your safety profile", fontWeight = FontWeight.Bold)
    ListItem(headlineContent = { Text("My Safety Card") }, modifier = Modifier.clickable { nav.navigate("card") })
    ListItem(headlineContent = { Text("Emergency contacts") }, modifier = Modifier.clickable { nav.navigate("contacts") })
    HorizontalDivider()
    Text("Appearance", fontWeight = FontWeight.Bold)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Dark cinematic theme", Modifier.weight(1f))
        Switch(dark, setDark)
    }
    HorizontalDivider()
    Text("Legal & Info", fontWeight = FontWeight.Bold)
    ListItem(
        headlineContent = { Text("Privacy Policy") },
        modifier = Modifier.clickable { openUrl(context, "https://sites.google.com/view/nivon-privacy") }
    )
    ListItem(
        headlineContent = { Text("Terms & Conditions") },
        modifier = Modifier.clickable { openUrl(context, "https://sites.google.com/view/nivon-terms") }
    )
    ListItem(
        headlineContent = { Text("About Nivon") },
        modifier = Modifier.clickable { openUrl(context, "https://mdasrafulrahaman.github.io/portfolio/") }
    )
    HorizontalDivider()
    Text("App Info", fontWeight = FontWeight.Bold)
    Text("Developed by MD Asraful Rahaman\nVersion 1.0.0\nNivon is not affiliated with government agencies or emergency services.")
}

@Composable
private fun Empty(title: String, body: String) = Column(
    Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Icon(Icons.Default.Shield, null, modifier = Modifier.size(60.dp), tint = accent)
    Spacer(Modifier.height(16.dp))
    Text(title, fontWeight = FontWeight.Bold, fontSize = 21.sp)
    Text(body, modifier = Modifier.padding(20.dp))
}

private fun dial(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
    } catch (e: Exception) {
        toast(context, "Unable to open dialer.")
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        toast(context, "Unable to open web page.")
    }
}

private fun share(context: Context, text: String) {
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
            "Share with"
        )
    )
}

private fun haptic(context: Context) {
    (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(
        VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
    )
}

private fun toast(context: Context, message: String) =
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()

private fun fetchLocation(context: Context, success: (Location) -> Unit, failure: (String) -> Unit) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        failure("Location permission is required.")
        return
    }
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener {
            if (it != null) success(it) else failure("We couldn't get your location. Check Location is enabled and try again.")
        }.addOnFailureListener { failure("We couldn't get your location. Try again.") }
}
