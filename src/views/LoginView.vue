<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Boxes,
  CheckCircle2,
  LockKeyhole,
  ShieldCheck,
  UserRound,
} from 'lucide-vue-next'
import { login } from '@/stores/backendStore'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '123456',
})

function fillAccount(type) {
  form.username = type === 'admin' ? 'admin' : type === 'manager' ? 'manager' : 'employee'
  form.password = '123456'
}

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const user = await login(form.username.trim(), form.password)
    ElMessage.success(`欢迎回来，${user.realName}`)
    const fallback = user.roleCode === 'EMPLOYEE' ? '/my-assets' : '/dashboard'
    await router.replace(route.query.redirect || fallback)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-intro">
      <div class="login-brand">
        <div class="brand-mark large">
          <Boxes :size="30" />
        </div>
        <div>
          <strong>资产云台</strong>
          <span>FIXED ASSET MANAGEMENT</span>
        </div>
      </div>

      <div class="login-copy">
        <span class="login-kicker">企业固定资产全生命周期管理</span>
        <h1>让每一台设备，都有清晰的去向。</h1>
        <p>
          从资产登记、领用到归还与调拨，用一套连续的业务记录，让企业资产状态、部门归属和责任人都保持一致。
        </p>
      </div>

      <div class="lifecycle-strip">
        <div>
          <span>01</span>
          <strong>登记</strong>
        </div>
        <ArrowRight :size="18" />
        <div>
          <span>02</span>
          <strong>领用</strong>
        </div>
        <ArrowRight :size="18" />
        <div>
          <span>03</span>
          <strong>归还</strong>
        </div>
        <ArrowRight :size="18" />
        <div>
          <span>04</span>
          <strong>调拨</strong>
        </div>
      </div>
    </section>

    <section class="login-panel-wrap">
      <div class="login-panel">
        <div class="login-panel-heading">
          <span>DEMO ACCESS</span>
          <h2>登录资产管理系统</h2>
          <p>选择体验身份，进入对应的业务视图。</p>
        </div>

        <div class="identity-switcher">
          <button
            :class="{ active: form.username === 'admin' }"
            type="button"
            @click="fillAccount('admin')"
          >
            <ShieldCheck :size="20" />
            <span>
              <strong>管理员</strong>
              <small>管理全部资产与业务</small>
            </span>
          </button>
          <button
            :class="{ active: form.username === 'employee' }"
            type="button"
            @click="fillAccount('employee')"
          >
            <UserRound :size="20" />
            <span>
              <strong>普通员工</strong>
              <small>申请领用与归还</small>
            </span>
          </button>
          <button
            :class="{ active: form.username === 'manager' }"
            type="button"
            @click="fillAccount('manager')"
          >
            <ShieldCheck :size="20" />
            <span>
              <strong>资产管理员</strong>
              <small>管理资产生命周期</small>
            </span>
          </button>
        </div>

        <el-form :model="form" label-position="top" size="large" @submit.prevent="handleLogin">
          <el-form-item label="登录账号">
            <el-input v-model="form.username" placeholder="请输入登录账号">
              <template #prefix>
                <UserRound :size="17" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入登录密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <LockKeyhole :size="17" />
              </template>
            </el-input>
          </el-form-item>

          <div class="demo-credentials">
            <CheckCircle2 :size="17" />
            <span>演示账号已填充，密码统一为 <strong>123456</strong></span>
          </div>

          <el-button
            class="login-submit"
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            进入系统
            <ArrowRight :size="18" />
          </el-button>
        </el-form>

        <div class="login-footnote">
          当前页面通过 JWT 连接 Spring Boot REST API，业务数据持久化到数据库。
        </div>
      </div>
    </section>
  </div>
</template>
