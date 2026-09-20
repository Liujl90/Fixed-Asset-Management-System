<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderTree, Pencil, Plus, Search, Trash2 } from 'lucide-vue-next'
import {
  addCategory,
  deleteCategory,
  demoState,
  updateCategory,
} from '@/stores/demoStore'

const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  name: '',
  code: '',
  parentId: null,
  status: 'active',
})

const rows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (key) {
    return demoState.categories
      .filter((item) => `${item.name}${item.code}`.toLowerCase().includes(key))
      .map((item) => ({
        ...item,
        assetCount: demoState.assets.filter((asset) => asset.categoryId === item.id).length,
      }))
  }

  function buildChildren(parentId) {
    return demoState.categories
      .filter((item) => item.parentId === parentId)
      .map((item) => ({
        ...item,
        assetCount: demoState.assets.filter((asset) => asset.categoryId === item.id).length,
        children: buildChildren(item.id),
      }))
  }

  return buildChildren(null)
})

function openCreate(parent = null) {
  editingId.value = null
  Object.assign(form, {
    name: '',
    code: '',
    parentId: parent?.id || null,
    status: 'active',
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    parentId: row.parentId,
    status: row.status,
  })
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
    if (editingId.value) {
      updateCategory(editingId.value, { ...form })
      ElMessage.success('分类已更新')
    } else {
      addCategory({ ...form })
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除分类“${row.name}”吗？`, '删除分类', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    deleteCategory(row.id)
    ElMessage.success('分类已删除')
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">CLASSIFICATION</span>
        <h2>资产分类</h2>
        <p>使用分层分类整理资产，为登记、查询和统计提供统一口径。</p>
      </div>
      <el-button type="primary" @click="openCreate()">
        <Plus :size="17" />
        新增一级分类
      </el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索分类名称或编码" class="search-input">
          <template #prefix>
            <Search :size="16" />
          </template>
        </el-input>
        <div class="toolbar-summary">
          共 <strong>{{ demoState.categories.length }}</strong> 个分类
        </div>
      </div>

      <el-table
        :data="rows"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
        stripe
      >
        <el-table-column label="分类名称" min-width="240">
          <template #default="{ row }">
            <div class="table-primary">
              <div class="table-avatar blue">
                <FolderTree :size="17" />
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.code }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="层级" width="100">
          <template #default="{ row }">
            <span class="count-pill">{{ row.parentId ? '子分类' : '一级分类' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="assetCount" label="关联资产" width="110" align="center" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" round>
              {{ row.status === 'active' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.parentId" link type="primary" @click="openCreate(row)">
              <Plus :size="15" />
              新增子分类
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">
              <Pencil :size="15" />
              编辑
            </el-button>
            <el-button link type="danger" @click="remove(row)">
              <Trash2 :size="15" />
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="分类名称" prop="name" :rules="[{ required: true, message: '请输入分类名称' }]">
          <el-input v-model="form.name" placeholder="例如：计算机设备" />
        </el-form-item>
        <el-form-item label="分类编码" prop="code" :rules="[{ required: true, message: '请输入分类编码' }]">
          <el-input v-model="form.code" placeholder="例如：OFFICE-PC" />
        </el-form-item>
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" clearable placeholder="不选择则为一级分类">
            <el-option
              v-for="category in demoState.categories.filter((item) => !item.parentId)"
              :key="category.id"
              :label="category.name"
              :value="category.id"
              :disabled="category.id === editingId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
