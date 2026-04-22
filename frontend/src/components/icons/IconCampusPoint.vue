<template>
  <svg 
    viewBox="0 0 1024 1024" 
    version="1.1" 
    xmlns="http://www.w3.org/2000/svg" 
    :width="size" 
    :height="size"
  >
    <path 
      d="M512 0C312.8 0 151.1 161.7 151.1 360.9c0 288.7 360.9 663.1 360.9 663.1s360.9-374.4 360.9-663.1C872.9 161.7 711.2 0 512 0z m0 541.4c-99.7 0-180.5-80.8-180.5-180.5S412.3 180.5 512 180.5s180.5 80.8 180.5 180.5-80.8 180.5-180.5 180.5z" 
      :fill="color"
    ></path>
  </svg>
</template>

<script setup lang="ts">
interface Props {
  size?: number | string;
  color?: string;
}

withDefaults(defineProps<Props>(), {
  size: 16,
  color: 'currentColor'
});
</script>
