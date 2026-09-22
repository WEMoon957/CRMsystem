<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">CRM 客户管理</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon><span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/customers">
          <el-icon><User /></el-icon><span>客户管理</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/users">
          <el-icon><Setting /></el-icon><span>账号管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="crumb">{{ pageTitle }}</div>
        <el-dropdown @command="onCommand">
          <span class="user-chip">
            <el-tag :type="auth.isAdmin ? 'danger' : 'primary'" size="small">
              {{ auth.isAdmin ? '主账号' : '子账号' }}
            </el-tag>
            {{ auth.user?.realName || auth.user?.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis, User, Setting, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/customers')) return '/customers'
  if (route.path === '/users') return '/users'
  return '/'
})

const pageTitle = computed(() => {
  if (route.name === 'customer-detail') return '客户详情'
  if (route.name === 'customers') return '客户管理'
  if (route.name === 'users') return '账号管理'
  return '数据看板'
})

async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    await auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #fff; border-right: 1px solid #e4e7ed; }
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  color: #409eff;
  border-bottom: 1px solid #e4e7ed;
}
.menu { border-right: none; }
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
}
.crumb { font-weight: 600; }
.user-chip { display: inline-flex; align-items: center; gap: 8px; cursor: pointer; }
.main { background: #f5f7fa; padding: 20px; overflow-y: auto; }
</style>
