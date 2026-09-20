import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Profile from './Profile.vue'
import { useAuthStore } from '@/store/modules/auth'
import streakService from '@/services/streakService'
import dashboardService from '@/services/dashboardService'

vi.mock('@/services/streakService', () => ({
  default: { getHistory: vi.fn(), getCurrentStreak: vi.fn(), getSnapshot: vi.fn() }
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
  const wrappers = []
  afterEach(() => {
    wrappers.splice(0).forEach(wrapper => wrapper.unmount())
    vi.useRealTimers()
  })
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    streakService.getCurrentStreak.mockResolvedValue({ currentStreak: 5, today: '2026-09-16' })
    streakService.getHistory.mockResolvedValue([])
    streakService.getSnapshot.mockResolvedValue({ currentStreak: 5, today: '2026-09-16',
      studiedToday: false, studiedDays: [], effectiveFrom: '2026-09-01',
      legacyAccessDays: [], legacyHistoryAvailable: true })
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
    wrappers.push(wrapper)
    await flushPromises()

    const numbers = wrapper.findAll('p.font-black.text-2xl')
    expect(numbers[0].text()).toBe('5')
  })

  it('shows retry instead of a false empty calendar after an API failure', async () => {
    streakService.getSnapshot.mockRejectedValueOnce(new Error('offline'))
    const wrapper = shallowMount(Profile)
    wrappers.push(wrapper)
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('Không tải được lịch học')
    expect(wrapper.findComponent({ name: 'StreakCalendar' }).exists()).toBe(false)
    await wrapper.get('[data-testid="study-retry"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(streakService.getSnapshot).toHaveBeenCalledTimes(2)
  })

  it('refreshes on focus and stops listening after unmount', async () => {
    const wrapper = shallowMount(Profile)
    wrappers.push(wrapper)
    await flushPromises()
    window.dispatchEvent(new Event('focus'))
    await flushPromises()
    expect(streakService.getSnapshot).toHaveBeenCalledTimes(2)
    wrapper.unmount()
    wrappers.pop()
    window.dispatchEvent(new Event('focus'))
    expect(streakService.getSnapshot).toHaveBeenCalledTimes(2)
  })

  it('refreshes at Vietnam midnight even if the browser timezone differs', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-18T16:59:59Z'))
    const wrapper = shallowMount(Profile)
    wrappers.push(wrapper)
    await flushPromises()
    await vi.advanceTimersByTimeAsync(1100)
    expect(streakService.getSnapshot).toHaveBeenCalledTimes(2)
  })
})
