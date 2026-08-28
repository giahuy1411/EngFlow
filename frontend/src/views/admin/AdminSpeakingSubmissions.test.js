import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminSpeakingSubmissions from './AdminSpeakingSubmissions.vue'

const serviceMocks = vi.hoisted(() => ({
  getAdminSubmissions: vi.fn(),
  getSubmissions: vi.fn(),
  gradeSubmission: vi.fn()
}))

vi.mock('@/services/speakingService', () => ({
  default: {
    getAdminSubmissions: serviceMocks.getAdminSubmissions,
    getSubmissions: serviceMocks.getSubmissions,
    gradeSubmission: serviceMocks.gradeSubmission
  }
}))

const submission = {
  id: 42,
  userId: 7,
  user: { fullName: 'Nguyen Van A' },
  promptId: 3,
  promptTitle: 'Describe your hometown',
  status: 'SUBMITTED',
  videoUrl: 'http://localhost/media/submission-42.webm',
  score: null,
  submittedAt: '2026-07-28T12:00:00'
}

describe('AdminSpeakingSubmissions', () => {
  beforeEach(() => {
    serviceMocks.getAdminSubmissions.mockResolvedValue({
      content: [submission],
      totalPages: 1,
      totalElements: 1
    })
    serviceMocks.getSubmissions.mockResolvedValue({
      content: [],
      totalPages: 1,
      totalElements: 0
    })
  })

  it('loads the admin page endpoint and renders its Spring Page content', async () => {
    const wrapper = mount(AdminSpeakingSubmissions, {
      global: { stubs: { Pagination: true } }
    })

    await flushPromises()

    expect(serviceMocks.getAdminSubmissions).toHaveBeenCalledWith(0, 10, '')
    expect(serviceMocks.getSubmissions).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Nguyen Van A')
    expect(wrapper.text()).toContain('Describe your hometown')
  })
})
