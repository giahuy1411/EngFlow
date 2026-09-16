import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminVideoLessons from './AdminVideoLessons.vue'

const serviceMocks = vi.hoisted(() => ({
  getAllAdmin: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  remove: vi.fn()
}))

vi.mock('@/services/videoLessonService', () => ({
  default: {
    getAllAdmin: serviceMocks.getAllAdmin,
    create: serviceMocks.create,
    update: serviceMocks.update,
    remove: serviceMocks.remove
  }
}))

/** Dung hinh dang THAT cua VideoLessonSummary: co youtubeVideoId, KHONG co youtubeUrl. */
const lesson = {
  id: 12,
  title: 'Daily Routine',
  description: 'Chu de hang ngay',
  youtubeVideoId: '2VeQTuSSiI0',
  level: 'PRE_INTERMEDIATE',
  category: 'DAILY',
  durationSeconds: 90,
  lineCount: 6,
  isPublished: true
}

function mountPage() {
  return mount(AdminVideoLessons, {
    global: {
      stubs: {
        RouterLink: { template: '<a><slot /></a>' },
        AppModal: { template: '<div><slot /><slot name="footer" /></div>', props: ['modelValue'] }
      }
    }
  })
}

describe('AdminVideoLessons', () => {
  beforeEach(() => {
    serviceMocks.getAllAdmin.mockResolvedValue({ content: [lesson] })
    serviceMocks.update.mockResolvedValue({ id: lesson.id })
    serviceMocks.create.mockResolvedValue({ id: 99 })
    serviceMocks.remove.mockResolvedValue({})
  })

  it('renders a Sua button next to Xem/Xoa for each row', async () => {
    const wrapper = mountPage()
    await flushPromises()

    const buttons = wrapper.findAll('button').map(b => b.text())
    expect(buttons).toContain('Sửa')
    expect(buttons).toContain('Xóa')
  })

  it('rebuilds the YouTube url from youtubeVideoId when opening the edit form', async () => {
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.findAll('button').find(b => b.text() === 'Sửa').trigger('click')

    const urlInput = wrapper.find('#vl-url')
    expect(urlInput.element.value).toBe('https://www.youtube.com/watch?v=2VeQTuSSiI0')
    expect(wrapper.find('#vl-title').element.value).toBe('Daily Routine')
    expect(wrapper.find('#vl-transcript').element.value).toBe('')
  })

  it('saves an edit with transcript null so the backend keeps the existing transcript', async () => {
    const wrapper = mountPage()
    await flushPromises()

    await wrapper.findAll('button').find(b => b.text() === 'Sửa').trigger('click')
    await wrapper.find('#vl-title').setValue('Daily Routine v2')
    await wrapper.find('#video-lesson-form').trigger('submit')
    await flushPromises()

    expect(serviceMocks.update).toHaveBeenCalledTimes(1)
    const [id, payload] = serviceMocks.update.mock.calls[0]
    expect(id).toBe(12)
    expect(payload).toMatchObject({
      title: 'Daily Routine v2',
      youtubeUrl: 'https://www.youtube.com/watch?v=2VeQTuSSiI0',
      transcript: null
    })
  })
})
