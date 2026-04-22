<template>
  <div class="bar-chart" ref="chartRef"></div>
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
}

const props = defineProps<Props>();

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
      axisPointer: {
        type: 'shadow'
      },
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
            <span style="color: rgba(255, 255, 255, 0.85);">数据量</span>
            <span style="font-weight: 600;">${param.value}</span>
          </div>
        `;
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: props.data.length > 8 ? '18%' : '8%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: props.data && props.data.length > 0 ? props.data.map(item => item.month) : ['无数据'],
      axisLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      },
      axisLabel: {
        color: '#999',
        fontSize: 12,
        interval: 0,
        hideOverlap: false,
        rotate: props.data.length > 8 ? 30 : 0
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
      type: 'bar',
      itemStyle: {
        color: '#1890ff',
        borderRadius: [4, 4, 0, 0]
      },
      emphasis: {
        itemStyle: {
          color: '#40a9ff'
        }
      },
      barWidth: '40%'
    }],
    dataZoom: props.data.length > 8 ? [
      {
        type: 'inside',
        xAxisIndex: 0,
        start: 0,
        end: 100
      },
      {
        type: 'slider',
        xAxisIndex: 0,
        bottom: 0,
        height: 14,
        start: 0,
        end: 100
      }
    ] : []
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
.bar-chart {
  width: 100%;
  height: 100%;
}
</style>

