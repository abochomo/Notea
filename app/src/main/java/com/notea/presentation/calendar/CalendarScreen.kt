package com.notea.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notea.R
import com.notea.domain.model.CalendarDaySummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarRoute(
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val summary = viewModel.monthSummary.collectAsStateWithLifecycle().value
    CalendarScreen(
        month = viewModel.currentMonth(),
        summary = summary,
        onBack = onBack,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToday = viewModel::goToToday,
        onOpenDay = onOpenDay
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    month: YearMonth,
    summary: Map<LocalDate, CalendarDaySummary>,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    LaunchedEffect(month) {
        if (YearMonth.from(selectedDate) != month) {
            selectedDate = month.atDay(1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalendarMonthHeader(
                month = month,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onToday = {
                    onToday()
                    selectedDate = today
                }
            )

            WeekdayHeader()

            val days = remember(month) {
                val firstDay = month.atDay(1)
                val leadingBlanks = firstDay.dayOfWeek.value - 1
                buildList<LocalDate?> {
                    repeat(leadingBlanks) { add(null) }
                    repeat(month.lengthOfMonth()) { add(month.atDay(it + 1)) }
                    while (size % 7 != 0) add(null)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                userScrollEnabled = false
            ) {
                items(days) { date ->
                    CalendarDayCell(
                        date = date,
                        summary = date?.let { summary[it] },
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        onSelect = { selectedDate = it }
                    )
                }
            }

            CalendarLegend()
            SelectedDayCard(
                selectedDate = selectedDate,
                summary = summary[selectedDate],
                onOpenDay = onOpenDay
            )
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-ES"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.a11y_previous_month))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = month.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.calendar_today))
            }
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.a11y_next_month))
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf(
        stringResource(R.string.calendar_weekday_monday),
        stringResource(R.string.calendar_weekday_tuesday),
        stringResource(R.string.calendar_weekday_wednesday),
        stringResource(R.string.calendar_weekday_thursday),
        stringResource(R.string.calendar_weekday_friday),
        stringResource(R.string.calendar_weekday_saturday),
        stringResource(R.string.calendar_weekday_sunday)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    summary: CalendarDaySummary?,
    isToday: Boolean,
    isSelected: Boolean,
    onSelect: (LocalDate) -> Unit
) {
    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }
    val backgroundColor = when {
        date == null -> MaterialTheme.colorScheme.background
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isToday -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .border(2.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable(enabled = date != null) {
                if (date != null) onSelect(date)
            },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )
                ActivityIndicators(summary)
            }
        }
    }
}

@Composable
private fun ActivityIndicators(summary: CalendarDaySummary?) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if ((summary?.notes ?: 0) > 0) {
            ActivityDot(color = NoteColor)
        }
        if ((summary?.reminders ?: 0) > 0) {
            ActivityDot(color = ReminderColor)
        }
        if ((summary?.routines ?: 0) > 0) {
            ActivityDot(color = RoutineColor)
        }
    }
}

@Composable
private fun ActivityDot(color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun CalendarLegend() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(label = stringResource(R.string.home_stat_notes), color = NoteColor)
            LegendItem(label = stringResource(R.string.home_stat_reminders), color = ReminderColor)
            LegendItem(label = stringResource(R.string.home_stat_routines), color = RoutineColor)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActivityDot(color = color)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SelectedDayCard(
    selectedDate: LocalDate,
    summary: CalendarDaySummary?,
    onOpenDay: (LocalDate) -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = selectedDate.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.calendar_notes_count, summary?.notes ?: 0))
            Text(stringResource(R.string.calendar_reminders_count, summary?.reminders ?: 0))
            Text(stringResource(R.string.calendar_routines_count, summary?.routines ?: 0))
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenDay(selectedDate) }
            ) {
                Text(stringResource(R.string.calendar_open_day_detail))
            }
        }
    }
}

private val NoteColor = Color(0xFF2E7D32)
private val ReminderColor = Color(0xFFEF6C00)
private val RoutineColor = Color(0xFF1565C0)
