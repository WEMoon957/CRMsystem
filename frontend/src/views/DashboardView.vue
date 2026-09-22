<template>
  <div v-loading="loading">
    <el-row :gutter="16" class="cards">
      <el-col :span="6" v-for="opt in INTENT_OPTIONS" :key="opt.value">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-num" :class="`tone-${opt.tone}`">{{ stats[opt.value] ?? 0 }}</div>
            <div class="stat-label">{{ opt.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>意向分布</template>
      <div v-for="opt in INTENT_OPTIONS" :key="opt.value" class="bar-row">
        <span class="bar-label">{{ opt.label }}</span>
        <el-progress
          :percentage="percentage(stats[opt.value] ?? 0)"
          :stroke-width="16"
          :color="colorOf(opt.tone)"
          class="bar"
        />
        <span class="bar-num">{{ stats[opt.value] ?? 0 }}</span>
      </div>
      <el-empty v-if="total === 0" description="暂无客户数据" />
    </el-card>

    <el-card shadow="never" class="desc-card">
      <template #header>权限说明</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="主账号">
          查看与管理全部客户资料与意向信息；可新增/禁用子账号；可将客户分配给任意子账号
        </el-descriptions-item>
        <el-descriptions-item label="子账号">
          拥有客户添加与管理功能，但仅能看到并操作自己名下的客户数据
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { customerApi } from '@/api'
import { friendlyError } from '@/api/http'
import { INTENT_OPTIONS } from '@/types'

const loading = ref(false)
const stats = ref<Record<string, number>>({})

const total = computed(() =>
  Object.values(stats.value).reduce((a, b) => a + b, 0))

function percentage(v: number): number {
  if (total.value === 0) return 0
  return Math.round((v / total.value) * 100)
}

function colorOf(tone: string): string {
  const map: Record<string, string> = {
    info: '#909399', success: '#67c23a', warning: '#e6a23c',
    primary: '#409eff', danger: '#f56c6c'
  }
  return map[tone] ?? '#409eff'
}

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await customerApi.stats()
  } catch (e) {
    friendlyError(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.cards { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-num { font-size: 34px; font-weight: 700; }
.tone-info { color: #909399; }
.tone-success { color: #67c23a; }
.tone-warning { color: #e6a23c; }
.tone-primary { color: #409eff; }
.tone-danger { color: #f56c6c; }
.stat-label { color: #909399; margin-top: 4px; }
.bar-row { display: flex; align-items: center; margin-bottom: 14px; gap: 12px; }
.bar-label { width: 64px; color: #606266; font-size: 14px; }
.bar { flex: 1; }
.bar-num { width: 40px; text-align: right; color: #909399; }
.desc-card { margin-top: 16px; }
</style>
