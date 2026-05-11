<template>
  <a-config-provider :locale="locale">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
const locale = zhCN
</script>

<style>
body {
  margin: 0;
  padding: 0;
  background: #f5f5f5;
}
</style>
