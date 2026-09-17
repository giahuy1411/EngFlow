import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Profile from './Profile.vue'
import { useAuthStore } from '@/store/modules/auth'
import streakService from '@/services/streakService'
import dashboardService from '@/services/dashboardService'

vi.mock('@/services/streakService', () => ({
  default: { getHistory: vi.fn(), getCurrentStreak: vi.fn() }
}))
vi.mock('@/services/dashboardService', () => ({
  default: { getStats: vi.fn() }
}))
vi.mock('@/services/paymentService', () => ({
  default: { getStatus: vi.fn().mockResolvedValue({ isPremium: false }) }
}))

/** Header Profile phải hiện streak HIỆU LỰC từ /api/streak/current,
 *  không phải số thô treo trong localStorage.user (B1). */
describe('Profile streak header', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    streakService.getCurrentStreak.mockResolvedValue({ currentStreak: 5, today: '2026-09-16' })
    streakService.getHistory.mockResolvedValue([])
    dashboardService.getStats.mockResolvedValue({ completedLessons: 1, totalLessons: 2 })
  })

  it('shows effective streak from API instead of stale store value', async () => {
    const auth = useAuthStore()
    auth.token = 't'
    auth.user = {
      id: 1, username: 'u', fullName: 'U', isAdmin: false,
      currentStreak: 99, totalPoints: 10
    }

    const wrapper = shallowMount(Profile, {
      global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } }
    })
    await flushPromises()

    const numbers = wrapper.findAll('p.font-black.text-2xl')
    expect(numbers[0].text()).toBe('5')
  })
})