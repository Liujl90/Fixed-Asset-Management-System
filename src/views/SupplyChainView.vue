<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Boxes,
  CheckCircle2,
  PackagePlus,
  Plus,
  Send,
  ShoppingCart,
  Truck,
} from 'lucide-vue-next'
import {
  approvePurchase,
  cancelInbound,
  confirmInbound,
  createInbound,
  createPurchase,
  createSupplier,
  demoState,
  employeeName,
  submitPurchase,
  updateSupplier,
} from '@/stores/backendStore'
import { formatCurrency, formatDate } from '@/utils/format'

const activeTab = ref('suppliers')
const supplierDialog = ref(false)
const purchaseDialog = ref(false)
const inboundDialog = ref(false)
const editingSupplierId = ref(null)

const supplierForm = reactive({
  name: '',
  code: '',
  contactName: '',
  phone: '',
  email: '',
  address: '',
  status: 'active',
})

const purchaseForm = reactive({
  orderNo: '',
  supplierId: null,
  applicantId: null,
  orderDate: new Date().toISOString().slice(0, 10),
  expectedDate: '',
  remark: '',
  items: [],
})

const inboundForm = reactive({
  inboundNo: '',
  purchaseOrderId: null,
  supplierId: null,
  warehouseName: '总部资产库',
  inboundDate: new Date().toISOString().slice(0, 10),
  operatorId: null,
  remark: '',
  items: [],
})

const purchaseRows = computed(() =>
  demoState.purchaseOrders.map((item) => ({
    ...item,
    supplierName: demoState.suppliers.find((supplier) => supplier.id === item.supplierId)?.name || '-',
    applicantName: employeeName(item.applicantId),
  })),
)

const inboundRows = computed(() =>
  demoState.inboundOrders.map((item) => ({
    ...item,
    supplierName: demoState.suppliers.find((supplier) => supplier.id === item.supplierId)?.name || '-',
    operatorName: employeeName(item.operatorId),
  })),
)

function newPurchaseItem() {
  return { assetName: '', categoryId: null, quantity: 1, unitPrice: 0, remark: '' }
}

function newInboundItem() {
  return {
    assetName: '',
    categoryId: null,
    brandModel: '',
    quantity: 1,
    unitPrice: 0,
    departmentId: null,
    remark: '',
  }
}

function openSupplier(row) {
  editingSupplierId.value = row?.id || null
  Object.assign(supplierForm, row || {
    name: '',
    code: '',
    contactName: '',
    phone: '',
    email: '',
    address: '',
    status: 'active',
  })
  supplierDialog.value = true
}

async function saveSupplier() {
  if (!supplierForm.name || !supplierForm.code) {
    ElMessage.warning('请填写供应商名称和编码')
    return
  }
  try {
    if (editingSupplierId.value) {
      await updateSupplier(editingSupplierId.value, { ...supplierForm })
    } else {
      await createSupplier({ ...supplierForm })
    }
    supplierDialog.value = false
    ElMessage.success('供应商已保存')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openPurchase() {
  Object.assign(purchaseForm, {
    orderNo: `PO-${new Date().getFullYear()}-${String(demoState.purchaseOrders.length + 1).padStart(3, '0')}`,
    supplierId: demoState.suppliers[0]?.id || null,
    applicantId: demoState.employees[0]?.id || null,
    orderDate: new Date().toISOString().slice(0, 10),
    expectedDate: '',
    remark: '',
    items: [newPurchaseItem()],
  })
  purchaseDialog.value = true
}

async function savePurchase() {
  if (!purchaseForm.supplierId || !purchaseForm.applicantId || purchaseForm.items.some((item) => !item.assetName || !item.categoryId)) {
    ElMessage.warning('请完善采购单和明细')
    return
  }
  try {
    await createPurchase({ ...purchaseForm, items: purchaseForm.items.map((item) => ({ ...item })) })
    purchaseDialog.value = false
    ElMessage.success('采购单已创建')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openInbound() {
  Object.assign(inboundForm, {
    inboundNo: `IN-${new Date().getFullYear()}-${String(demoState.inboundOrders.length + 1).padStart(3, '0')}`,
    purchaseOrderId: null,
    supplierId: demoState.suppliers[0]?.id || null,
    warehouseName: '总部资产库',
    inboundDate: new Date().toISOString().slice(0, 10),
    operatorId: demoState.employees[0]?.id || null,
    remark: '',
    items: [newInboundItem()],
  })
  inboundDialog.value = true
}

async function saveInbound() {
  if (!inboundForm.supplierId || !inboundForm.operatorId || inboundForm.items.some((item) => !item.assetName || !item.categoryId || !item.departmentId)) {
    ElMessage.warning('请完善入库单和明细')
    return
  }
  try {
    await createInbound({ ...inboundForm, items: inboundForm.items.map((item) => ({ ...item })) })
    inboundDialog.value = false
    ElMessage.success('入库单已创建')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleSubmitPurchase(row) {
  try {
    await submitPurchase(row.id)
    ElMessage.success('采购单已提交')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleApprovePurchase(row) {
  try {
    await approvePurchase(row.id)
    ElMessage.success('采购单已审核通过')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleConfirmInbound(row) {
  try {
    const result = await confirmInbound(row.id)
    ElMessage.success(`入库完成，生成 ${result.createdAssets || 0} 项资产`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleCancelInbound(row) {
  try {
    await cancelInbound(row.id)
    ElMessage.success('入库单已取消')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function statusType(status) {
  return {
    approved: 'success',
    completed: 'success',
    confirmed: 'success',
    pending: 'warning',
    draft: 'info',
    rejected: 'danger',
    cancelled: 'info',
  }[status] || 'info'
}

function statusLabel(status) {
  return {
    draft: '草稿',
    pending: '待审核',
    approved: '已审核',
    rejected: '已驳回',
    completed: '已完成',
    confirmed: '已入库',
    cancelled: '已取消',
  }[status] || status
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">PURCHASE & INBOUND</span>
        <h2>采购与入库</h2>
        <p>维护供应商，管理采购审核流程，并在确认入库后批量生成固定资产。</p>
      </div>
    </section>

    <section class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="`供应商 ${demoState.suppliers.length}`" name="suppliers">
          <div class="tab-toolbar">
            <el-button type="primary" @click="openSupplier()"><Plus :size="16" />新增供应商</el-button>
          </div>
          <el-table :data="demoState.suppliers" stripe>
            <el-table-column label="供应商" min-width="200">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="table-avatar teal"><Truck :size="17" /></div>
                  <div><strong>{{ row.name }}</strong><span>{{ row.code }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="contactName" label="联系人" width="120" />
            <el-table-column prop="phone" label="电话" width="140" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'" round>
                  {{ row.status === 'active' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openSupplier(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`采购单 ${purchaseRows.length}`" name="purchases">
          <div class="tab-toolbar">
            <el-button type="primary" @click="openPurchase"><ShoppingCart :size="16" />创建采购单</el-button>
          </div>
          <el-table :data="purchaseRows" stripe>
            <el-table-column prop="orderNo" label="采购单号" width="150" />
            <el-table-column prop="supplierName" label="供应商" min-width="150" />
            <el-table-column prop="applicantName" label="申请人" width="110" />
            <el-table-column label="订单日期" width="120">
              <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="130" align="right">
              <template #default="{ row }">{{ formatCurrency(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'draft'" link type="primary" @click="handleSubmitPurchase(row)">
                  <Send :size="14" />提交
                </el-button>
                <el-button v-if="row.status === 'pending'" link type="success" @click="handleApprovePurchase(row)">
                  <CheckCircle2 :size="14" />审核
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`入库单 ${inboundRows.length}`" name="inbounds">
          <div class="tab-toolbar">
            <el-button type="primary" @click="openInbound"><PackagePlus :size="16" />创建入库单</el-button>
          </div>
          <el-table :data="inboundRows" stripe>
            <el-table-column prop="inboundNo" label="入库单号" width="150" />
            <el-table-column prop="supplierName" label="供应商" min-width="150" />
            <el-table-column prop="warehouseName" label="仓库" min-width="130" />
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column label="入库日期" width="120">
              <template #default="{ row }">{{ formatDate(row.inboundDate) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'draft'" link type="success" @click="handleConfirmInbound(row)">
                  <CheckCircle2 :size="14" />确认入库
                </el-button>
                <el-button v-if="row.status === 'draft'" link type="danger" @click="handleCancelInbound(row)">
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="supplierDialog" :title="editingSupplierId ? '编辑供应商' : '新增供应商'" width="600px">
      <el-form :model="supplierForm" label-position="top" class="form-grid">
        <el-form-item label="供应商名称"><el-input v-model="supplierForm.name" /></el-form-item>
        <el-form-item label="供应商编码"><el-input v-model="supplierForm.code" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="supplierForm.contactName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="supplierForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="supplierForm.email" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="supplierForm.status">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址" class="span-two"><el-input v-model="supplierForm.address" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplierDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSupplier">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="purchaseDialog" title="创建采购单" width="820px">
      <el-form :model="purchaseForm" label-position="top">
        <div class="form-grid">
          <el-form-item label="采购单号"><el-input v-model="purchaseForm.orderNo" /></el-form-item>
          <el-form-item label="供应商">
            <el-select v-model="purchaseForm.supplierId">
              <el-option v-for="item in demoState.suppliers" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="申请人">
            <el-select v-model="purchaseForm.applicantId">
              <el-option v-for="item in demoState.employees" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="订单日期">
            <el-date-picker v-model="purchaseForm.orderDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="预计到货日期">
            <el-date-picker v-model="purchaseForm.expectedDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </div>
        <el-divider>采购明细</el-divider>
        <div v-for="(item, index) in purchaseForm.items" :key="index" class="order-item-row">
          <el-input v-model="item.assetName" placeholder="资产名称" />
          <el-select v-model="item.categoryId" placeholder="分类">
            <el-option v-for="category in demoState.categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" />
          <el-input-number v-model="item.unitPrice" :min="0" />
          <el-button text type="danger" @click="purchaseForm.items.splice(index, 1)">删除</el-button>
        </div>
        <el-button plain @click="purchaseForm.items.push(newPurchaseItem())"><Plus :size="15" />添加明细</el-button>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialog = false">取消</el-button>
        <el-button type="primary" @click="savePurchase">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inboundDialog" title="创建入库单" width="860px">
      <el-form :model="inboundForm" label-position="top">
        <div class="form-grid">
          <el-form-item label="入库单号"><el-input v-model="inboundForm.inboundNo" /></el-form-item>
          <el-form-item label="供应商">
            <el-select v-model="inboundForm.supplierId">
              <el-option v-for="item in demoState.suppliers" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联采购单">
            <el-select v-model="inboundForm.purchaseOrderId" clearable>
              <el-option
                v-for="item in demoState.purchaseOrders.filter((order) => order.status === 'approved')"
                :key="item.id"
                :label="item.orderNo"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="操作人">
            <el-select v-model="inboundForm.operatorId">
              <el-option v-for="item in demoState.employees" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="仓库"><el-input v-model="inboundForm.warehouseName" /></el-form-item>
          <el-form-item label="入库日期">
            <el-date-picker v-model="inboundForm.inboundDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </div>
        <el-divider>入库明细</el-divider>
        <div v-for="(item, index) in inboundForm.items" :key="index" class="order-item-row inbound-row">
          <el-input v-model="item.assetName" placeholder="资产名称" />
          <el-select v-model="item.categoryId" placeholder="分类">
            <el-option v-for="category in demoState.categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
          <el-select v-model="item.departmentId" placeholder="接收部门">
            <el-option v-for="department in demoState.departments" :key="department.id" :label="department.name" :value="department.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" />
          <el-input-number v-model="item.unitPrice" :min="0" />
          <el-button text type="danger" @click="inboundForm.items.splice(index, 1)">删除</el-button>
        </div>
        <el-button plain @click="inboundForm.items.push(newInboundItem())"><Plus :size="15" />添加明细</el-button>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialog = false">取消</el-button>
        <el-button type="primary" @click="saveInbound">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
