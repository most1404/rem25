package app.yadavar.shamsi.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import app.yadavar.shamsi.model.formatDayMonth
import app.yadavar.shamsi.model.formatTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.yadavar.shamsi.CrashActivity
import app.yadavar.shamsi.CrashLog
import app.yadavar.shamsi.Graph
import app.yadavar.shamsi.data.Actions
import app.yadavar.shamsi.jalali.toJalali
import app.yadavar.shamsi.model.Reminder
import app.yadavar.shamsi.model.Section
import app.yadavar.shamsi.model.Settings
import app.yadavar.shamsi.model.TimeFilter
import app.yadavar.shamsi.model.filterReminders
import app.yadavar.shamsi.model.dateOf
import app.yadavar.shamsi.model.formatLongDate
import app.yadavar.shamsi.model.rowMeta
import app.yadavar.shamsi.model.sectionOf
import app.yadavar.shamsi.voice.parseReminderSpeech
import java.util.Date

class MainActivity : ComponentActivity() {
    /** Reminder id requested by a widget tap; the screen opens its edit form once the list is loaded. */
    private val openRequest = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openRequest.value = intent.getStringExtra(EXTRA_OPEN_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CrashLog.read(this) != null || !Graph.isReady()) {
            startActivity(Intent(this, CrashActivity::class.java))
            finish()
            return
        }
        openRequest.value = intent?.getStringExtra(EXTRA_OPEN_ID)
        enableEdgeToEdge()
        setContent {
            val settings by Graph.settings.flow.collectAsStateWithLifecycle(initialValue = Settings())
            YadavarTheme(settings.theme, settings.accent, appTextScale(settings.appFont), settings.appText) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    // The app fades in softly when it opens.
                    val appeared = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visibleState = appeared,
                        enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 30 },
                    ) {
                        AppScreen(openRequest)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_ID = "open_reminder_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScreen(openRequest: MutableState<String?>, vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(openRequest.value, reminders) {
        val wanted = openRequest.value ?: return@LaunchedEffect
        val target = reminders.firstOrNull { it.id == wanted }
        if (target != null) {
            vm.tab = Tab.Home
            vm.open(target)
            openRequest.value = null
        } else if (reminders.isNotEmpty()) {
            openRequest.value = null
        }
    }
    val speech = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val said = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
        vm.openParsed(parseReminderSpeech(said), said)
    }
    val askNotification = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(vm.notice) {
        val text = vm.notice ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.notice = null
    }
    val startSystemVoice: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "یادآور را بگو")
        }
        runCatching { speech.launch(intent) }
            .onFailure { vm.notice = "تشخیص گفتار روی این گوشی نیست" }
    }
    var showVoice by remember { mutableStateOf(false) }
    var showBattery by remember { mutableStateOf(false) }
    val askMic = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showVoice = true else vm.notice = "برای گفتن یادآور، اجازهٔ میکروفون لازم است"
    }
    val startVoice: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            showVoice = true
        } else {
            askMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    // After the first launch: take the person to the battery settings to choose "Unrestricted".
    LaunchedEffect(Unit) {
        val saved = Graph.settings.current()
        delay(2500)
        if (!saved.batteryAsked && !BatterySettings.isUnrestricted(context)) showBattery = true
    }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        delay(4000)
        val ver = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.3"
        app.yadavar.shamsi.UpdateChecker.checkAndPrompt(context, ver) { msg ->
            updateMessage = msg
            showUpdateDialog = true
        }
    }
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("به‌روزرسانی") },
            text = { Text(updateMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    app.yadavar.shamsi.UpdateChecker.startDownload(context)
                }) { Text("دانلود و نصب") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("بعداً") }
            },
        )
    }
    if (showBattery) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBattery = false; vm.markBatteryAsked() },
            title = { Text("بهینه‌سازی باتری") },
            text = {
                Text("برای اینکه زنگ‌ها و ویجت بدون تأخیر و راحت کار کنند، در صفحهٔ بعد اجازه بده برنامه بدون محدودیت باتری اجرا شود (گزینهٔ «بدون محدودیت» / Unrestricted).")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showBattery = false
                    vm.markBatteryAsked()
                    BatterySettings.requestUnrestricted(context)
                }) { Text("برو به تنظیمات باتری") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBattery = false; vm.markBatteryAsked() }) { Text("بعداً") }
            },
        )
    }
    if (showVoice) {
        VoiceCaptureDialog(
            onText = { said ->
                showVoice = false
                vm.openParsed(parseReminderSpeech(said), said)
            },
            onDismiss = { showVoice = false },
            onUseSystemRecognizer = {
                showVoice = false
                startSystemVoice()
            },
        )
    }
    AnimatedContent(
        targetState = vm.editing,
        contentKey = { it?.id },
        transitionSpec = {
            (fadeIn(softTween(360)) + slideInVertically(softTween(400)) { it / 14 }) togetherWith fadeOut(softTween(220))
        },
        label = "screens",
    ) { editing ->
        if (editing != null) {
            val isNew = reminders.none { it.id == editing.id }
            EditorScreen(
                initial = editing,
                initialSentence = vm.spoken,
                onClose = vm::closeEditor,
                onSave = vm::save,
                onDelete = if (isNew) null else ({ vm.run { Actions.delete(editing.id); vm.closeEditor() } }),
            )
        } else {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (vm.tab == Tab.Home) {
                // "+" opens the new-reminder page directly; the small button above it records a spoken reminder.
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SmallFloatingActionButton(onClick = startVoice) {
                        Icon(Icons.Outlined.Mic, contentDescription = "گفتن یادآور")
                    }
                    FloatingActionButton(onClick = vm::openNew) {
                        Icon(Icons.Filled.Add, contentDescription = "یادآور جدید")
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(Tab.Home, "یادآورها", Icons.Filled.Home),
                    Triple(Tab.Stats, "آمار", Icons.Filled.Widgets),
                    Triple(Tab.Settings, "تنظیمات", Icons.Filled.Tune),
                ).forEach { (tab, label, icon) ->
                    NavigationBarItem(
                        selected = vm.tab == tab,
                        onClick = { vm.tab = tab },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = vm.tab,
            transitionSpec = {
                (fadeIn(softTween(340)) + slideInVertically(softTween(360)) { it / 24 }) togetherWith fadeOut(softTween(200))
            },
            label = "tabs",
        ) { tab ->
            when (tab) {
                Tab.Home -> HomeTab(
                    padding = padding,
                    reminders = reminders,
                    filter = vm.filter,
                    onFilter = { vm.filter = it },
                    onOpen = vm::open,
                    onComplete = { vm.run { if (it.completed) Actions.restore(it.id) else Actions.complete(it.id) } },
                    onExpand = { vm.run { Actions.toggleExpanded(it.id) } },
                    onToggleItem = { reminder, item -> vm.run { Actions.toggleItem(reminder.id, item) } },
                    onRestore = { reminder ->
                        vm.run { Actions.restore(reminder.id) }
                        vm.filter = "all"
                    },
                    onEditDone = { reminder ->
                        vm.run { Actions.restore(reminder.id) }
                        vm.filter = "all"
                        vm.open(reminder.copy(completed = false, completedAt = null))
                    },
                    onDelete = { reminder -> vm.run { Actions.delete(reminder.id) } },
                )
                Tab.Stats -> StatsTab(padding, reminders)
                Tab.Settings -> SettingsTab(padding, settings, vm)
            }
        }
    }
        }
    }
}

@Composable
private fun HomeTab(
    padding: PaddingValues,
    reminders: List<Reminder>,
    filter: String,
    onFilter: (String) -> Unit,
    onOpen: (Reminder) -> Unit,
    onComplete: (Reminder) -> Unit,
    onExpand: (Reminder) -> Unit,
    onToggleItem: (Reminder, String) -> Unit,
    onRestore: (Reminder) -> Unit,
    onEditDone: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    val today = remember { toJalali(Date()) }
    var query by rememberSaveable { mutableStateOf("") }
    val searching = query.isNotBlank()
    // While searching, every active reminder is searched (not only the selected time filter).
    val activeFilter = if (searching && filter != "done") "all" else filter
    val timeFilter = TimeFilter.of(activeFilter)
    val base: List<Reminder> = if (activeFilter == "done") {
        reminders.filter { it.completed }.sortedByDescending { it.completedAt ?: 0L }
    } else {
        filterReminders(reminders, timeFilter)
    }
    val rows: List<Reminder> = if (searching) {
        val needle = normalizeSearch(query)
        base.filter { reminder -> searchHaystack(reminder).contains(needle) }
    } else {
        base
    }
    val grouped = if (activeFilter == "all") {
        Section.entries.map { section -> section to rows.filter { sectionOf(it) == section } }.filter { it.second.isNotEmpty() }
    } else {
        emptyList()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 170.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(formatLongDate(today), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("جستجو در یادآورها") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "پاک کردن جستجو")
                        }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            ChipFlow {
                (TimeFilter.entries.map { it.id to it.label } + ("done" to "انجام‌شده")).forEach { (id, label) ->
                    FilterChip(selected = filter == id, onClick = { onFilter(id) }, label = { Text(label) })
                }
            }
        }
        if (activeFilter == "all") {
            grouped.forEach { (section, list) ->
                item { Text(app.yadavar.shamsi.model.SECTION_LABEL.getValue(section), style = MaterialTheme.typography.titleMedium) }
                items(list, key = { it.id }) { reminder ->
                    ReminderRow(reminder, section, onOpen, onComplete, onExpand, onToggleItem, onRestore, onEditDone, onDelete, Modifier.animateItem(fadeInSpec = softTween(420), placementSpec = softTween(420), fadeOutSpec = softTween(480)))
                }
            }
            if (grouped.isEmpty()) {
                item { Text(if (searching) "نتیجه‌ای پیدا نشد." else "هنوز یادآوری نیست. دکمهٔ + را بزن.") }
            }
        } else {
            items(rows, key = { it.id }) { reminder ->
                ReminderRow(
                    reminder,
                    if (reminder.completed) null else sectionOf(reminder),
                    onOpen,
                    onComplete,
                    onExpand,
                    onToggleItem,
                    onRestore,
                    onEditDone,
                    onDelete,
                    Modifier.animateItem(fadeInSpec = softTween(420), placementSpec = softTween(420), fadeOutSpec = softTween(480)),
                )
            }
            if (rows.isEmpty()) item { Text(if (searching) "نتیجه‌ای پیدا نشد." else "چیزی در این فهرست نیست.") }
        }
    }
}

/** Makes Arabic/Persian letter and digit variants equal so search finds every spelling. */
private fun normalizeSearch(text: String): String {
    val digits = buildString {
        for (ch in text) {
            append(
                when (ch) {
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    else -> ch
                },
            )
        }
    }
    return digits.trim().lowercase().replace('ي', 'ی').replace('ك', 'ک').replace('-', '/')
}

/** Everything searchable about a reminder: title, items, date (several spellings) and time. */
private fun searchHaystack(reminder: Reminder): String {
    val parts = mutableListOf(reminder.title)
    reminder.items.forEach { parts += it.title }
    val date = dateOf(reminder)
    if (date != null) {
        val month2 = date.jm.toString().padStart(2, '0')
        val day2 = date.jd.toString().padStart(2, '0')
        parts += formatLongDate(date)
        parts += formatDayMonth(date)
        parts += "${date.jy}/${date.jm}/${date.jd}"
        parts += "${date.jy}/$month2/$day2"
        parts += "${date.jm}/${date.jd}"
    }
    if (reminder.hour != null && reminder.minute != null) {
        parts += formatTime(reminder.hour, reminder.minute)
        parts += "${reminder.hour}:${reminder.minute.toString().padStart(2, '0')}"
    }
    return normalizeSearch(parts.joinToString(" "))
}

private val OverdueColor = Color(0xFFD96060)
private val TodayColor = Color(0xFF3E9E6E)
private val CheckGreen = Color(0xFF45B27A)

@Composable
private fun ReminderRow(
    reminder: Reminder,
    section: Section?,
    onOpen: (Reminder) -> Unit,
    onComplete: (Reminder) -> Unit,
    onExpand: (Reminder) -> Unit,
    onToggleItem: (Reminder, String) -> Unit,
    onRestore: (Reminder) -> Unit,
    onEditDone: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val status = if (reminder.completed) null else section ?: sectionOf(reminder)
    // Past: soft red. Today: green. Later: the normal text color (white on dark themes).
    val titleColor = when (status) {
        Section.Overdue -> OverdueColor
        Section.Today -> TodayColor
        else -> MaterialTheme.colorScheme.onSurface
    }
    val metaColor = when (status) {
        Section.Overdue -> OverdueColor
        Section.Today -> TodayColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val ringColor = when (status) {
        Section.Overdue -> OverdueColor
        Section.Today -> TodayColor
        else -> MaterialTheme.colorScheme.primary
    }
    // Tick animation: a soft green check is drawn stroke by stroke, then the card fades out of the list.
    val tickScope = rememberCoroutineScope()
    var ticking by remember(reminder.id, reminder.jy, reminder.jm, reminder.jd, reminder.completed) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (ticking || reminder.completed) 1f else 0f,
        animationSpec = tween(420),
        label = "check",
    )
    Card(
        onClick = { onOpen(reminder) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize(softTween())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (reminder.completed) {
                                onRestore(reminder)
                            } else if (!ticking) {
                                ticking = true
                                tickScope.launch {
                                    delay(680)
                                    onComplete(reminder)
                                }
                            }
                        },
                ) {
                    val stroke = 2.dp.toPx()
                    drawCircle(
                        color = if (progress > 0.02f) CheckGreen else ringColor,
                        radius = size.minDimension / 2f - stroke / 2f,
                        style = Stroke(width = stroke),
                    )
                    if (progress > 0f) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.27f, h * 0.53f)
                            lineTo(w * 0.44f, h * 0.69f)
                            lineTo(w * 0.74f, h * 0.34f)
                        }
                        val measure = PathMeasure()
                        measure.setPath(path, false)
                        val drawn = Path()
                        measure.getSegment(0f, measure.length * progress, drawn, true)
                        drawPath(
                            path = drawn,
                            color = CheckGreen,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        reminder.title.ifBlank { "بدون عنوان" },
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                        textDecoration = if (reminder.completed) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = rowMeta(reminder, section, null)
                    if (meta.isNotBlank()) {
                        Text(meta, color = metaColor, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (reminder.items.isNotEmpty()) {
                Text(
                    if (reminder.expanded) "بستن موارد" else "موارد (${app.yadavar.shamsi.model.faNum(reminder.items.size)})",
                    modifier = Modifier.padding(top = 8.dp).clickable { onExpand(reminder) },
                    color = MaterialTheme.colorScheme.primary,
                )
                if (reminder.expanded) {
                    reminder.items.forEach { item ->
                        Row(
                            modifier = Modifier.padding(top = 6.dp).clickable { onToggleItem(reminder, item.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (item.done) "☑" else "☐")
                            Text(
                                item.title,
                                modifier = Modifier.padding(start = 8.dp),
                                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            if (reminder.completed) {
                // Buttons: restore, edit, and permanent delete.
                var showDeleteConfirm by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { onRestore(reminder) }, modifier = Modifier.weight(1f)) { Text("بازگرداندن") }
                    OutlinedButton(onClick = { onEditDone(reminder) }, modifier = Modifier.weight(1f)) { Text("ویرایش") }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("حذف") }
                }
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("مطمئنی؟") },
                        text = { Text("این یادآور برای همیشه حذف می‌شود و قابل بازگردانی نیست.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                onDelete(reminder)
                            }) { Text("بله، حذف کن") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") }
                        },
                    )
                }
            }
        }
    }
}


@Composable
private fun StatsTab(padding: PaddingValues, reminders: List<Reminder>) {
    val now = remember { toJalali(Date()) }
    val thisMonth = reminders.filter { r ->
        val jy = r.jy ?: return@filter false
        val jm = r.jm ?: return@filter false
        jy == now.jy && jm == now.jm
    }
    val done = thisMonth.count { it.completed }
    val overdue = thisMonth.count { !it.completed && sectionOf(it) == Section.Overdue }
    val pending = thisMonth.count { !it.completed && sectionOf(it) != Section.Overdue }
    val total = done + overdue + pending

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("آمار این ماه", style = MaterialTheme.typography.headlineSmall)
        Text(
            "انجام‌شده: $done  |  عقب‌افتاده: $overdue  |  در انتظار: $pending",
            style = MaterialTheme.typography.bodyLarge,
        )
        if (total == 0) {
            Text("هنوز یادآوری برای این ماه ثبت نشده.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Simple bar chart
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBar("انجام‌شده", done, total, Color(0xFF2E9B4F))
                StatBar("عقب‌افتاده", overdue, total, Color(0xFFE05A5A))
                StatBar("در انتظار", pending, total, MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Simple pie-like legend
            Text("نسبت پیشرفت: ${if (total > 0) (done * 100 / total) else 0}٪", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, total: Int, color: Color) {
    val fraction = if (total > 0) value.toFloat() / total else 0f
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("$value")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(color),
            )
        }
    }
}
