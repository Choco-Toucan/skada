<template>
  <div>
    <a-card>
      <template #title>
        <span>指标管理</span>
      </template>
      <div style="margin-bottom: 16px">
        <span>先选择租户查看和管理指标</span>
      </div>

      <a-form layout="inline" :model="newForm" @finish="handleCreate" style="margin-bottom: 16px">
        <a-form-item label="租户" name="tenantId">
          <a-select v-model:value="newForm.tenantId" placeholder="选择租户" style="width: 200px" @change="fetchMetrics">
            <a-select-option v-for="t in tenants" :key="t.tenantId" :value="t.tenantId">
              {{ t.name }} ({{ t.tenantId }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="name">
          <a-input v-model:value="newForm.name" placeholder="指标名称" />
        </a-form-item>
        <a-form-item name="description">
          <a-input v-model:value="newForm.description" placeholder="描述(可选)" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :disabled="!newForm.tenantId">新增指标</a-button>
        </a-form-item>
      </a-form>

      <a-alert v-if="error" type="error" message="加载失败" closable @close="fetchMetrics(selectedTenantId)" style="margin-bottom: 16px" />
      <a-table
        :dataSource="metrics"
        :columns="columns"
        :loading="loading"
        rowKey="id"
      >
        <template #emptyText>
          <span v-if="!loading && !error">暂无指标数据</span>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-popconfirm title="确定删除该指标？" @confirm="handleDelete(record.id)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listTenants } from '@/api/tenant'
import { listMetrics, createMetric, deleteMetric } from '@/api/metric'
import type { Tenant, Metric } from '@/types'

const tenants = ref<Tenant[]>([])
const metrics = ref<Metric[]>([])
const loading = ref(false)
const error = ref(false)
const selectedTenantId = ref('')

const newForm = reactive({
  tenantId: '',
  name: '',
  description: '',
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '标识', dataIndex: 'metricId', key: 'metricId' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'actions' },
]

async function fetchTenants() {
  const res = await listTenants()
  tenants.value = res.data.data.records
}

async function fetchMetrics(tenantId: string) {
  if (!tenantId) {
    metrics.value = []
    error.value = false
    return
  }
  loading.value = true
  error.value = false
  try {
    selectedTenantId.value = tenantId
    const res = await listMetrics(tenantId)
    metrics.value = res.data.data
  } catch {
    error.value = true
    metrics.value = []
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newForm.tenantId || !newForm.name) {
    message.warning('请填写租户和名称')
    return
  }
  await createMetric({
    tenantId: newForm.tenantId,
    name: newForm.name,
    description: newForm.description || undefined,
  })
  message.success('创建成功')
  newForm.name = ''
  newForm.description = ''
  fetchMetrics(selectedTenantId.value)
}

async function handleDelete(id: number) {
  await deleteMetric(id)
  message.success('删除成功')
  fetchMetrics(selectedTenantId.value)
}

onMounted(fetchTenants)
</script>
