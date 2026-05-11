<template>
  <div>
    <a-card title="仪表盘">
      <p>欢迎使用 Skada 管理后台</p>
      <a-statistic title="当前管理员" :value="auth.displayId" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/store/auth'
const auth = useAuthStore()
</script>
