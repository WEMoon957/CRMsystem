<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">SaaS 客户管理系统</h2>
      <p class="subtitle">主账号 / 子账号 登录</p>
      <el-form :model="form" @keyup.enter="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
      <el-alert v-if="hint" :title="hint" type="info" :closable="false" class="hint" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { friendlyError } from '@/api/http'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const hint = ref('初始主账号 admin / Admin@123，演示子账号 sales01 / Sales@123')

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    router.push((route.query.redirect as string) || '/')
  } catch (e) {
    ElMessage.error(friendlyError(e, '登录失败'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 24px 8px 8px;
  border-radius: 12px;
}
.title { text-align: center; margin-bottom: 4px; }
.subtitle { text-align: center; color: #909399; font-size: 13px; margin-bottom: 24px; }
.login-btn { width: 100%; margin-top: 4px; }
.hint { margin-top: 16px; }
</style>
