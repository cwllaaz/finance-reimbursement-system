<script setup>
defineProps({
  loading: Boolean,
  emptyText: { type: String, default: '暂无数据' },
})
</script>

<template>
  <div v-loading="loading" class="data-table-shell">
    <slot v-if="$slots.default" />
    <el-empty v-else :description="emptyText" />
  </div>
</template>
