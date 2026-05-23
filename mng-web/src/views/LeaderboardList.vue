<template>
  <div>
    <a-card>
      <template #title>
        <span>排行榜计划</span>
        <router-link to="/leaderboard/create">
          <a-button type="primary" size="small" style="margin-left: 16px">新建排行榜</a-button>
        </router-link>
      </template>
      <div style="margin-bottom: 16px">
        <a-select
          v-model:value="filterTenantId"
          placeholder="选择租户筛选"
          allowClear
          style="width: 240px"
          @change="fetchLeaderboards"
        >
          <a-select-option v-for="t in tenants" :key="t.tenantId" :value="t.tenantId">
            {{ t.name }} ({{ t.tenantId }})
          </a-select-option>
        </a-select>
      </div>
      <a-alert v-if="error" type="error" message="加载失败" closable @close="fetchLeaderboards" style="margin-bottom: 16px" />
      <a-table
        :dataSource="list"
        :columns="columns"
        :loading="loading"
        rowKey="id"
        :pagination="{ current: page, pageSize: pageSize, total: total, showSizeChanger: true, onChange: onPageChange, onShowSizeChange: onPageChange }"
      >
        <template #emptyText>
          <span v-if="!loading && !error">暂无排行榜数据</span>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'tenantId'">
            <span>{{ tenantMap[record.tenantId]?.name || record.tenantId }} ({{ record.tenantId }})</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'active' ? 'green' : 'red'">
              {{ record.status === 'active' ? '进行中' : '已终止' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'rollStrategy'">
            <span>{{
              record.rollStrategy === 'none' ? '不滚动'
              : record.rollStrategy === 'periodic' ? `每${record.rollIntervalValue}${record.rollIntervalUnit === 'minute' ? '分钟' : record.rollIntervalUnit === 'hour' ? '小时' : '天'}`
              : record.rollStrategy === 'user_count' ? `每${record.rollUserCount}人`
              : record.rollStrategy
            }}</span>
          </template>
          <template v-if="column.key === 'actions'">
            <a-button type="link" @click="handleRoll(record)" :loading="rollingId === record.id" :disabled="record.status !== 'active'">滚动</a-button>
            <a-popconfirm title="确定终止该排行榜？" @confirm="handleStop(record)">
              <a-button type="link" danger :loading="stoppingId === record.id" :disabled="record.status !== 'active'">终止</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listLeaderboards, rollLeaderboard, stopLeaderboard } from '@/api/leaderboard'
import { listAllTenants } from '@/api/tenant'
import type { Leaderboard, Tenant } from '@/types'

const list = ref<Leaderboard[]>([])
const tenants = ref<Tenant[]>([])
const tenantMap = ref<Record<string, Tenant>>({})
const filterTenantId = ref<string | undefined>(undefined)
const loading = ref(false)
const error = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const rollingId = ref<number | null>(null)
const stoppingId = ref<number | null>(null)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '所属租户', key: 'tenantId' },
  { title: '滚动策略', key: 'rollStrategy' },
  { title: '当前实例', dataIndex: 'currentInstanceId', key: 'currentInstanceId' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'actions' },
]

async function fetchLeaderboards() {
  loading.value = true
  error.value = false
  try {
    const res = await listLeaderboards(page.value, pageSize.value, filterTenantId.value)
    list.value = res.data.data.records
    total.value = res.data.data.total
  } catch {
    error.value = true
    list.value = []
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number, ps: number) {
  page.value = p
  pageSize.value = ps
  fetchLeaderboards()
}

async function fetchTenants() {
  const res = await listAllTenants()
  tenants.value = res.data.data.records
  tenantMap.value = Object.fromEntries(res.data.data.records.map(t => [t.tenantId, t]))
}

async function handleRoll(record: Leaderboard) {
  rollingId.value = record.id
  try {
    await rollLeaderboard(record.id)
    await fetchLeaderboards()
  } finally {
    rollingId.value = null
  }
}

async function handleStop(record: Leaderboard) {
  stoppingId.value = record.id
  try {
    await stopLeaderboard(record.id)
    await fetchLeaderboards()
  } finally {
    stoppingId.value = null
  }
}

onMounted(() => {
  fetchTenants()
  fetchLeaderboards()
})
</script>
