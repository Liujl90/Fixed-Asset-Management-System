<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRightLeft,
  Bell,
  Boxes,
  Building2,
  ChevronRight,
  ClipboardCheck,
  LayoutDashboard,
  LogOut,
  Menu,
  PackageOpen,
  ShieldCheck,
  Tags,
  UserCircle,
  Users,
  X,
} from 'lucide-vue-next'
import {
  demoState,
  canAccess,
  isAdmin,
  logout,
  switchDemoIdentity,
} from '@/stores/backendStore'

const route = useRoute()
const router = useRouter()
const mobileMenuOpen = ref(false)

const adminGroups = [
  {
    label: '资产驾驶舱',
    items: [{ label: '资产总览', route: '/dashboard', icon: LayoutDashboard }],
  },
  {
    label: '资产中心',
    items: [{ label: '固定资产', route: '/assets', icon: Boxes }],
  },
  {
    label: '业务管理',
    items: [
      { label: '领用与归还', route: '/loans', icon: ClipboardCheck },
      { label: '资产调拨', route: '/transfers', icon: ArrowRightLeft },
    ],
  },
  {
    label: '基础数据',
    items: [
      { label: '部门管理', route: '/departments', icon: Building2 },
      { label: '员工管理', route: '/employees', icon: Users },
      { label: '资产分类', route: '/categories', icon: Tags },
    ],
  },
  {
    label: '系统设置',
    items: [{ label: '用户与角色', route: '/system', icon: ShieldCheck }],
  },
]

const employeeGroups = [
  {
    label: '个人工作台',
    items: [{ label: '我的资产', route: '/my-assets', icon: PackageOpen }],
  },
  {
    label: '资产服务',
    items: [{ label: '领用与归还', route: '/loans', icon: ClipboardCheck }],
  },
]

const menuGroups = computed(() => {
  if (demoState.currentUser?.roleCode === 'EMPLOYEE') return employeeGroups
  if (isAdmin()) return adminGroups
  return adminGroups.filter((group) => group.label !== '系统设置')
})

const currentTitle = computed(() => route.meta.title || '固定资产管理系统')
const today = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'short',
}).format(new Date())

function navigate(path) {
  router.push(path)
  mobileMenuOpen.value = false
}

async function handleSwitchIdentity() {
  const user = await switchDemoIdentity()
  if (!user) return
  await router.push(user.roleCode === 'EMPLOYEE' ? '/my-assets' : '/dashboard')
  ElMessage.success(`已切换为${user.roleName}身份`)
}

async function handleLogout() {
  await logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false" />

    <aside class="app-sidebar" :class="{ 'is-open': mobileMenuOpen }">
      <div class="brand-block">
        <div class="brand-mark">
          <Boxes :size="24" />
        </div>
        <div>
          <strong>资产云台</strong>
          <span>FIXED ASSET</span>
        </div>
        <button class="mobile-close" aria-label="关闭菜单" @click="mobileMenuOpen = false">
          <X :size="20" />
        </button>
      </div>

      <nav class="sidebar-nav">
        <section v-for="group in menuGroups" :key="group.label" class="nav-group">
          <div class="nav-group-label">{{ group.label }}</div>
          <button
            v-for="item in group.items"
            :key="item.route"
            class="nav-item"
            :class="{ active: route.path === item.route }"
            @click="navigate(item.route)"
          >
            <component :is="item.icon" :size="18" />
            <span>{{ item.label }}</span>
            <ChevronRight class="nav-arrow" :size="15" />
          </button>
        </section>
      </nav>

      <div class="sidebar-footer">
        <button class="nav-item" @click="navigate('/profile')">
          <UserCircle :size="18" />
          <span>个人信息</span>
          <ChevronRight class="nav-arrow" :size="15" />
        </button>
        <div class="demo-note">
          <span class="status-dot" />
          <div>
            <strong>Spring Boot API</strong>
            <small>JWT 认证 · 数据库持久化</small>
          </div>
        </div>
      </div>
    </aside>

    <main class="app-main">
      <header class="app-topbar">
        <div class="topbar-left">
          <button class="icon-button mobile-menu-button" aria-label="打开菜单" @click="mobileMenuOpen = true">
            <Menu :size="21" />
          </button>
          <div>
            <span class="eyebrow">{{ today }}</span>
            <h1>{{ currentTitle }}</h1>
          </div>
        </div>

        <div class="topbar-actions">
          <button class="topbar-action" title="切换体验身份" @click="handleSwitchIdentity">
            <ShieldCheck :size="18" />
            <span>切换体验身份</span>
          </button>
          <button class="icon-button notification-button" aria-label="通知">
            <Bell :size="19" />
            <span class="notice-dot" />
          </button>
          <div class="user-chip">
            <div class="user-avatar">{{ demoState.currentUser?.realName?.slice(0, 1) }}</div>
            <div class="user-meta">
              <strong>{{ demoState.currentUser?.realName }}</strong>
              <span>{{ demoState.currentUser?.roleName }}</span>
            </div>
          </div>
          <button class="icon-button" title="退出登录" aria-label="退出登录" @click="handleLogout">
            <LogOut :size="19" />
          </button>
        </div>
      </header>

      <div class="page-container">
        <router-view />
      </div>
    </main>
  </div>
</template>
