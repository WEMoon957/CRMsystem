<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="title">账号管理</span>
      <div class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增子账号</el-button>
    </div>

    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="username" label="登录名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100">
        <template #default="{ row }">{{ row.realName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" min-width="130">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
            {{ row.role === 'ADMIN' ? '主账号' : '子账号' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="customerCount" label="名下客户" width="100" align="center" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            link :type="row.status === 1 ? 'warning' : 'success'" size="small"
            @click="toggleStatus(row)"
          >{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          <el-button link type="danger" size="small" :disabled="row.id === auth.user?.id" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑账号' : '新增子账号'" width="480px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="登录名" prop="username" v-if="!editingId">
          <el-input v-model="form.username" placeholder="3-32 位字母/数字/下划线" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="子账号（只看自己客户）" value="MEMBER" />
            <el-option label="主账号（全量权限）" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item :label="editingId ? '重置密码' : '初始密码'" :prop="editingId ? 'newPassword' : 'password'">
          <el-input v-model="passwordInput" type="password"
            :placeholder="editingId ? '留空则不修改' : '至少 6 位'" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import { friendlyError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { UserVO } from '@/types'

const auth = useAuthStore()
const users = ref<UserVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '', realName: '', phone: '', role: 'MEMBER'
})
const passwordInput = ref('')

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    users.value = await userApi.list()
  } catch (e) {
    ElMessage.error(friendlyError(e))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', realName: '', phone: '', role: 'MEMBER' })
  passwordInput.value = ''
  dialogVisible.value = true
}

function openEdit(row: UserVO) {
  editingId.value = row.id
  Object.assign(form, { username: row.username, realName: row.realName || '', phone: row.phone || '', role: row.role })
  passwordInput.value = ''
  dialogVisible.value = true
}

async function onSave() {
  if (!editingId.value && !passwordInput.value) {
    ElMessage.warning('请设置初始密码')
    return
  }
  if (passwordInput.value && passwordInput.value.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await userApi.update(editingId.value, {
        realName: form.realName, phone: form.phone,
        newPassword: passwordInput.value || undefined
      })
      ElMessage.success('已更新')
    } else {
      await userApi.create({
        username: form.username, password: passwordInput.value,
        realName: form.realName, phone: form.phone, role: form.role
      })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(friendlyError(e, '保存失败'))
  } finally {
    saving.value = false
  }
}

function toggleStatus(row: UserVO) {
  const next = row.status === 1 ? 0 : 1
  ElMessageBox.confirm(
    `确定${next === 0 ? '禁用' : '启用'}账号「${row.username}」？`,
    '提示', { type: 'warning' }
  ).then(async () => {
    await userApi.update(row.id, { status: next })
    ElMessage.success('已更新')
    load()
  }).catch(() => {})
}

function onDelete(row: UserVO) {
  ElMessageBox.confirm(`确定删除账号「${row.username}」？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await userApi.remove(row.id)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
.title { font-weight: 600; font-size: 16px; }
.spacer { flex: 1; }
</style>
