<template>
  <a-card title="新建排行榜计划">
    <a-form :model="form" layout="vertical" style="max-width: 600px" @finish="handleCreate">
      <a-form-item label="所属租户" name="tenantId" :rules="[{ required: true, message: '请选择租户' }]">
        <a-select v-model:value="form.tenantId" placeholder="选择租户" @change="onTenantChange">
          <a-select-option v-for="t in tenants" :key="t.tenantId" :value="t.tenantId">
            {{ t.name }} ({{ t.tenantId }})
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="排行榜名称" name="name" :rules="[{ required: true, message: '请输入名称' }]">
        <a-input v-model:value="form.name" />
      </a-form-item>
      <a-form-item label="开始时间" name="startTime" :rules="[{ required: true, message: '请选择时间' }]">
        <a-date-picker show-time v-model:value="form.startTime" value-format="x" style="width: 100%" />
      </a-form-item>
      <a-form-item label="结束时间（可选）" name="endTime">
        <a-date-picker show-time v-model:value="form.endTime" value-format="x" style="width: 100%" />
      </a-form-item>

      <a-divider>关联指标</a-divider>
      <div v-for="(item, idx) in form.metrics" :key="idx" style="margin-bottom: 12px">
        <a-row :gutter="8">
          <a-col :span="10">
            <a-select v-model:value="item.metricId" placeholder="选择指标">
              <a-select-option v-for="m in availableMetrics" :key="m.id" :value="m.id">
                {{ m.name }} ({{ m.code }})
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
            <a-input-number v-model:value="item.priority" :min="1" placeholder="优先级" style="width:100%" />
          </a-col>
          <a-col :span="6">
            <a-select v-model:value="item.sortOrder">
              <a-select-option value="desc">降序</a-select-option>
              <a-select-option value="asc">升序</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
            <a-button danger @click="removeMetric(idx)" :disabled="form.metrics.length <= 1">删除</a-button>
          </a-col>
        </a-row>
      </div>
      <a-button type="dashed" @click="addMetric" style="margin-bottom: 16px">+ 添加指标</a-button>

      <a-divider>其他配置</a-divider>
      <a-form-item label="滚动策略" name="rollStrategy">
        <a-select v-model:value="form.rollStrategy">
          <a-select-option value="none">不滚动</a-select-option>
          <a-select-option value="periodic">周期性滚动</a-select-option>
          <a-select-option value="user_count">按用户数滚动</a-select-option>
        </a-select>
      </a-form-item>
      <template v-if="form.rollStrategy === 'periodic'">
        <a-form-item label="滚动间隔">
          <a-input-number v-model:value="form.rollIntervalValue" :min="1" style="width: 120px" />
          <a-select v-model:value="form.rollIntervalUnit" style="width: 120px; margin-left: 8px">
            <a-select-option value="minute">分钟</a-select-option>
            <a-select-option value="hour">小时</a-select-option>
            <a-select-option value="day">天</a-select-option>
          </a-select>
        </a-form-item>
      </template>
      <template v-if="form.rollStrategy === 'user_count'">
        <a-form-item label="触发用户数">
          <a-input-number v-model:value="form.rollUserCount" :min="1" />
        </a-form-item>
      </template>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">创建</a-button>
        <router-link to="/leaderboards">
          <a-button style="margin-left: 8px">取消</a-button>
        </router-link>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listTenants } from '@/api/tenant'
import { listMetrics } from '@/api/metric'
import { createLeaderboard } from '@/api/leaderboard'
import type { Tenant, Metric } from '@/types'

const router = useRouter()
const tenants = ref<Tenant[]>([])
const availableMetrics = ref<Metric[]>([])
const loading = ref(false)

const form = reactive({
  tenantId: '',
  name: '',
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  rollStrategy: 'none',
  rollIntervalValue: undefined as number | undefined,
  rollIntervalUnit: 'hour' as string | undefined,
  rollUserCount: undefined as number | undefined,
  metrics: [
    { metricId: undefined as number | undefined, priority: 1, sortOrder: 'desc' },
  ] as { metricId: number | undefined; priority: number; sortOrder: string }[],
})

async function fetchTenants() {
  const res = await listTenants()
  tenants.value = res.data.data.records
}

async function onTenantChange(tenantId: string) {
  if (!tenantId) return
  const res = await listMetrics(tenantId)
  availableMetrics.value = res.data.data
}

function addMetric() {
  form.metrics.push({ metricId: undefined, priority: form.metrics.length + 1, sortOrder: 'desc' })
}

function removeMetric(idx: number) {
  form.metrics.splice(idx, 1)
}

async function handleCreate() {
  loading.value = true
  try {
    await createLeaderboard({
      tenantId: form.tenantId,
      name: form.name,
      startTime: Number(form.startTime),
      endTime: form.endTime ? Number(form.endTime) : undefined,
      rollStrategy: form.rollStrategy,
      rollIntervalValue: form.rollIntervalValue,
      rollIntervalUnit: form.rollIntervalUnit,
      rollUserCount: form.rollUserCount,
      metrics: form.metrics.map((m, i) => ({
        metricId: m.metricId!,
        priority: m.priority || i + 1,
        sortOrder: m.sortOrder,
      })),
    })
    router.push('/leaderboards')
  } finally {
    loading.value = false
  }
}

onMounted(fetchTenants)
</script>
