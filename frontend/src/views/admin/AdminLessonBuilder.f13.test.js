import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AdminLessonBuilder from '@/views/admin/AdminLessonBuilder.vue'
import lessonStructureService from '@/services/lessonStructureService'
import lessonService from '@/services/lessonService'

vi.mock('@/services/lessonStructureService')
vi.mock('@/services/lessonService')
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRoute: () => ({ params: { id: '447' } }) }
})

// Regression tests for audit-v13 F-13-02 / F-13-22.
//
// Measured live before the fix:
//   - the "Xem trước" button pointed at /lessons/447/preview, a route that does NOT
//     exist, so the catch-all redirected the admin to the homepage (/). The admin
//     believed they had previewed the lesson.
//   - blocks authored here are stored in lesson_sections/lesson_blocks and NO learner
//     view renders them, yet nothing in the UI said so.
describe('AdminLessonBuilder — F-13-02 / F-13-22', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // The component loads the lesson via getAll() (AdminLessonBuilder.vue:436), NOT getById.
    // Mocking the wrong method made the data load throw while the assertions still passed —
    // a green test over a crashed component.
    lessonService.getAll.mockResolvedValue([{ id: 447, title: 'Present simple' }])
    lessonStructureService.getAdminStructure.mockResolvedValue([])
    lessonStructureService.getSnapshots?.mockResolvedValue?.([])
  })

  const mountBuilder = async () => {
    const wrapper = mount(AdminLessonBuilder, {
      global: {
        stubs: {
          AppButton: {
            props: ['href', 'disabled', 'variant', 'loading', 'size', 'as'],
            template: '<a v-if="href" :href="href"><slot /></a><button v-else><slot /></button>',
          },
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  it('preview button points at the real lesson route, never a non-existent /preview', async () => {
    const wrapper = await mountBuilder()
    const hrefs = wrapper.findAll('a').map((a) => a.attributes('href')).filter(Boolean)
    // It must link to the lesson page...
    expect(hrefs).toContain('/lessons/447')
    // ...and must NOT use the dead /preview suffix.
    for (const h of hrefs) {
      expect(h).not.toMatch(/\/preview$/)
    }
  })

  it('warns that authored blocks are not shown to learners', async () => {
    const wrapper = await mountBuilder()
    const text = wrapper.text()
    expect(text).toMatch(/chưa được hiển thị/i)
    expect(text).toMatch(/Quản lý bài tập/)
  })
})
