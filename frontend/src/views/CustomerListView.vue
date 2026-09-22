<template>
  <div>
    <el-card shadow="never">
      <el-tabs v-model="query.intentLevel" class="intent-tabs" @tab-change="onTabChange">
        <el-tab-pane name="">
          <template #label>全部 {{ allCount }}</template>
        </el-tab-pane>
        <el-tab-pane v-for="opt in INTENT_OPTIONS" :key="opt.value" :name="opt.value">
          <template #label>{{ opt.label }} {{ stats[opt.value] ?? 0 }}</template>
        </el-tab-pane>
      </el-tabs>

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索姓名 / 电话 / 公司"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @keyup.enter="load(1)"
          @clear="load(1)"
        />
        <el-button type="primary" @click="load(1)">查询</el-button>
        <div class="spacer" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增客户</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="姓名" min-width="110">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.id)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" min-width="130" />
        <el-table-column prop="company" label="公司" min-width="150" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column label="意向等级" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.intentTone" effect="light">{{ row.intentLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="归属人" width="100" />
        <el-table-column label="下次跟进" width="150">
          <template #default="{ row }">{{ formatTime(row.nextFollowupAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row.id)">跟进</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="auth.isAdmin" link type="warning" size="small" @click="openAssign(row)">分配</el-button>
            <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑客户' : '新增客户'" width="620px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="客户姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公司">
              <el-input v-model="form.company" placeholder="公司名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="form.position" placeholder="职位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源">
              <el-select v-model="form.source" placeholder="客户来源" clearable style="width: 100%">
                <el-option v-for="s in SOURCES" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行业">
              <el-input v-model="form.industry" placeholder="所属行业" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="意向等级" prop="intentLevel">
              <el-select v-model="form.intentLevel" style="width: 100%">
                <el-option v-for="opt in INTENT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次跟进">
              <el-date-picker v-model="form.nextFollowupAt" type="datetime" style="width: 100%"
                value-format="YYYY-MM-DDTHH:mm:ss" placeholder="下次跟进时间" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="auth.isAdmin">
            <el-form-item label="归属人">
              <el-select v-model="form.ownerId" style="width: 100%" clearable placeholder="默认归自己">
                <el-option v-for="u in users" :key="u.id" :label="u.realName || u.username" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="联系地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配对话框（仅主账号） -->
    <el-dialog v-model="assignVisible" title="分配客户" width="420px">
      <p class="assign-tip">将「{{ assignTarget?.name }}」分配给：</p>
      <el-select v-model="assignOwnerId" style="width: 100%" placeholder="选择归属账号">
        <el-option v-for="u in users" :key="u.id" :label="`${u.realName || u.username} (${u.role === 'ADMIN' ? '主账号' : '子账号'})`" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="onAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { customerApi, userApi, type CustomerSavePayload } from '@/api'
import { friendlyError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { INTENT_OPTIONS, type CustomerVO, type UserVO } from '@/types'

const router = useRouter()
const auth = useAuthStore()

const SOURCES = ['转介绍', '广告', '陌拜', '展会', '线上咨询', '老客户复购', '其他']

const list = ref<CustomerVO[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', intentLevel: '', page: 1, size: 10 })
const stats = ref<Record<string, number>>({})
const allCount = computed(() => Object.values(stats.value).reduce((a, b) => a + b, 0))

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()
const users = ref<UserVO[]>([])

const emptyForm = (): CustomerSavePayload => ({
  name: '',
  phone: '',
  company: '',
  position: '',
  source: '',
  industry: '',
  intentLevel: 'PENDING',
  address: '',
  remark: '',
  ownerId: undefined,
  nextFollowupAt: null
})
const form = reactive<CustomerSavePayload>(emptyForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  intentLevel: [{ required: true, message: '请选择意向等级', trigger: 'change' }]
}

const assignVisible = ref(false)
const assignTarget = ref<CustomerVO | null>(null)
const assignOwnerId = ref<number>()
const assigning = ref(false)

function formatTime(v: string | null): string {
  if (!v) return '-'
  return v.replace('T', ' ').slice(0, 16)
}

async function load(page = query.page) {
  query.page = page
  loading.value = true
  try {
    const [data, st] = await Promise.all([customerApi.page(query), customerApi.stats()])
    list.value = data.records
    total.value = data.total
    stats.value = st
  } catch (e) {
    ElMessage.error(friendlyError(e))
  } finally {
    loading.value = false
  }
}

function onTabChange() { load(1) }

function onPageChange(p: number) { load(p) }
function onSizeChange(s: number) { query.size = s; load(1) }

async function loadUsers() {
  if (!auth.isAdmin) return
  try {
    users.value = await userApi.list()
  } catch { /* 忽略 */ }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: CustomerVO) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, phone: row.phone, company: row.company, position: row.position,
    source: row.source, industry: row.industry, intentLevel: row.intentLevel,
    address: row.address, remark: row.remark, ownerId: row.ownerId,
    nextFollowupAt: row.nextFollowupAt
  })
  dialogVisible.value = true
}

async function onSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await customerApi.update(editingId.value, form)
      ElMessage.success('已更新')
    } else {
      await customerApi.create(form)
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(friendlyError(e, '保存失败'))
  } finally {
    saving.value = false
  }
}

function onDelete(row: CustomerVO) {
  ElMessageBox.confirm(`确定删除客户「${row.name}」？该操作不可恢复。`, '删除确认', { type: 'warning' })
    .then(async () => {
      await customerApi.remove(row.id)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

function openAssign(row: CustomerVO) {
  assignTarget.value = row
  assignOwnerId.value = row.ownerId
  assignVisible.value = true
}

async function onAssign() {
  if (!assignTarget.value || assignOwnerId.value == null) {
    ElMessage.warning('请选择归属账号')
    return
  }
  assigning.value = true
  try {
    await customerApi.assign(assignTarget.value.id, assignOwnerId.value)
    ElMessage.success('分配成功')
    assignVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(friendlyError(e, '分配失败'))
  } finally {
    assigning.value = false
  }
}

function goDetail(id: number) {
  router.push(`/customers/${id}`)
}

onMounted(() => {
  load()
  loadUsers()
})
</script>

<style scoped>
.intent-tabs { margin-bottom: 4px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.spacer { flex: 1; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.assign-tip { margin-bottom: 12px; color: #606266; }
</style>
