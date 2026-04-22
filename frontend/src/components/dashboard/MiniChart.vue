<template>
  <div class="mini-chart" ref="chartRef"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import type { ECharts } from 'echarts';

interface Props {
  data: number[]
  type?: 'line' | 'area' | 'bar'
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  type: 'line'
});

const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: ECharts | null = null;

const initChart = (): void => {
  if (!chartRef.value) return;
  
  chartInstance = echarts.init(chartRef.value);
  
  const option: echarts.EChartsOption = {
    grid: {
      left: 0,
      right: 0,
      top: 0,
      bottom: 0
    },
    xAxis: {
      type: 'category',
      show: false,
      data: props.data.map((_, i) => i)
    },
    yAxis: {
      type: 'value',
      show: false
    },
    series: []
  };

  if (props.type === 'line') {
    option.series = [{
      data: props.data,
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: '#1890ff',
        width: 2
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(24, 144, 255, 0.2)' },
            { offset: 1, color: 'rgba(24, 144, 255, 0)' }
          ]
        }
      }
    }];
  } else if (props.type === 'area') {
    option.series = [{
      data: props.data,
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: '#975fe4',
        width: 2
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(151, 95, 228, 0.4)' },
            { offset: 1, color: 'rgba(151, 95, 228, 0.1)' }
          ]
        }
      }
    }];
  } else if (props.type === 'bar') {
    option.series = [{
      data: props.data,
      type: 'bar',
      itemStyle: {
        color: '#1890ff',
        borderRadius: [2, 2, 0, 0]
      },
      barWidth: '60%'
    }];
  }

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
.mini-chart {
  width: 100%;
  height: 46px;
}
</style>

