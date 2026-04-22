import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const root = resolve(__dirname, '..')

describe('sidebar dark theme styles', () => {
  it('uses dark sidebar tokens and sidebar-top brand layout', () => {
    const themeCss = readFileSync(resolve(root, 'styles/theme.css'), 'utf-8')
    const layoutVue = readFileSync(resolve(root, 'layouts/BackendLayout.vue'), 'utf-8')

    expect(themeCss).toContain('--color-sidebar-bg: #0f1b2d;')
    expect(themeCss).toContain('--color-sidebar-sub-bg: #13233b;')

    expect(layoutVue).toContain('color: rgba(255, 255, 255, 0.75);')
    expect(layoutVue).toContain('background: rgba(255, 255, 255, 0.08);')
    expect(layoutVue).toContain('color: #fff;')
    expect(layoutVue).toContain('background: var(--color-primary);')
    expect(layoutVue).toContain('background: #fff;')
    expect(layoutVue).toContain('class="sider-brand"')
    expect(layoutVue).toContain('sider-brand__title')
    expect(layoutVue).toContain('sider-brand__subtitle')
    expect(layoutVue).toContain('class="header-left"')
  })
})
