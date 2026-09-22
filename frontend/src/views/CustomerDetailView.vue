<template>
  <div v-loading="loading">
    <el-page-header @back="router.push('/customers')" class="back">
      <template #content>
        <span class="ph-title">客户详情</span>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>客户资料</span>
              <el-tag :type="customer?.intentTone" effect="light">{{ customer?.intentLabel }}</el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border v-if="customer">
            <el-descriptions-item label="姓名">{{ customer.name }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ customer.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="公司">{{ customer.company || '-' }}</el-descriptions-item>
            <el-descriptions-item label="职位">{{ customer.position || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ customer.source || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业">{{ customer.industry || '-' }}</el-descriptions-item>
            <el-descriptions-item label="归属人">{{ customer.ownerName }}</el-descriptions-item>
            <el-descriptions-item label="下次跟进">{{ formatTime(customer.nextFollowupAt) }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ customer.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ customer.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 产品资料 -->
        <el-card shadow="never" class="block-card">
          <template #header>
            <div class="card-header">
              <span>产品资料（{{ products.length }}）</span>
              <el-button type="primary" size="small" :icon="Plus" @click="openProduct()">新增产品</el-button>
            </div>
          </template>
          <el-empty v-if="products.length === 0" description="暂无产品资料" :image-size="60" />
          <el-table v-else :data="products" size="small" border>
            <el-table-column prop="productName" label="产品名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="spec" label="规格型号" min-width="100" show-overflow-tooltip />
            <el-table-column prop="quantity" label="数量" width="70" align="center" />
            <el-table-column label="单价" width="100" align="right">
              <template #default="{ row }">{{ money(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openProduct(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeProduct(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="14">
        <!-- 图片 / 附件 -->
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>图片 / 附件（{{ attachments.length }}）</span>
              <el-upload :show-file-list="false" :http-request="doUpload" accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv">
                <el-button type="primary" size="small" :icon="Upload" :loading="uploading">上传文件</el-button>
              </el-upload>
            </div>
          </template>
          <el-empty v-if="attachments.length === 0" description="暂无图片或附件，支持 JPG/PNG/PDF/Office 等" :image-size="60" />
          <div v-else class="attach-grid">
            <div v-for="a in attachments" :key="a.id" class="attach-item">
              <template v-if="a.fileType === 'image'">
                <el-image :src="fileUrl(a.filePath)" fit="cover" class="attach-img" :preview-src-list="[fileUrl(a.filePath)]" preview-teleported />
              </template>
              <template v-else>
                <div class="attach-doc">
                  <el-icon :size="34"><Document /></el-icon>
                </div>
              </template>
              <div class="attach-name" :title="a.fileName">{{ a.fileName }}</div>
              <el-button link type="danger" size="small" class="attach-del" @click="removeAttachment(a)">删除</el-button>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="block-card">
          <template #header>添加跟进</template>
          <el-form :model="followForm" label-position="top">
            <el-form-item label="跟进内容">
              <el-input v-model="followForm.content" type="textarea" :rows="4" placeholder="记录本次沟通内容…" />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="本次结论（可选）">
                  <el-select v-model="followForm.resultLevel" clearable placeholder="选择意向结论" style="width: 100%">
                    <el-option v-for="opt in INTENT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="下次跟进时间（可选）">
                  <el-date-picker v-model="followForm.nextFollowupAt" type="datetime" style="width: 100%"
                    value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-button type="primary" :loading="submitting" @click="submitFollow">提交跟进</el-button>
          </el-form>
        </el-card>

        <el-card shadow="never" class="block-card">
          <template #header>跟进记录（{{ followUps.length }}）</template>
          <el-empty v-if="followUps.length === 0" description="暂无跟进记录" />
          <el-timeline v-else>
            <el-timeline-item v-for="f in followUps" :key="f.id" :timestamp="formatTime(f.createdAt)" placement="top">
              <div class="follow-item">
                <div class="follow-content">{{ f.content }}</div>
                <div class="follow-meta">
                  <el-tag v-if="f.resultLevel" size="small" :type="toneOf(f.resultLevel)" effect="plain">
                    {{ labelOf(f.resultLevel) }}
                  </el-tag>
                  <span v-if="f.nextFollowupAt" class="next">下次跟进：{{ formatTime(f.nextFollowupAt) }}</span>
                  <span class="by">记录人：{{ f.createdBy }}</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 产品新增/编辑对话框 -->
    <el-dialog v-model="productDialog" :title="productForm.id ? '编辑产品' : '新增产品'" width="480px">
      <el-form :model="productForm" label-width="80px">
        <el-form-item label="产品名称" required>
          <el-input v-model="productForm.productName" placeholder="如：云南野生菌火锅套餐" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="productForm.spec" placeholder="规格/型号/容量" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="数量">
              <el-input-number v-model="productForm.quantity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价(元)">
              <el-input-number v-model="productForm.unitPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="productForm.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingProduct" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Document } from '@element-plus/icons-vue'
import type { UploadRequestOptions } from 'element-plus'
import { customerApi, followUpApi, productApi, attachmentApi, type ProductPayload } from '@/api'
import { friendlyError } from '@/api/http'
import { INTENT_OPTIONS, fileUrl, type CustomerVO, type FollowUpRecord, type ProductVO, type AttachmentVO } from '@/types'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const customer = ref<CustomerVO | null>(null)
const followUps = ref<FollowUpRecord[]>([])
const products = ref<ProductVO[]>([])
const attachments = ref<AttachmentVO[]>([])
const followForm = reactive({ content: '', resultLevel: '', nextFollowupAt: '' })

const productDialog = ref(false)
const savingProduct = ref(false)
const productForm = reactive<{ id: number | null } & ProductPayload>({
  id: null, productName: '', spec: '', quantity: 0, unitPrice: 0, remark: ''
})

function formatTime(v: string | null): string {
  if (!v) return '-'
  return v.replace('T', ' ').slice(0, 16)
}

function money(v: number | null): string {
  if (v == null) return '-'
  return '¥' + v.toFixed(2)
}

function toneOf(level: string): string {
  return INTENT_OPTIONS.find(o => o.value === level)?.tone ?? 'info'
}
function labelOf(level: string): string {
  return INTENT_OPTIONS.find(o => o.value === level)?.label ?? level
}

async function load() {
  loading.value = true
  try {
    const [c, f, p, a] = await Promise.all([
      customerApi.detail(id),
      followUpApi.list(id),
      productApi.list(id),
      attachmentApi.list(id)
    ])
    customer.value = c
    followUps.value = f
    products.value = p
    attachments.value = a
  } catch (e) {
    ElMessage.error(friendlyError(e))
    router.push('/customers')
  } finally {
    loading.value = false
  }
}

// ---- 跟进 ----
async function submitFollow() {
  if (!followForm.content.trim()) {
    ElMessage.warning('请填写跟进内容')
    return
  }
  submitting.value = true
  try {
    await followUpApi.create(id, {
      content: followForm.content,
      resultLevel: followForm.resultLevel || undefined,
      nextFollowupAt: followForm.nextFollowupAt || null
    })
    ElMessage.success('跟进已记录')
    followForm.content = ''
    followForm.resultLevel = ''
    followForm.nextFollowupAt = ''
    await load()
  } catch (e) {
    ElMessage.error(friendlyError(e, '提交失败'))
  } finally {
    submitting.value = false
  }
}

// ---- 产品资料 ----
function openProduct(row?: ProductVO) {
  if (row) {
    Object.assign(productForm, {
      id: row.id, productName: row.productName, spec: row.spec || '',
      quantity: row.quantity ?? 0, unitPrice: row.unitPrice ?? 0, remark: row.remark || ''
    })
  } else {
    Object.assign(productForm, { id: null, productName: '', spec: '', quantity: 0, unitPrice: 0, remark: '' })
  }
  productDialog.value = true
}

async function saveProduct() {
  if (!productForm.productName.trim()) {
    ElMessage.warning('请输入产品名称')
    return
  }
  savingProduct.value = true
  try {
    const payload: ProductPayload = {
      productName: productForm.productName.trim(),
      spec: productForm.spec || undefined,
      quantity: productForm.quantity ?? undefined,
      unitPrice: productForm.unitPrice ?? undefined,
      remark: productForm.remark || undefined
    }
    if (productForm.id) {
      await productApi.update(id, productForm.id, payload)
      ElMessage.success('已更新')
    } else {
      await productApi.create(id, payload)
      ElMessage.success('已新增')
    }
    productDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(friendlyError(e, '保存失败'))
  } finally {
    savingProduct.value = false
  }
}

function removeProduct(row: ProductVO) {
  ElMessageBox.confirm(`确定删除产品「${row.productName}」？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await productApi.remove(id, row.id)
      ElMessage.success('已删除')
      await load()
    })
    .catch(() => {})
}

// ---- 附件 ----
async function doUpload(options: UploadRequestOptions) {
  uploading.value = true
  try {
    await attachmentApi.upload(id, options.file as File)
    ElMessage.success('上传成功')
    await load()
  } catch (e) {
    ElMessage.error(friendlyError(e, '上传失败'))
  } finally {
    uploading.value = false
  }
}

function removeAttachment(a: AttachmentVO) {
  ElMessageBox.confirm(`确定删除文件「${a.fileName}」？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await attachmentApi.remove(id, a.id)
      ElMessage.success('已删除')
      await load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.back { margin-bottom: 16px; }
.ph-title { font-weight: 600; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.block-card { margin-top: 16px; }

.attach-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.attach-item { position: relative; border: 1px solid #ebeef5; border-radius: 6px; overflow: hidden; text-align: center; }
.attach-img { width: 100%; height: 100px; display: block; }
.attach-doc { height: 100px; display: flex; align-items: center; justify-content: center; color: #909399; background: #f5f7fa; }
.attach-name { font-size: 12px; color: #606266; padding: 4px 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.attach-del { position: absolute; top: 2px; right: 2px; }

.follow-item { padding-bottom: 4px; }
.follow-content { color: #303133; line-height: 1.6; }
.follow-meta { margin-top: 6px; display: flex; gap: 12px; align-items: center; font-size: 12px; color: #909399; }
.next { color: #e6a23c; }
</style>
