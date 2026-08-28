import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SpeakingList from './SpeakingList.vue'
import AdminSpeakingPrompts from '../admin/AdminSpeakingPrompts.vue'

const serviceMocks = vi.hoisted(() => ({
  getPrompts: vi.fn(),
  getAdminPrompts: vi.fn(),
  createPrompt: vi.fn(),
  updatePrompt: vi.fn(),
  deletePrompt: vi.fn(),
  getLessons: vi.fn()
}))

vi.mock('@/services/speakingService', () => ({
  default: {
    getPrompts: serviceMocks.getPrompts,
    getAdminPrompts: serviceMocks.getAdminPrompts,
    createPrompt: serviceMocks.createPrompt,
    updatePrompt: serviceMocks.updatePrompt,
    deletePrompt: serviceMocks.deletePrompt
  }
}))

vi.mock('@/services/lessonService', () => ({
  default: {
    getAll: serviceMocks.getLessons
  }
}))

const prompt = {
  id: 3,
  title: 'E2E Manual Grading',
  description: 'Manual grading flow',
  level: 'A1',
  category: 'E2E',
  isPremium: false
}

describe('Video Speaking prompt lists', () => {
  beforeEach(() => {
    serviceMocks.getPrompts.mockResolvedValue({
      content: [prompt],
      totalPages: 1,
      totalElements: 1
    })
    serviceMocks.getAdminPrompts.mockResolvedValue({
      content: [prompt],
      totalPages: 1,
      totalElements: 1
    })
    serviceMocks.getLessons.mockResolvedValue([])
  })

  it('renders an array response in the learner list', async () => {
    const wrapper = mount(SpeakingList, {
      global: {
        stubs: {
          RouterLink: { template: '<a><slot /></a>' }
        }
      }
    })

    await flushPromises()

    expect(wrapper.text()).toContain(prompt.title)
    expect(wrapper.text()).not.toContain('Chưa có bài tập nào.')
  })

  it('renders an array response in the admin list', async () => {
    const wrapper = mount(AdminSpeakingPrompts, {
      global: { stubs: { Pagination: true } }
    })

    await flushPromises()

    expect(wrapper.text()).toContain(prompt.title)
    expect(wrapper.text()).not.toContain('Chua co de bai nao')
  })
})
