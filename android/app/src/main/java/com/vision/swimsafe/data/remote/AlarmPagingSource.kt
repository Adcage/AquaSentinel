package com.vision.swimsafe.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.vision.swimsafe.ui.model.AlarmRecordItem

class AlarmPagingSource(
    private val service: ApiService,
    private val alertStatus: String? = null,
    private val keyword: String? = null,
) : PagingSource<Int, AlarmRecordItem>() {

    override fun getRefreshKey(state: PagingState<Int, AlarmRecordItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AlarmRecordItem> {
        val page = params.key ?: 1
        return runCatching {
            val response = service.listAlerts(
                AlertListRequest(
                    current = page,
                    pageSize = params.loadSize,
                    alertStatus = alertStatus,
                    keyword = keyword,
                )
            ).requireData("加载报警列表失败")

            val items = response.records.map(RemoteMapper::toAlarmRecordItem)
            val totalPages = if (response.total > 0) {
                ((response.total - 1) / params.loadSize) + 1
            } else {
                0
            }

            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (page >= totalPages) null else page + 1,
            )
        }.getOrElse { throw it }
    }
}
