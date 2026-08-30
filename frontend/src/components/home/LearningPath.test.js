import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import LearningPath from './LearningPath.vue'

// Mount với router thật (không stub router-link) để assert link đích chính xác.
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div />' } },
    { path: '/lessons', component: { template: '<div />' } },
    { path: '/decks', component: { template: '<div />' } },
    { path: '/premium', component: { template: '<div />' } },
    { path: '/search', component: { template: '<div />' } },
    { path: '/leaderboard', component: { template: '<div />' } },
  ],
})

const mountPath = () => mount(LearningPath, {
  global: { plugins: [router] },
})

describe('LearningPath', () => {
  it('renders exactly 5 stations', () => {
    const wrapper = mountPath()
    const items = wrapper.findAll('ol > li')
    expect(items.length).toBe(5)
    // Mỗi trạm có node đánh số 1..5 trên đường ray
    const nodes = wrapper.findAll('.lp__node').map(n => n.text())
    expect(nodes).toEqual(['1', '2', '3', '4', '5'])
  })

  it('links every step to its real destination', () => {
    const wrapper = mountPath()
    const links = wrapper.findAll('a').map(a => a.attributes('href'))
    expect(links).toContain('/lessons')
    expect(links).toContain('/decks')
    expect(links).toContain('/premium?redirect=%2Fspeaking')
    expect(links).toContain('/search')
    expect(links).toContain('/leaderboard')
  })

  it('labels premium steps so guests are not surprised', () => {
    const wrapper = mountPath()
    expect(wrapper.text()).toContain('Premium')
    expect(wrapper.text()).toContain('Game cần đăng nhập')
    expect(wrapper.findComponent({ name: 'Lock' }).exists() || wrapper.html().includes('lucide')).toBe(true)
  })

  it('shows only numbers traceable to the DB query 2026-08-30', () => {
    const wrapper = mountPath()
    const text = wrapper.text()
    expect(text).toContain('1.473 bài')
    expect(text).toContain('107 bài nói')
    expect(text).toContain('11 bộ thẻ công khai')
    expect(text).toContain('41 người học')
    // Không được tái xuất hiện số đã bị loại khỏi spec
    expect(text).not.toContain('13 bộ')
    expect(text).not.toContain('46 người')
    expect(text).not.toContain('500+')
  })
})
