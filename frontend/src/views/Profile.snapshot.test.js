import { createServer } from 'node:http'
import { readFileSync } from 'node:fs'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { describe, it, expect, vi } from 'vitest'
import Profile from './Profile.vue'
import api from '@/services/api'

vi.mock('@/services/dashboardService', () => ({
  default: { getStats: vi.fn().mockResolvedValue({ completedLessons: 1, totalLessons: 2 }) }
}))
vi.mock('@/services/paymentService', () => ({
  default: { getStatus: vi.fn().mockResolvedValue({ isPremium: false }) }
}))

describe('Profile backend serialization contract', () => {
  it.skipIf(!process.env.STUDY_SNAPSHOT_CONTRACT)('renders the controller-produced HTTP snapshot through the actual client', async () => {
    const json = readFileSync(process.env.STUDY_SNAPSHOT_CONTRACT, 'utf8')
    const requests = []
    const server = createServer((request, response) => {
      requests.push(request.url)
      response.writeHead(request.url === '/api/streak/snapshot' ? 200 : 404,
        { 'Content-Type': 'application/json' })
      response.end(request.url === '/api/streak/snapshot' ? json : '{}')
    })
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
    const previousBase = api.defaults.baseURL
    const previousAdapter = api.defaults.adapter
    let wrapper
    try {
      api.defaults.baseURL = `http://127.0.0.1:${server.address().port}`
      api.defaults.adapter = 'http'
      localStorage.clear()
      const pinia = createPinia()
      setActivePinia(pinia)
      const router = createRouter({ history: createMemoryHistory(),
        routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
      await router.push('/profile')
      await router.isReady()
      wrapper = mount(Profile, { global: { plugins: [pinia, router] } })
      await vi.waitFor(() => expect(wrapper.find('[data-date="2026-09-20"]').exists()).toBe(true))
      await flushPromises()
      expect(requests).toEqual(['/api/streak/snapshot'])
      expect(wrapper.get('[data-date="2026-09-20"]').attributes('aria-label')).toContain('đã học')
      expect(wrapper.get('[data-date="2026-09-19"]').attributes('aria-label')).toContain('Không học')
      expect(wrapper.get('[data-date="2026-09-17"]').attributes('aria-label')).not.toContain('Không học')
      expect(wrapper.get('.streak-calendar').classes()).not.toContain('border-tertiary')
      expect(wrapper.findAll('p.font-black.text-2xl')[0].text()).toBe('1')
    } finally {
      wrapper?.unmount()
      api.defaults.baseURL = previousBase
      api.defaults.adapter = previousAdapter
      await new Promise(resolve => server.close(resolve))
    }
  })
})
