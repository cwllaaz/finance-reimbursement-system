<script setup>
defineProps({
  label: { type: String, default: '-' },
  type: { type: String, default: 'info' },
})
</script>

<template>
  <el-tag :type="type">{{ label }}</el-tag>
</template>
