import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import LessonLayout from '@/views/lessons/LessonLayout.vue'
import lessonService from '@/services/lessonService'

vi.mock('@/services/lessonService')
vi.mock('@/services/streakService', () => ({ default: { getCurrentStreak: vi.fn().mockResolvedValue({ currentStreak: 0 }) } }))
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRoute: () => ({ params: { id: '445' } }) }
})

// Regression test for audit-v13 F-13-18.
//
// Measured live on /lessons/445: the route rendered NO h1 at all and its heading order
// started at h3 — a WCAG 2.4.6 / 1.3.1 defect (no page title, skipped level). The lesson
// title is the natural h1.
describe('LessonLayout — F-13-18 lesson page must expose exactly one h1', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    lessonService.getById.mockResolvedValue({
      id: 445,
      title: 'English Grammar Exercises for A1 – have got and articles',
    })
  })

  const mountLayout = async () => {
    const wrapper = mount(LessonLayout, {
      global: {
        plugins: [createPinia()],
        stubs: {
          StreakBanner: true,
          LessonContent: true,
          LessonExerciseTab: true,
          LessonPreview: true,
          GuestCtaCard: true,
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  it('renders exactly one h1 containing the lesson title', async () => {
    const wrapper = await mountLayout()
    const h1s = wrapper.findAll('h1')
    expect(h1s.length).toBe(1)
    expect(h1s[0].text()).toContain('English Grammar Exercises for A1')
  })

  it('falls back to a generic title when the lesson cannot be loaded', async () => {
    lessonService.getById.mockRejectedValue(new Error('network'))
    const wrapper = await mountLayout()
    const h1s = wrapper.findAll('h1')
    expect(h1s.length).toBe(1)
    expect(h1s[0].text().trim().length).toBeGreaterThan(0)
  })
})
