import { createRouter, createWebHistory } from 'vue-router'
import { canAccess, demoState } from '@/stores/backendStore'
import MainLayout from '@/layouts/MainLayout.vue'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'
import DepartmentsView from '@/views/DepartmentsView.vue'
import EmployeesView from '@/views/EmployeesView.vue'
import CategoriesView from '@/views/CategoriesView.vue'
import AssetsView from '@/views/AssetsView.vue'
import LoansView from '@/views/LoansView.vue'
import TransfersView from '@/views/TransfersView.vue'
import OperationsView from '@/views/OperationsView.vue'
import SupplyChainView from '@/views/SupplyChainView.vue'
import InventoryScrapView from '@/views/InventoryScrapView.vue'
import SystemView from '@/views/SystemView.vue'
import MyAssetsView from '@/views/MyAssetsView.vue'
import ProfileView from '@/views/ProfileView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        redirect: () => (canAccess('dashboard') ? '/dashboard' : '/my-assets'),
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: { title: '资产总览', permission: 'dashboard' },
      },
      {
        path: 'departments',
        name: 'departments',
        component: DepartmentsView,
        meta: { title: '部门管理', permission: 'departments' },
      },
      {
        path: 'employees',
        name: 'employees',
        component: EmployeesView,
        meta: { title: '员工管理', permission: 'employees' },
      },
      {
        path: 'categories',
        name: 'categories',
        component: CategoriesView,
        meta: { title: '资产分类', permission: 'categories' },
      },
      {
        path: 'assets',
        name: 'assets',
        component: AssetsView,
        meta: { title: '固定资产', permission: 'assets' },
      },
      {
        path: 'loans',
        name: 'loans',
        component: LoansView,
        meta: { title: '领用与归还', permission: 'loans' },
      },
      {
        path: 'transfers',
        name: 'transfers',
        component: TransfersView,
        meta: { title: '资产调拨', permission: 'transfers' },
      },
      {
        path: 'operations',
        name: 'operations',
        component: OperationsView,
        meta: { title: '运维任务', permission: 'operations' },
      },
      {
        path: 'supply-chain',
        name: 'supply-chain',
        component: SupplyChainView,
        meta: { title: '采购与入库', permission: 'supply' },
      },
      {
        path: 'inventory-scrap',
        name: 'inventory-scrap',
        component: InventoryScrapView,
        meta: { title: '盘点与报废', permission: 'inventory' },
      },
      {
        path: 'system',
        name: 'system',
        component: SystemView,
        meta: { title: '用户与角色', permission: 'system' },
      },
      {
        path: 'my-assets',
        name: 'my-assets',
        component: MyAssetsView,
        meta: { title: '我的资产', permission: 'my-assets' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: ProfileView,
        meta: { title: '个人信息', permission: 'profile' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const loggedIn = Boolean(demoState.currentUser)
  if (!to.meta.public && !loggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && loggedIn) {
    return demoState.currentUser.roleCode === 'ADMIN' ? '/dashboard' : '/my-assets'
  }
  if (to.meta.permission && !canAccess(to.meta.permission)) {
    return canAccess('dashboard') ? '/dashboard' : '/my-assets'
  }
  return true
})

export default router
