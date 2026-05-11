<template>
  <a-card title="新建排行榜">
    <a-form :model="form" layout="vertical" style="max-width: 600px" @finish="handleCreate">
      <a-form-item label="所属租户" name="tenantId" :rules="[{ required: true, message: '请选择租户' }]">
        <a-select v-model:value="form.tenantId" placeholder="选择租户">
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
      <a-form-item label="排序规则" name="sortOrder">
        <a-radio-group v-model:value="form.sortOrder">
          <a-radio value="desc">降序（高分在前）</a-radio>
          <a-radio value="asc">升序（低分在前）</a-radio>
        </a-radio-group>
      </a-form-item>
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
import { createLeaderboard } from '@/api/leaderboard'
import type { Tenant } from '@/types'

const router = useRouter()
const tenants = ref<Tenant[]>([])
const loading = ref(false)

const form = reactive({
  tenantId: '',
  name: '',
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
  sortOrder: 'desc',
  rollStrategy: 'none',
  rollIntervalValue: undefined as number | undefined,
  rollIntervalUnit: 'hour' as string | undefined,
  rollUserCount: undefined as number | undefined,
})

async function fetchTenants() {
  const res = await listTenants()
  tenants.value = res.data.data
}

async function handleCreate() {
  loading.value = true
  try {
    await createLeaderboard({
      tenantId: form.tenantId,
      name: form.name,
      startTime: Number(form.startTime),
      endTime: form.endTime ? Number(form.endTime) : undefined,
      sortOrder: form.sortOrder,
      rollStrategy: form.rollStrategy,
      rollIntervalValue: form.rollIntervalValue,
      rollIntervalUnit: form.rollIntervalUnit,
      rollUserCount: form.rollUserCount,
    })
    router.push('/leaderboards')
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(fetchTenants)
</script>
