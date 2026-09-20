<script setup>
import { computed } from 'vue'
import {
  getAssetStatusMeta,
  getLoanStatusMeta,
  getTransferStatusMeta,
} from '@/stores/demoStore'

const props = defineProps({
  status: {
    type: String,
    required: true,
  },
  type: {
    type: String,
    default: 'asset',
  },
})

const meta = computed(() => {
  if (props.type === 'loan') return getLoanStatusMeta(props.status)
  if (props.type === 'transfer') return getTransferStatusMeta(props.status)
  return getAssetStatusMeta(props.status)
})
</script>

<template>
  <el-tag :type="meta.type" effect="light" round>{{ meta.label }}</el-tag>
</template>
