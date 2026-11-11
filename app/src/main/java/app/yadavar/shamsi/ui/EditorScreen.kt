package app.yadavar.shamsi.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.yadavar.shamsi.jalali.JDate
import app.yadavar.shamsi.jalali.WEEKDAY_NAMES
import app.yadavar.shamsi.jalali.addDays
import app.yadavar.shamsi.jalali.dateEpoch
import app.yadavar.shamsi.jalali.iranWeekday
import app.yadavar.shamsi.jalali.toJalali
import app.yadavar.shamsi.model.WORKDAYS_MASK
import app.yadavar.shamsi.model.has
import app.yadavar.shamsi.model.toggle
import app.yadavar.shamsi.model.CheckItem
import app.yadavar.shamsi.model.Reminder
import app.yadavar.shamsi.model.Repeat
import app.yadavar.shamsi.model.formatLongDate
import app.yadavar.shamsi.model.formatTime
import app.yadavar.shamsi.model.leadLabel
import app.yadavar.shamsi.model.repeatLabel
import app.yadavar.shamsi.voice.parseReminderSpeech
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    initial: Reminder,
    initialSentence: String = "",
    onClose: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var title by remember(initial.id) { mutableStateOf(initial.title) }
    var items by remember(initial.id) { mutableStateOf(initial.items) }
    var draftItem by remember(initial.id) { mutableStateOf("") }
    var jy by remember(initial.id) { mutableStateOf(initial.jy) }
    var jm by remember(initial.id) { mutableStateOf(initial.jm) }
    var jd by remember(initial.id) { mutableStateOf(initial.jd) }
    var hour by remember(initial.id) { mutableStateOf(initial.hour) }
    var minute by remember(initial.id) { mutableStateOf(initial.minute) }
    var repeat by remember(initial.id) { mutableStateOf(initial.repeat) }
    var sentence by remember(initial.id) { mutableStateOf(initialSentence) }
    var showDate by remember(initial.id) { mutableStateOf(false) }
    var showTime by remember(initial.id) { mutableStateOf(false) }
    var tempHour by remember(initial.id) { mutableStateOf(initial.hour ?: 9) }
    var tempMinute by remember(initial.id) { mutableStateOf(initial.minute ?: 0) }
    var lead by remember(initial.id) { mutableStateOf(initial.leadMinutes) }
    var showLead by remember(initial.id) { mutableStateOf(false) }
    var leadText by remember(initial.id) { mutableStateOf("") }
    var error by remember(initial.id) { mutableStateOf<String?>(null) }
    val today = remember { toJalali(Date()) }
    var visible by remember(initial.id) {
        mutableStateOf(JDate(jy ?: today.jy, jm ?: today.jm, 1))
    }

    fun applySpeech() {
        val parsed = parseReminderSpeech(sentence)
        if (parsed.title.isNotBlank()) title = parsed.title
        if (parsed.jy != null && parsed.jm != null && parsed.jd != null) {
            jy = parsed.jy
            jm = parsed.jm
            jd = parsed.jd
            visible = JDate(parsed.jy, parsed.jm, 1)
        }
        if (parsed.hour != null && parsed.minute != null) {
            hour = parsed.hour
            minute = parsed.minute
        }
        val spokenRepeat = parsed.repeatKind
        if (spokenRepeat != null) repeat = Repeat.of(spokenRepeat, 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (onDelete == null) "یادآور جدید" else "ویرایش") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "بستن") }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "حذف") }
                    }
                },
            )
        },
    ) { padding ->
        val boxColor = fieldContainerColor()
        val primary = MaterialTheme.colorScheme.primary
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = boxColor,
            unfocusedContainerColor = boxColor,
            focusedBorderColor = primary,
            unfocusedBorderColor = primary.copy(alpha = 0.55f),
        )
        val fieldShape = RoundedCornerShape(18.dp)
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The typing box with the small "extract from sentence" tab hanging half inside, half below its bottom edge.
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(bottom = 20.dp)) {
                    OutlinedTextField(
                        value = sentence,
                        onValueChange = { sentence = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("جمله، مثل: فردا ساعت ۷ صبح قرص") },
                        minLines = 3,
                        shape = fieldShape,
                        colors = fieldColors,
                    )
                }
                Button(
                    onClick = ::applySpeech,
                    modifier = Modifier.align(Alignment.BottomCenter).height(40.dp),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                ) { Text("درآوردن از جمله", style = MaterialTheme.typography.labelLarge) }
            }
            SoftDivider()

            // Title, added items, Jalali date and time: one group.
            SectionBox {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان") },
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors,
                )
                Column(modifier = Modifier.animateContentSize(softTween())) {
                    items.forEach { item ->
                        androidx.compose.runtime.key(item.id) {
                            var shown by remember { mutableStateOf(false) }
                            androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
                            AnimatedVisibility(
                                visible = shown,
                                enter = fadeIn(softTween(300)) + expandVertically(softTween(300)),
                                exit = fadeOut(softTween(200)) + shrinkVertically(softTween(240)),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (item.done) "☑" else "☐", modifier = Modifier.clickable {
                                        items = items.map { if (it.id == item.id) it.copy(done = !it.done) else it }
                                    })
                                    Text(item.title, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                                    IconButton(onClick = { items = items.filter { it.id != item.id } }) {
                                        Icon(Icons.Filled.Close, contentDescription = "حذف مورد")
                                    }
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draftItem,
                        onValueChange = { draftItem = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("موارد افزودنی") },
                        placeholder = { Text("مورد جدید") },
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors,
                    )
                    TextButton(onClick = {
                        val clean = draftItem.trim()
                        if (clean.isEmpty()) return@TextButton
                        items = items + CheckItem(UUID.randomUUID().toString(), clean, false)
                        draftItem = ""
                    }) { Text("افزودن") }
                }
                val date = if (jy != null && jm != null && jd != null) JDate(jy!!, jm!!, jd!!) else null
                // First child sits on the right in this right-to-left screen: Jalali date on the right, time on the left.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                        Text(if (date == null) "تاریخ شمسی" else formatLongDate(date), maxLines = 2)
                    }
                    OutlinedButton(
                        onClick = {
                            tempHour = hour ?: 9
                            tempMinute = minute ?: 0
                            showTime = true
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (hour == null || minute == null) "ساعت" else formatTime(hour!!, minute!!)) }
                }
                SoftVisibility(visible = hour != null) {
                    TextButton(onClick = { hour = null; minute = null; lead = null }) { Text("حذف ساعت") }
                }
            }
            SoftDivider()

            // Early notice: its own box.
            SectionBox {
                Text("اطلاع قبل از موعد", style = MaterialTheme.typography.titleSmall)
                ChipFlow {
                    val timeSet = hour != null
                    val leadNow = lead
                    val custom = leadNow != null && leadNow != 15 && leadNow != 30 && leadNow != 60
                    FilterChip(
                        selected = custom,
                        enabled = timeSet,
                        onClick = {
                            leadText = if (custom && leadNow != null) leadNow.toString() else ""
                            showLead = true
                        },
                        label = { Text(if (custom && leadNow != null) "دلخواه: " + leadLabel(leadNow) else "دلخواه") },
                    )
                    listOf(15 to "۱۵ دقیقه", 30 to "۳۰ دقیقه", 60 to "۱ ساعت").forEach { (minutes, label) ->
                        FilterChip(selected = lead == minutes, enabled = timeSet, onClick = { lead = minutes }, label = { Text(label) })
                    }
                    FilterChip(
                        selected = lead == null && !custom,
                        enabled = timeSet,
                        onClick = { lead = null },
                        label = { Text("بدون") },
                    )
                }
                SoftVisibility(visible = hour == null) {
                    Text(
                        "برای اطلاع قبل از موعد، اول ساعت را انتخاب کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SoftDivider()

            Text("تکرار", style = MaterialTheme.typography.titleSmall)
            ChipFlow {
                listOf(
                    "بدون تکرار" to Repeat.None,
                    "هر روز" to Repeat.Every(Repeat.Kind.Daily, 1),
                    "هر هفته" to Repeat.Every(Repeat.Kind.Weekly, 1),
                    "هر ماه شمسی" to Repeat.Every(Repeat.Kind.Monthly, 1),
                    "هر سال شمسی" to Repeat.Every(Repeat.Kind.Yearly, 1),
                ).forEach { (label, value) ->
                    FilterChip(
                        selected = repeat.kindName == value.kindName,
                        onClick = { repeat = value },
                        label = { Text(label) },
                    )
                }
                FilterChip(
                    selected = repeat is Repeat.Days,
                    onClick = { if (repeat !is Repeat.Days) repeat = Repeat.Days(WORKDAYS_MASK) },
                    label = { Text("تنظیم دستی") },
                )
            }
            // Manual repeat: tick the weekdays (for example every day except Friday).
            val manual = repeat as? Repeat.Days
            SoftVisibility(visible = manual != null) {
                val shownDays = manual ?: Repeat.Days(WORKDAYS_MASK)
                SectionBox {
                    Text("روزهای هفته را تیک بزن", style = MaterialTheme.typography.titleSmall)
                    ChipFlow {
                        (0..6).forEach { day ->
                            val on = shownDays.has(day)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { (repeat as? Repeat.Days)?.let { repeat = it.toggle(day) } }
                                    .padding(end = 10.dp),
                            ) {
                                Checkbox(checked = on, onCheckedChange = { (repeat as? Repeat.Days)?.let { repeat = it.toggle(day) } })
                                Text(WEEKDAY_NAMES[day])
                            }
                        }
                    }
                }
            }
            Text(repeatLabel(repeat), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.animateContentSize(softTween()))
            SoftVisibility(visible = error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanItems = items.filter { it.title.isNotBlank() }
                    if (cleanTitle.isEmpty() && cleanItems.isEmpty()) {
                        error = "عنوان یا حداقل یک مورد لازم است"
                        return@Button
                    }
                    if ((hour != null || minute != null) && (jy == null || jm == null || jd == null)) {
                        error = "اگر ساعت مشخص کردی، تاریخ هم لازم است"
                        return@Button
                    }
                    error = null
                    var saveY = jy
                    var saveM = jm
                    var saveD = jd
                    val manualRepeat = repeat as? Repeat.Days
                    if (manualRepeat != null && (saveY == null || saveM == null || saveD == null)) {
                        // A manual weekday repeat without a date starts on the next chosen day.
                        var cursor = today
                        var guard = 0
                        val hh = hour
                        val mm = minute
                        val laterToday = hh != null && mm != null &&
                            dateEpoch(today.jy, today.jm, today.jd, hh, mm) > System.currentTimeMillis()
                        if (!laterToday) {
                            cursor = addDays(cursor, 1)
                        }
                        while (!manualRepeat.has(iranWeekday(cursor)) && guard < 7) {
                            cursor = addDays(cursor, 1)
                            guard++
                        }
                        saveY = cursor.jy
                        saveM = cursor.jm
                        saveD = cursor.jd
                    }
                    onSave(
                        initial.copy(
                            title = cleanTitle,
                            items = cleanItems,
                            jy = saveY,
                            jm = saveM,
                            jd = saveD,
                            hour = hour,
                            minute = minute,
                            // The on-time alarm is always on; a reminder with a date and time always rings.
                            alert = true,
                            repeat = repeat,
                            leadMinutes = if (hour != null) lead else null,
                            anchorDay = if (saveY == initial.jy && saveM == initial.jm && saveD == initial.jd) initial.anchorDay ?: saveD else saveD,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ذخیره") }
        }
    }

    if (showLead) {
        AlertDialog(
            onDismissRequest = { showLead = false },
            title = { Text("چند دقیقه قبل از موعد؟") },
            text = {
                OutlinedTextField(
                    value = leadText,
                    onValueChange = { leadText = it },
                    label = { Text("دقیقه (۱ تا ۱۰۰۸۰)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ascii = buildString {
                        leadText.forEach { ch ->
                            append(
                                when (ch) {
                                    in '۰'..'۹' -> '0' + (ch - '۰')
                                    in '٠'..'٩' -> '0' + (ch - '٠')
                                    else -> ch
                                },
                            )
                        }
                    }
                    val minutes = ascii.trim().toIntOrNull()
                    if (minutes != null && minutes in 1..10080) {
                        lead = minutes
                        showLead = false
                    }
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton(onClick = { showLead = false }) { Text("انصراف") } },
        )
    }

    if (showDate) {
        val selected = if (jy != null && jm != null && jd != null) JDate(jy!!, jm!!, jd!!) else null
        AlertDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { showDate = false }) { Text("باشه") } },
            dismissButton = {
                TextButton(onClick = {
                    jy = null
                    jm = null
                    jd = null
                    showDate = false
                }) { Text("حذف تاریخ") }
            },
            text = {
                MonthGrid(
                    visible = visible,
                    selected = selected,
                    onSelect = {
                        jy = it.jy
                        jm = it.jm
                        jd = it.jd
                        visible = JDate(it.jy, it.jm, 1)
                    },
                    onMonth = { visible = JDate(it.jy, it.jm, 1) },
                )
            },
        )
    }

    if (showTime) {
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("انتخاب ساعت") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ScrollTimePicker(
                        hour = tempHour,
                        minute = tempMinute,
                        onHourChange = { tempHour = it },
                        onMinuteChange = { tempMinute = it },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hour = tempHour
                    minute = tempMinute
                    showTime = false
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("انصراف") }
            },
        )
    }
}
