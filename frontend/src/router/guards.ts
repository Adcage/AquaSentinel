import type { Router } from 'vue-router'

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to, _from, next) => {
    const token = sessionStorage.getItem('token')

    // 设置页面标题
    if (to.meta?.title) {
      document.title = `${to.meta.title} - AI防溺水监测预警系统`
    } else {
      document.title = 'AI防溺水监测预警系统'
    }

    if (to.path.startsWith('/admin') && !token) {
      next('/user/login')
      return
    }

    if (to.path.startsWith('/user/') && token && to.path !== '/user/register') {
      next('/admin/dashboard')
      return
    }

    next()
  })

  router.afterEach((_to, _from) => {
    // console.log(`路由跳转: ${from.path} -> ${to.path}`)
  })

  router.onError((error) => {
    console.error('路由错误:', error)
  })
}
