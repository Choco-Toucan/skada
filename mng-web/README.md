# skada-mng-web

管理台前端 — Vue3 + Ant Design Vue + TypeScript。端口 **3000**。

## 技术栈

- Vue 3 (Composition API + `<script setup>`)
- Ant Design Vue 4.x
- Vue Router 4
- Pinia 状态管理
- Axios HTTP 客户端
- Vite 构建工具

## 功能页面

| 路径 | 页面 | 说明 |
|---|---|---|
| `/login` | 登录页 | 管理员登录 |
| `/dashboard` | 仪表盘 | 概览信息 |
| `/tenants` | 租户管理 | 租户列表、创建、编辑 |
| `/metrics` | 指标管理 | 按租户创建/查看/删除指标 |
| `/leaderboards` | 排行榜计划列表 | 查看所有计划，滚动/终止操作 |
| `/leaderboard/create` | 新建排行榜 | 选择租户→关联指标→配置策略 |

## 本地开发

```bash
npm install
npm run dev
# 端口 3000，API 代理到 localhost:8811
```

## 构建

```bash
npm run build
# 输出到 dist/ 目录
```
