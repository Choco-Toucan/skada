<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo">{{ collapsed ? 'S' : 'Skada' }}</div>
      <a-menu theme="dark" mode="inline" v-model:selectedKeys="selectedKeys">
        <a-menu-item key="/dashboard">
          <router-link to="/dashboard">
            <dashboard-outlined />
            <span>仪表盘</span>
          </router-link>
        </a-menu-item>
        <a-menu-item key="/tenants">
          <router-link to="/tenants">
            <team-outlined />
            <span>租户管理</span>
          </router-link>
        </a-menu-item>
        <a-menu-item key="/leaderboards">
          <router-link to="/leaderboards">
            <bar-chart-outlined />
            <span>排行榜</span>
          </router-link>
        </a-menu-item>
        <a-menu-item key="/metrics">
          <router-link to="/metrics">
            <line-chart-outlined />
            <span>指标管理</span>
          </router-link>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <span class="header-info">
          欢迎，{{ auth.displayId }}
          <a-button type="link" @click="handleLogout">退出</a-button>
        </span>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import {
  DashboardOutlined,
  TeamOutlined,
  BarChartOutlined,
  LineChartOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const collapsed = ref(false)
const selectedKeys = ref([route.path])

watch(
  () => route.path,
  (path) => { selectedKeys.value = [path] },
)

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.logo {
  height: 64px;
  line-height: 64px;
  text-align: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
}
.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.header-info {
  color: #666;
}
.content {
  margin: 24px;
  min-height: 280px;
}
</style>
