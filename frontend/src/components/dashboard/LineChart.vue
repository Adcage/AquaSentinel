<template>
  <div class="line-chart" ref="chartRef"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import type { ECharts } from 'echarts';

interface MonthlyData {
  month: string
  value: number
}

interface Props {
  data: MonthlyData[]
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  color: '#1890ff'
});

const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: ECharts | null = null;

const initChart = (): void => {
  if (!chartRef.value) return;
  
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.85)',
      borderColor: 'transparent',
      textStyle: {
        color: '#fff'
      },
      formatter: (params: any) => {
        const param = params[0];
        return `
          <div style="font-weight: 500; margin-bottom: 4px;">${param.name}</div>
          <div style="display: flex; justify-content: space-between; gap: 12px;">
            <span style="color: rgba(255, 255, 255, 0.85);">数值</span>
            <span style="font-weight: 600;">${param.value}</span>
          </div>
        `;
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.data && props.data.length > 0 ? props.data.map(item => item.month) : ['无数据'],
      axisLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      },
      axisLabel: {
        color: '#999',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      },
      axisLabel: {
        color: '#999',
        fontSize: 12
      }
    },
    series: [{
      data: props.data && props.data.length > 0 ? props.data.map(item => item.value) : [0],
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: {
        color: props.color
      },
      lineStyle: {
        width: 3,
        color: props.color
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: props.color + '33' },
          { offset: 1, color: props.color + '00' }
        ])
      }
    }]
  };

  chartInstance.setOption(option);
};

const resizeChart = (): void => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

onMounted(() => {
  initChart();
  window.addEventListener('resize', resizeChart);
});

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose();
  }
  window.removeEventListener('resize', resizeChart);
});

watch(() => props.data, () => {
  if (chartInstance) {
    initChart();
  }
}, { deep: true });
</script>

<style scoped>
.line-chart {
  width: 100%;
  height: 100%;
}
</style>
