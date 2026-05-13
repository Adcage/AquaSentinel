<script setup lang="ts">
interface Props {
  size?: string | number;
}

withDefaults(defineProps<Props>(), {
  size: 64,
});
</script>

<template>
  <img
    src="/logo.svg"
    alt="AquaSentinel Logo"
    class="logo-icon"
    :style="{
      width: typeof size === 'number' ? `${size}px` : size,
      height: typeof size === 'number' ? `${size}px` : size,
    }"
  />
</template>

<style scoped>
.logo-icon {
  display: inline-block;
  object-fit: contain;
  flex-shrink: 0;
}
</style>
