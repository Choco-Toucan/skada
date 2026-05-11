<template>
  <div>
    <a-card>
      <template #title>
        <span>排行榜</span>
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
            {{ t.name }}
          </a-select-option>
        </a-select>
      </div>
      <a-table :dataSource="list" :columns="columns" :loading="loading" rowKey="id">
        <template #bodyCell="{ column, record }">
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
            <a-button type="link" @click="handleRoll(record)" :disabled="record.status !== 'active'">滚动</a-button>
            <a-popconfirm title="确定终止该排行榜？" @confirm="handleStop(record)">
              <a-button type="link" danger :disabled="record.status !== 'active'">终止</a-button>
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
import { listTenants } from '@/api/tenant'
import type { Leaderboard, Tenant } from '@/types'

const list = ref<Leaderboard[]>([])
const tenants = ref<Tenant[]>([])
const filterTenantId = ref<string | undefined>(undefined)
const loading = ref(false)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '所属租户', dataIndex: 'tenantId', key: 'tenantId' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
  { title: '滚动策略', key: 'rollStrategy' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'actions' },
]

async function fetchLeaderboards() {
  loading.value = true
  try {
    const res = await listLeaderboards(filterTenantId.value)
    list.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function fetchTenants() {
  const res = await listTenants()
  tenants.value = res.data.data
}

async function handleRoll(record: Leaderboard) {
  await rollLeaderboard(record.id)
  await fetchLeaderboards()
}

async function handleStop(record: Leaderboard) {
  await stopLeaderboard(record.id)
  await fetchLeaderboards()
}

onMounted(() => {
  fetchTenants()
  fetchLeaderboards()
})
</script>
