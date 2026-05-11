<template>
  <div>
    <a-card>
      <template #title>
        <span>租户管理</span>
        <a-button type="primary" size="small" style="margin-left: 16px" @click="showCreate">
          新建租户
        </a-button>
      </template>
      <a-table :dataSource="tenants" :columns="columns" :loading="loading" rowKey="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'actions'">
            <a-button type="link" @click="showEdit(record)">编辑</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="isEdit ? '编辑租户' : '新建租户'" @ok="handleSubmit">
      <a-form :model="form" layout="vertical">
        <a-form-item label="租户名称" name="name" :rules="[{ required: true, message: '请输入名称' }]">
          <a-input v-model:value="form.name" />
        </a-form-item>
        <a-form-item label="允许匿名查询">
          <a-switch v-model:checked="form.allowAnonymousQuery" />
        </a-form-item>
        <template v-if="isEdit">
          <a-form-item label="状态">
            <a-switch v-model:checked="form.status" :checkedValue="1" :unCheckedValue="0" />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { listTenants, createTenant, updateTenant, getTenant } from '@/api/tenant'
import type { Tenant } from '@/types'

const tenants = ref<Tenant[]>([])
const loading = ref(false)
const modalOpen = ref(false)
const isEdit = ref(false)
const editId = ref(0)

const columns = [
  { title: 'ID', dataIndex: 'tenantId', key: 'tenantId' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: 'SecretKey', dataIndex: 'secretKey', key: 'secretKey' },
  { title: '匿名查询', dataIndex: 'allowAnonymousQuery', key: 'allowAnonymousQuery' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'actions' },
]

const form = reactive({
  name: '',
  allowAnonymousQuery: false,
  status: 1,
})

async function fetchTenants() {
  loading.value = true
  try {
    const res = await listTenants()
    tenants.value = res.data.data
  } finally {
    loading.value = false
  }
}

function showCreate() {
  isEdit.value = false
  form.name = ''
  form.allowAnonymousQuery = false
  modalOpen.value = true
}

async function showEdit(record: Tenant) {
  isEdit.value = true
  editId.value = record.id
  form.name = record.name
  form.allowAnonymousQuery = record.allowAnonymousQuery === 1
  form.status = record.status
  modalOpen.value = true
}

async function handleSubmit() {
  try {
    if (isEdit.value) {
      await updateTenant({ id: editId.value, name: form.name, allowAnonymousQuery: form.allowAnonymousQuery ? 1 : 0, status: form.status })
    } else {
      await createTenant({ name: form.name, allowAnonymousQuery: form.allowAnonymousQuery ? 1 : 0 })
    }
    modalOpen.value = false
    await fetchTenants()
  } catch {
    // error handled by interceptor
  }
}

onMounted(fetchTenants)
</script>
