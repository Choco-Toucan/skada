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
            <a-button type="link" size="small" @click="openRanking(record)">查看当前实例</a-button>
            <a-button type="link" size="small" @click="openInstances(record)">历史实例</a-button>
            <a-button type="link" @click="handleRoll(record)" :loading="rollingId === record.id" :disabled="record.status !== 'active'">滚动</a-button>
            <a-popconfirm title="确定终止该排行榜？" @confirm="handleStop(record)">
              <a-button type="link" danger :loading="stoppingId === record.id" :disabled="record.status !== 'active'">终止</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 排行数据弹窗 -->
    <a-modal
      v-model:open="rankingVisible"
      :title="`排行数据 — ${rankingTitle}`"
      width="900px"
      :footer="null"
      @cancel="rankingVisible = false"
    >
      <a-table
        :dataSource="rankingList"
        :columns="rankingColumns"
        :loading="rankingLoading"
        :pagination="{ current: rankingPage, pageSize: rankingPageSize, total: rankingTotal, onChange: onRankingPageChange }"
        rowKey="rank"
        size="small"
      >
        <template #emptyText>
          <span v-if="!rankingLoading">暂无排行数据</span>
        </template>
      </a-table>
    </a-modal>

    <!-- 历史实例弹窗 -->
    <a-modal
      v-model:open="instancesVisible"
      :title="`历史实例 — ${instancesTitle}`"
      width="900px"
      :footer="null"
      @cancel="instancesVisible = false"
    >
      <a-table
        :dataSource="instances"
        :columns="instanceColumns"
        :loading="instancesLoading"
        rowKey="id"
        size="small"
      >
        <template #emptyText>
          <span v-if="!instancesLoading">暂无历史实例</span>
        </template>
        <template #bodyCell="{ column: col, record: inst }">
          <template v-if="col.key === 'instanceStatus'">
            <a-tag :color="inst.status === 'active' ? 'green' : 'default'">
              {{ inst.status === 'active' ? '活跃' : '已关闭' }}
            </a-tag>
          </template>
          <template v-if="col.key === 'instanceActions'">
            <a-button type="link" size="small" @click="openInstanceRanking(inst)">查看排行</a-button>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 历史实例中的排行数据弹窗 -->
    <a-modal
      v-model:open="instanceRankingVisible"
      :title="`排行数据 — 实例 ${instanceRankingTitle}`"
      width="900px"
      :footer="null"
      @cancel="instanceRankingVisible = false"
    >
      <a-table
        :dataSource="instanceRankingList"
        :columns="rankingColumns"
        :loading="instanceRankingLoading"
        :pagination="{ current: instanceRankingPage, pageSize: instanceRankingPageSize, total: instanceRankingTotal, onChange: onInstanceRankingPageChange }"
        rowKey="rank"
        size="small"
      >
        <template #emptyText>
          <span v-if="!instanceRankingLoading">暂无排行数据</span>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listLeaderboards, rollLeaderboard, stopLeaderboard, getInstances, getRanking } from '@/api/leaderboard'
import { listAllTenants } from '@/api/tenant'
import type { Leaderboard, Tenant, LeaderboardInstance, LeaderboardRankEntry } from '@/types'

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
  { title: '当前实例', dataIndex: 'currentInstanceBusinessId', key: 'currentInstanceId' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'actions', width: 300 },
]

// ==================== 排行数据弹窗 ====================
const rankingVisible = ref(false)
const rankingTitle = ref('')
const rankingList = ref<LeaderboardRankEntry[]>([])
const rankingLoading = ref(false)
const rankingPage = ref(1)
const rankingPageSize = ref(30)
const rankingTotal = ref(0)
let _rankingLbId = 0
let _rankingInstId = 0

const rankingColumns = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 60 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '指标值', key: 'metrics', width: 400 },
]

async function openRanking(record: Leaderboard) {
  if (!record.currentInstanceId) {
    return
  }
  _rankingLbId = record.id
  _rankingInstId = record.currentInstanceId
  rankingTitle.value = record.name
  rankingPage.value = 1
  rankingVisible.value = true
  await fetchRanking()
}

async function fetchRanking() {
  rankingLoading.value = true
  try {
    const from = (rankingPage.value - 1) * rankingPageSize.value
    const to = from + rankingPageSize.value - 1
    const res = await getRanking(_rankingLbId, _rankingInstId, from, to)
    rankingList.value = res.data.data
  } catch {
    rankingList.value = []
  } finally {
    rankingLoading.value = false
  }
}

function onRankingPageChange(p: number) {
  rankingPage.value = p
  fetchRanking()
}

// ==================== 历史实例弹窗 ====================
const instancesVisible = ref(false)
const instancesTitle = ref('')
const instances = ref<LeaderboardInstance[]>([])
const instancesLoading = ref(false)

const instanceColumns = [
  { title: '序号', dataIndex: 'instanceSeq', key: 'instanceSeq', width: 60 },
  { title: '实例ID', dataIndex: 'instanceId', key: 'instanceId' },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime' },
  { title: '状态', key: 'instanceStatus', width: 80 },
  { title: '操作', key: 'instanceActions', width: 100 },
]

async function openInstances(record: Leaderboard) {
  instancesTitle.value = record.name
  instancesLoading.value = true
  instancesVisible.value = true
  try {
    const res = await getInstances(record.id)
    instances.value = res.data.data.sort((a, b) => b.instanceSeq - a.instanceSeq)
  } catch {
    instances.value = []
  } finally {
    instancesLoading.value = false
  }
}

// ==================== 历史实例排行 ====================
const instanceRankingVisible = ref(false)
const instanceRankingTitle = ref('')
const instanceRankingList = ref<LeaderboardRankEntry[]>([])
const instanceRankingLoading = ref(false)
const instanceRankingPage = ref(1)
const instanceRankingPageSize = ref(30)
const instanceRankingTotal = ref(0)
let _instLbId = 0
let _instInstId = 0

async function openInstanceRanking(inst: LeaderboardInstance) {
  _instLbId = inst.leaderboardId
  _instInstId = inst.id
  instanceRankingTitle.value = String(inst.instanceSeq)
  instanceRankingPage.value = 1
  instanceRankingVisible.value = true
  await fetchInstanceRanking()
}

async function fetchInstanceRanking() {
  instanceRankingLoading.value = true
  try {
    const from = (instanceRankingPage.value - 1) * instanceRankingPageSize.value
    const to = from + instanceRankingPageSize.value - 1
    const res = await getRanking(_instLbId, _instInstId, from, to)
    instanceRankingList.value = res.data.data
  } catch {
    instanceRankingList.value = []
  } finally {
    instanceRankingLoading.value = false
  }
}

function onInstanceRankingPageChange(p: number) {
  instanceRankingPage.value = p
  fetchInstanceRanking()
}

// ==================== 排行榜列表 ====================
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
