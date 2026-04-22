<template>
  <div class="pie-chart" ref="chartRef"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import type { ECharts } from 'echarts';

interface ChartData {
  name: string
  value: number
}

interface Props {
  data: ChartData[]
}

const props = defineProps<Props>();

const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: ECharts | null = null;

const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1'];

const initChart = (): void => {
  if (!chartRef.value) return;

  if (!chartInstance) {
    chartInstance = echarts.getInstanceByDom(chartRef.value) || echarts.init(chartRef.value);
  }
  
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 0, 0, 0.85)',
      borderColor: 'transparent',
      textStyle: {
        color: '#fff'
      },
      formatter: (params: { name: string; value: number; percent: number }) => {
        return `
          <div style="font-weight: 500; margin-bottom: 4px;">${params.name}</div>
          <div style="display: flex; justify-content: space-between; gap: 12px;">
            <span style="color: rgba(255, 255, 255, 0.85);">数量</span>
            <span style="font-weight: 600;">${params.value.toLocaleString()}</span>
          </div>
          <div style="display: flex; justify-content: space-between; gap: 12px;">
            <span style="color: rgba(255, 255, 255, 0.85);">占比</span>
            <span style="font-weight: 600;">${params.percent}%</span>
          </div>
        `;
      }
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      itemWidth: 12,
      itemHeight: 12,
      formatter: (name: string) => {
        const item = props.data.find(d => d.name === name);
        return `{name|${name}} {value|${item?.value.toLocaleString()}}`;
      },
      textStyle: {
        rich: {
          name: {
            width: 80,
            color: 'rgba(0, 0, 0, 0.65)'
          },
          value: {
            width: 60,
            align: 'right',
            color: 'rgba(0, 0, 0, 0.85)',
            fontWeight: 500
          }
        }
      }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['30%', '50%'],
      avoidLabelOverlap: false,
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: false
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      labelLine: {
        show: false
      },
      data: props.data.map((item, index) => ({
        name: item.name,
        value: item.value,
        itemStyle: {
          color: colors[index % colors.length]
        }
      }))
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
    chartInstance = null;
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
.pie-chart {
  width: 100%;
  height: 300px;
}
</style>

