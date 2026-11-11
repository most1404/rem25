package app.yadavar.shamsi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

/** Gentle shared animation specs, so every movement in the app feels the same: slow, soft and smooth. */
const val SOFT_MS = 340

val SoftEasing = FastOutSlowInEasing

fun <T> softTween(ms: Int = SOFT_MS) = tween<T>(durationMillis = ms, easing = SoftEasing)

/**
 * A row of small boxes (chips): they sit side by side, move to the next line when there is no room,
 * and every line is centered.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = { content() },
    )
}

/** A chip with a colored dot in front of its name (used for the color choices). */
@Composable
fun ColorChip(label: String, color: Color?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    .border(1.dp, Color(0x66000000), CircleShape),
            )
        },
    )
}

/** Light color for the typing boxes: clearly different from the page behind it. */
@Composable
fun fieldContainerColor(): Color {
    val background = MaterialTheme.colorScheme.background
    return if (background.luminance() > 0.5f) Color.White else lerp(background, Color.White, 0.14f)
}

/** A titled section drawn as a soft rounded box; the divider line below it is drawn by the caller. */
@Composable
fun SectionBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)), RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

/** The thin line that separates two parts of a page. */
@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 2.dp),
        thickness = 1.2.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    )
}

/** Appears and disappears with a soft fade and a smooth change of height. */
@Composable
fun SoftVisibility(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(softTween(300)) + expandVertically(softTween(320)),
        exit = fadeOut(softTween(220)) + shrinkVertically(softTween(260)),
    ) {
        content()
    }
}

/**
 * Dual-column scrollable time picker.
 * Hour uses larger font, minute smaller. User scrolls to select.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 48.dp
    val visibleCount = 3
    val hours = (0..23).toList()
    val minutes = (0..59).toList()
    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = hour.coerceIn(0, 23))
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = minute.coerceIn(0, 59))

    LaunchedEffect(hour) {
        val target = hour.coerceIn(0, 23)
        if (hourState.firstVisibleItemIndex != target) {
            hourState.scrollToItem(target)
        }
    }
    LaunchedEffect(minute) {
        val target = minute.coerceIn(0, 59)
        if (minuteState.firstVisibleItemIndex != target) {
            minuteState.scrollToItem(target)
        }
    }

    LaunchedEffect(hourState) {
        snapshotFlow { hourState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx -> onHourChange(idx.coerceIn(0, 23)) }
    }
    LaunchedEffect(minuteState) {
        snapshotFlow { minuteState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx -> onMinuteChange(idx.coerceIn(0, 59)) }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hour column (larger font)
        Box(
            modifier = Modifier
                .height(itemHeight * visibleCount)
                .width(90.dp),
            contentAlignment = Alignment.Center,
        ) {
            LazyColumn(
                state = hourState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = hourState),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(hours.size) { i ->
                    val selected = hourState.firstVisibleItemIndex == i
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "%02d".format(i),
                            fontSize = if (selected) 34.sp else 22.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }

        Text(
            text = ":",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        // Minute column (smaller font)
        Box(
            modifier = Modifier
                .height(itemHeight * visibleCount)
                .width(90.dp),
            contentAlignment = Alignment.Center,
        ) {
            LazyColumn(
                state = minuteState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = minuteState),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(minutes.size) { i ->
                    val selected = minuteState.firstVisibleItemIndex == i
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "%02d".format(i),
                            fontSize = if (selected) 26.sp else 18.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
    }
}
