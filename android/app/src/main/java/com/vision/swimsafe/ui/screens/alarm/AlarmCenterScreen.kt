package com.vision.swimsafe.ui.screens.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.vision.swimsafe.data.remote.RemoteAlarmRepository
import com.vision.swimsafe.ui.components.common.AppTopBar
import com.vision.swimsafe.ui.components.common.StatusChip
import com.vision.swimsafe.ui.model.AlarmRecordItem
import com.vision.swimsafe.ui.theme.AlarmRed
import com.vision.swimsafe.ui.theme.AndroidTheme
import com.vision.swimsafe.ui.theme.AppDimens
import com.vision.swimsafe.ui.theme.CardBackground
import com.vision.swimsafe.ui.theme.PageBackground
import com.vision.swimsafe.ui.theme.TextPrimary
import com.vision.swimsafe.ui.theme.TextSecondary

@Composable
fun AlarmCenterScreen(
    modifier: Modifier = Modifier,
    onOpenAlarmDetail: (String) -> Unit,
) {
    val pagingItems = remember { RemoteAlarmRepository.getAlarmCenterPagingData() }.collectAsLazyPagingItems()
    val highlightedAlarm = remember(pagingItems.itemCount) {
        pagingItems.itemCount > 0
    }

    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        AppTopBar(
            title = "报警中心",
            containerColor = CardBackground,
            titleColor = TextPrimary,
        )
        AlarmCenterContent(
            pagingItems = pagingItems,
            showHighlightedAlarm = highlightedAlarm,
            onOpenAlarmDetail = onOpenAlarmDetail,
        )
    }
}

@Composable
private fun AlarmCenterContent(
    pagingItems: LazyPagingItems<AlarmRecordItem>,
    showHighlightedAlarm: Boolean,
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

    when {
        pagingItems.loadState.refresh is LoadState.Loading && pagingItems.itemCount == 0 -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        pagingItems.loadState.refresh is LoadState.Error && pagingItems.itemCount == 0 -> {
            val error = (pagingItems.loadState.refresh as LoadState.Error).error
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(AppDimens.spacingMd),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(AppDimens.spacingLg),
            ) {
                if (showHighlightedAlarm && pagingItems.itemCount > 0) {
                    item {
                        val firstItem = pagingItems[0]
                        if (firstItem != null) {
                            HighlightedAlarmCard(alarm = firstItem)
                        }
                    }
                }

                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.id },
                ) { index ->
                    val item = pagingItems[index]
                    if (item != null) {
                        if (index == 0 && showHighlightedAlarm) {
                        } else {
                            AlarmRecordRow(
                                item = item,
                                onClick = { onOpenAlarmDetail(item.id) },
                            )
                        }
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
                            TextButton(onClick = { pagingItems.retry() }) {
                                Text("加载更多失败，点击重试")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        content()
    }
}

@Composable
private fun HighlightedAlarmCard(alarm: AlarmRecordItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.cardRadius),
    ) {
        Column(modifier = Modifier.padding(AppDimens.spacingLg)) {
            Text(
                text = "当前高优先级报警",
                style = MaterialTheme.typography.titleMedium,
                color = AlarmRed,
            )
            Text(text = alarm.type, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = alarm.cameraName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Text(
                text = "请立即前往现场确认",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun AlarmRecordRow(
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

@Preview(showBackground = true)
@Composable
private fun AlarmCenterScreenPreview() {
    AndroidTheme {
        AlarmCenterScreen(onOpenAlarmDetail = {})
    }
}
