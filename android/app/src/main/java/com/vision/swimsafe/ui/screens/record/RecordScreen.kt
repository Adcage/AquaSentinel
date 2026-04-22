package com.vision.swimsafe.ui.screens.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.vision.swimsafe.data.remote.RemoteAlarmRepository
import com.vision.swimsafe.ui.components.common.AppTopBar
import com.vision.swimsafe.ui.components.common.EmptyState
import com.vision.swimsafe.ui.components.common.StatusChip
import com.vision.swimsafe.ui.model.AlarmRecordItem
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.TextPrimary
import com.vision.swimsafe.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val queryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    onOpenAlarmDetail: (String) -> Unit,
) {
    var selectedTimeFilter by remember { mutableStateOf("今日") }
    var selectedStatusFilter by remember { mutableStateOf("全部") }
    var selectedDate by remember { mutableStateOf(ALL_DATE_OPTION) }
    var showDatePicker by remember { mutableStateOf(false) }
    var cameraQuery by remember { mutableStateOf("") }

    val alertStatus = when (selectedStatusFilter) {
        "未处理" -> "PENDING"
        "已处理" -> "RESOLVED"
        "误报" -> "FALSE_ALARM"
        else -> null
    }

    val pagingData = remember(
        selectedTimeFilter,
        selectedStatusFilter,
        selectedDate,
        cameraQuery,
    ) {
        RemoteAlarmRepository.getAlarmRecordPagingData(
            alertStatus = alertStatus,
            keyword = cameraQuery.takeIf { it.isNotBlank() },
        )
    }.collectAsLazyPagingItems()

    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        AppTopBar(
            title = "报警记录",
            containerColor = CardBackground,
            titleColor = TextPrimary,
        )
        RecordContent(
            pagingItems = pagingData,
            selectedTimeFilter = selectedTimeFilter,
            selectedStatusFilter = selectedStatusFilter,
            selectedDate = selectedDate,
            cameraQuery = cameraQuery,
            showDatePicker = showDatePicker,
            onTimeFilterChange = { selectedTimeFilter = it },
            onStatusFilterChange = { selectedStatusFilter = it },
            onDateChange = { selectedDate = it },
            onShowDatePicker = { showDatePicker = it },
            onCameraQueryChange = { cameraQuery = it },
            onOpenAlarmDetail = onOpenAlarmDetail,
        )
    }
}

@Composable
private fun RecordContent(
    pagingItems: LazyPagingItems<AlarmRecordItem>,
    selectedTimeFilter: String,
    selectedStatusFilter: String,
    selectedDate: String,
    cameraQuery: String,
    showDatePicker: Boolean,
    onTimeFilterChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onShowDatePicker: (Boolean) -> Unit,
    onCameraQueryChange: (String) -> Unit,
    onOpenAlarmDetail: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index as Int? }
            .collect { lastVisibleIndex: Int? ->
                if (lastVisibleIndex != null && lastVisibleIndex >= pagingItems.itemCount - 5) {
                    pagingItems.retry()
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
        contentPadding = PaddingValues(AppDimens.spacingLg),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.cardRadius),
            ) {
                Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
                    FilterRow(
                        title = "时间",
                        items = listOf("今日", "近7天", "近30天"),
                        selected = selectedTimeFilter,
                        onSelect = onTimeFilterChange,
                    )
                    FilterRow(
                        title = "状态",
                        items = listOf("全部", "未处理", "已处理", "误报"),
                        selected = selectedStatusFilter,
                        onSelect = onStatusFilterChange,
                    )
                    DateSelectField(
                        selected = selectedDate,
                        onOpenPicker = { onShowDatePicker(true) },
                        onClearDate = { onDateChange(ALL_DATE_OPTION) },
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spacingMd))
                    OutlinedTextField(
                        value = cameraQuery,
                        onValueChange = onCameraQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("按摄像头编号搜索（如 2号机位）") },
                    )
                }
            }
        }

        when {
            pagingItems.loadState.refresh is LoadState.Loading && pagingItems.itemCount == 0 -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spacingLg),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
                val error = (pagingItems.loadState.refresh as LoadState.Error).error
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.spacingLg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "加载失败", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = error.localizedMessage ?: "未知错误",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
            pagingItems.itemCount == 0 -> {
                item {
                    EmptyState(title = "未找到报警记录", subtitle = "请调整日期或摄像头编号")
                }
            }
            else -> {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    val item = pagingItems[index]
                    if (item != null) {
                        RecordRow(
                            item = item,
                            onClick = { onOpenAlarmDetail(item.id) },
                        )
                    }
                }

                if (pagingItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.spacingLg),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (pagingItems.loadState.append is LoadState.Error) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.spacingLg),
                            contentAlignment = Alignment.Center,
                        ) {
                            RetryButton(onClick = { pagingItems.retry() })
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        CalendarDatePickerDialog(
            initialDate = parseSelectedDate(selectedDate),
            onDismiss = { onShowDatePicker(false) },
            onConfirm = { date ->
                onDateChange(date?.format(queryDateFormatter) ?: ALL_DATE_OPTION)
                onShowDatePicker(false)
            },
        )
    }
}

@Composable
private fun RetryButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("加载更多失败，点击重试")
    }
}

@Composable
private fun RecordRow(
    item: AlarmRecordItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(CardBackground, RoundedCornerShape(AppDimens.cardRadius))
            .padding(AppDimens.spacingLg),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            StatusChip(text = item.type)
            Text(text = item.cameraName, style = MaterialTheme.typography.bodyLarge)
            Text(text = item.time, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        StatusChip(text = item.status)
    }
}

@Composable
private fun CalendarDatePickerDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?) -> Unit,
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate ?: LocalDate.now())) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(AppDimens.cardRadius)) {
            Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
                Text(text = "选择日期", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(AppDimens.spacingMd))
                Text(
                    text = selectedDate?.format(queryDateFormatter) ?: "未选择日期",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(AppDimens.spacingLg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buildCalendarMonthTitle(displayedMonth),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row {
                        IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "上个月")
                        }
                        IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "下个月")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.spacingSm))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    chineseWeekdayLabels().forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.spacingSm))

                buildMonthDayGrid(displayedMonth).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { dayOfMonth ->
                            val date = dayOfMonth?.let { displayedMonth.atDay(it) }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                DayCell(
                                    dayText = dayOfMonth?.toString().orEmpty(),
                                    selected = selectedDate == date,
                                    enabled = date != null,
                                    onClick = {
                                        if (date != null) {
                                            selectedDate = date
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.spacingLg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    TextButton(onClick = { onConfirm(selectedDate) }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayText: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(vertical = AppDimens.spacingSm)
            .size(36.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else PageBackground,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayText,
            color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun parseSelectedDate(value: String): LocalDate? {
    if (value.isBlank() || value == ALL_DATE_OPTION) {
        return null
    }
    return runCatching { LocalDate.parse(value, queryDateFormatter) }.getOrNull()
}

@Composable
private fun DateSelectField(
    selected: String,
    onOpenPicker: () -> Unit,
    onClearDate: () -> Unit,
) {
    Box {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text("按日期搜索") },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected != ALL_DATE_OPTION) {
                        TextButton(onClick = onClearDate) {
                            Text("清除")
                        }
                    }
                    IconButton(onClick = onOpenPicker) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "选择日期",
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun FilterRow(
    title: String,
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = AppDimens.spacingMd)) {
        Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.spacingSm)) {
            items.forEach { item ->
                val isSelected = item == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(item) },
                    label = { Text(item) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PageBackground,
                        labelColor = TextSecondary,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordScreenPreview() {
    AndroidTheme {
        RecordScreen(onOpenAlarmDetail = {})
    }
}
