import type { RouteRecordRaw } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'

const basicRoutes: RouteRecordRaw[] = [
  {
    path: '/user/login',
    name: 'Login',
    component: LoginView,
    meta: { title: '管理员登录', hideNav: true }
  },
  {
    path: '/user/register',
    name: 'Register',
    component: RegisterView,
    meta: { title: '账号登记示意页', hideNav: true }
  },
  {
    path: '/',
    redirect: '/admin/dashboard'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/admin/dashboard'
  }
]

export default basicRoutes
