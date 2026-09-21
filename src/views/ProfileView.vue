<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Building2,
  Mail,
  Phone,
  Save,
  ShieldCheck,
  UserRound,
} from 'lucide-vue-next'
import {
  demoState,
  departmentName,
  getEmployee,
  updateProfile,
} from '@/stores/backendStore'

const saving = ref(false)
const form = reactive({
  realName: '',
  phone: '',
  email: '',
})
const currentEmployee = computed(() =>
  demoState.currentUser?.employeeId ? getEmployee(demoState.currentUser.employeeId) : null,
)

watch(
  () => demoState.currentUser,
  (user) => {
    Object.assign(form, {
      realName: user?.realName || '',
      phone: user?.phone || '',
      email: user?.email || '',
    })
  },
  { immediate: true },
)

async function save() {
  saving.value = true
  try {
    await updateProfile({ ...form })
    ElMessage.success('个人信息已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">PROFILE</span>
        <h2>个人信息</h2>
        <p>查看账号身份和员工档案，维护基础联系方式。</p>
      </div>
    </section>

    <section class="profile-grid">
      <article class="profile-summary">
        <div class="profile-avatar">
          {{ demoState.currentUser?.realName?.slice(0, 1) }}
        </div>
        <h3>{{ demoState.currentUser?.realName }}</h3>
        <span>{{ demoState.currentUser?.roleName }}</span>
        <div class="profile-facts">
          <div>
            <ShieldCheck :size="17" />
            <span>登录账号</span>
            <strong>{{ demoState.currentUser?.username }}</strong>
          </div>
          <div>
            <Building2 :size="17" />
            <span>所属部门</span>
            <strong>{{ departmentName(currentEmployee?.departmentId) }}</strong>
          </div>
        </div>
      </article>

      <article class="panel profile-form-panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">BASIC INFORMATION</span>
            <h3>基础资料</h3>
          </div>
        </div>
        <el-form :model="form" label-position="top" class="profile-form">
          <el-form-item label="姓名">
            <el-input v-model="form.realName">
              <template #prefix><UserRound :size="16" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone">
              <template #prefix><Phone :size="16" /></template>
            </el-input>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email">
              <template #prefix><Mail :size="16" /></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="save">
            <Save :size="16" />
            保存修改
          </el-button>
        </el-form>
      </article>
    </section>
  </div>
</template>
