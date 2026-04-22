<template>
  <div class="date-util-demo">
    <el-card shadow="never">
      <template #header>DateUtil 日期工具演示</template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="格式化" name="format">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>基础格式化</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="当前时间">{{ currentTime }}</el-descriptions-item>
                <el-descriptions-item label="format()">
                  {{ DateUtil.format(currentTime, 'YYYY-MM-DD HH:mm:ss') }}
                </el-descriptions-item>
                <el-descriptions-item label="formatDate()">{{ DateUtil.formatDate(currentTime) }}</el-descriptions-item>
                <el-descriptions-item label="formatTime()">{{ DateUtil.formatTime(currentTime) }}</el-descriptions-item>
                <el-descriptions-item label="formatChinese()">{{ DateUtil.formatChinese(currentTime) }}</el-descriptions-item>
                <el-descriptions-item label="formatChineseDateTime()">
                  {{ DateUtil.formatChineseDateTime(currentTime) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card shadow="never">
              <template #header>智能格式化</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="刚刚">{{ DateUtil.smartFormat(new Date()) }}</el-descriptions-item>
                <el-descriptions-item label="5分钟前">
                  {{ DateUtil.smartFormat(DateUtil.subtract(new Date(), 5, 'minute')) }}
                </el-descriptions-item>
                <el-descriptions-item label="2小时前">
                  {{ DateUtil.smartFormat(DateUtil.subtract(new Date(), 2, 'hour')) }}
                </el-descriptions-item>
                <el-descriptions-item label="3天前">
                  {{ DateUtil.smartFormat(DateUtil.subtract(new Date(), 3, 'day')) }}
                </el-descriptions-item>
                <el-descriptions-item label="1个月前">
                  {{ DateUtil.smartFormat(DateUtil.subtract(new Date(), 1, 'month')) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="相对时间" name="relative">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>相对时间</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="3天前">{{ DateUtil.timeAgo(DateUtil.subtract(new Date(), 3, 'day')) }}</el-descriptions-item>
                <el-descriptions-item label="2小时前">{{ DateUtil.timeAgo(DateUtil.subtract(new Date(), 2, 'hour')) }}</el-descriptions-item>
                <el-descriptions-item label="5分钟前">{{ DateUtil.timeAgo(DateUtil.subtract(new Date(), 5, 'minute')) }}</el-descriptions-item>
                <el-descriptions-item label="3天后">{{ DateUtil.toNow(DateUtil.add(new Date(), 3, 'day')) }}</el-descriptions-item>
                <el-descriptions-item label="2小时后">{{ DateUtil.toNow(DateUtil.add(new Date(), 2, 'hour')) }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="日期判断" name="check">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>时间判断</template>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="是否是今天">
                  <el-tag :type="DateUtil.isToday(new Date()) ? 'success' : 'danger'">
                    {{ DateUtil.isToday(new Date()) ? '是' : '否' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="是否是昨天">
                  <el-tag :type="DateUtil.isYesterday(DateUtil.subtract(new Date(), 1, 'day')) ? 'success' : 'danger'">
                    {{ DateUtil.isYesterday(DateUtil.subtract(new Date(), 1, 'day')) ? '是' : '否' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="是否是本周">
                  <el-tag :type="DateUtil.isThisWeek(new Date()) ? 'success' : 'danger'">
                    {{ DateUtil.isThisWeek(new Date()) ? '是' : '否' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="是否是本月">
                  <el-tag :type="DateUtil.isThisMonth(new Date()) ? 'success' : 'danger'">
                    {{ DateUtil.isThisMonth(new Date()) ? '是' : '否' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="是否是本年">
                  <el-tag :type="DateUtil.isThisYear(new Date()) ? 'success' : 'danger'">
                    {{ DateUtil.isThisYear(new Date()) ? '是' : '否' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="星期几">
                  <el-tag type="primary">{{ DateUtil.getDayName() }}</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card shadow="never">
              <template #header>日期范围判断</template>
              <p>判断 2026-03-06 是否在 2026-03-01 到 2026-03-10 之间：</p>
              <el-tag :type="DateUtil.isBetween('2026-03-06', '2026-03-01', '2026-03-10') ? 'success' : 'danger'">
                {{ DateUtil.isBetween('2026-03-06', '2026-03-01', '2026-03-10') ? '在范围内' : '不在范围内' }}
              </el-tag>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="日期计算" name="calculate">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>添加/减少时间</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="当前时间">{{ DateUtil.format(currentTime) }}</el-descriptions-item>
                <el-descriptions-item label="7天后">{{ DateUtil.format(DateUtil.add(currentTime, 7, 'day')) }}</el-descriptions-item>
                <el-descriptions-item label="2个月后">{{ DateUtil.format(DateUtil.add(currentTime, 2, 'month')) }}</el-descriptions-item>
                <el-descriptions-item label="3天前">{{ DateUtil.format(DateUtil.subtract(currentTime, 3, 'day')) }}</el-descriptions-item>
                <el-descriptions-item label="1年前">{{ DateUtil.format(DateUtil.subtract(currentTime, 1, 'year')) }}</el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card shadow="never">
              <template #header>时间差计算</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="距离2026-03-10还有">
                  {{ DateUtil.diff('2026-03-10', currentTime, 'day') }} 天
                </el-descriptions-item>
                <el-descriptions-item label="距离2026-01-01已过">
                  {{ DateUtil.diff(currentTime, '2026-01-01', 'day') }} 天
                </el-descriptions-item>
                <el-descriptions-item label="本月已过">
                  {{ DateUtil.diff(currentTime, DateUtil.startOfMonth(), 'day') }} 天
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="时间范围" name="range">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>开始/结束时间</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="今天开始">{{ DateUtil.format(DateUtil.startOfToday()) }}</el-descriptions-item>
                <el-descriptions-item label="今天结束">{{ DateUtil.format(DateUtil.endOfToday()) }}</el-descriptions-item>
                <el-descriptions-item label="本周开始">{{ DateUtil.format(DateUtil.startOfWeek()) }}</el-descriptions-item>
                <el-descriptions-item label="本周结束">{{ DateUtil.format(DateUtil.endOfWeek()) }}</el-descriptions-item>
                <el-descriptions-item label="本月开始">{{ DateUtil.format(DateUtil.startOfMonth()) }}</el-descriptions-item>
                <el-descriptions-item label="本月结束">{{ DateUtil.format(DateUtil.endOfMonth()) }}</el-descriptions-item>
                <el-descriptions-item label="本年开始">{{ DateUtil.format(DateUtil.startOfYear()) }}</el-descriptions-item>
                <el-descriptions-item label="本年结束">{{ DateUtil.format(DateUtil.endOfYear()) }}</el-descriptions-item>
              </el-descriptions>
            </el-card>

            <el-card shadow="never">
              <template #header>最近N天</template>
              <p>最近7天的日期范围：</p>
              <el-tag type="primary">
                {{ DateUtil.formatDate(recentDays[0]) }} 至 {{ DateUtil.formatDate(recentDays[1]) }}
              </el-tag>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="时长格式化" name="duration">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>时长格式化</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="30秒">{{ DateUtil.formatDuration(30000) }}</el-descriptions-item>
                <el-descriptions-item label="5分钟">{{ DateUtil.formatDuration(300000) }}</el-descriptions-item>
                <el-descriptions-item label="1小时30分钟">{{ DateUtil.formatDuration(5400000) }}</el-descriptions-item>
                <el-descriptions-item label="1天2小时30分钟">{{ DateUtil.formatDuration(95400000) }}</el-descriptions-item>
                <el-descriptions-item label="7天">{{ DateUtil.formatDuration(604800000) }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-space>
        </el-tab-pane>

        <el-tab-pane label="时间戳" name="timestamp">
          <el-space direction="vertical" :size="16" fill style="width: 100%">
            <el-card shadow="never">
              <template #header>时间戳操作</template>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="当前时间戳（毫秒）">{{ DateUtil.timestamp() }}</el-descriptions-item>
                <el-descriptions-item label="当前时间戳（秒）">{{ DateUtil.timestampSecond() }}</el-descriptions-item>
                <el-descriptions-item label="时间戳转日期">
                  {{ DateUtil.format(DateUtil.fromTimestamp(DateUtil.timestamp())) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-space>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { DateUtil } from '@/utils'

const activeTab = ref('format')
const currentTime = ref(new Date())

const recentDays = computed(() => DateUtil.getRecentDays(7))

setInterval(() => {
  currentTime.value = new Date()
}, 1000)
</script>

<style scoped>
.date-util-demo {
  padding: 24px;
}
</style>
