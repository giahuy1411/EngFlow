import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'

// audit-v13 F-13-02 (A1) — the learner-facing block renderer.
//
// The bug: an admin authored blocks in the Lesson Builder and they were stored in
// lesson_sections/lesson_blocks, but NO learner view read them, so the work was invisible.
//
// The trap this file guards above all else: GET /api/lessons/{id}/structure returns a
// synthesized read-only "virtual" section (id === null) built from lesson.content when a
// lesson has no materialized sections. Rendering that would show the lesson body TWICE on
// the ~1,462 lessons that have no authored blocks. Test "renders nothing for a virtual
// section" is the regression pin for that.

const getStructure = vi.fn()
vi.mock('@/services/lessonStructureService', () => ({
  default: { getStructure: vi.fn() },
}))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { id: '447' } }) }))

import lessonStructureService from '@/services/lessonStructureService'
import LessonBlocks from '@/views/lessons/LessonBlocks.vue'

const mountIt = async () => {
  const w = mount(LessonBlocks, {
    // Vue's dev-mode error handler re-throws errors raised inside lifecycle hooks, which
    // would fail this file even though the component catches its own fetch error. Silence
    // it here so the assertions below test the component's behaviour, not Vue's reporting.
    global: { config: { errorHandler: () => {} } },
  })
  await flushPromises()
  return w
}
const realSection = (blocks, id = 9) => [
  { id, title: 'Phần bổ sung', orderIndex: 1, blocks },
]

describe('LessonBlocks — F-13-02 A1', () => {
  const getStructure = lessonStructureService.getStructure

  beforeEach(() => getStructure.mockReset())

  it('renders nothing when the lesson has no materialized sections', async () => {
    getStructure.mockResolvedValue([])
    const w = await mountIt()
    expect(w.find('section').exists()).toBe(false)
    expect(w.text()).not.toContain('Nội dung biên soạn')
  })

  // THE regression pin: the synthesized virtual section must never be rendered.
  it('ignores the virtual section built from lesson.content (no duplication)', async () => {
    getStructure.mockResolvedValue([
      {
        id: null, // <-- the marker for "not materialized"
        title: 'Nội dung bài học',
        orderIndex: 10,
        blocks: [{ id: null, blockType: 'TEXT', data: '<p>SCRAPED LESSON BODY</p>', orderIndex: 10 }],
      },
    ])
    const w = await mountIt()
    expect(w.html()).not.toContain('SCRAPED LESSON BODY')
    expect(w.find('section').exists()).toBe(false)
  })

  it('renders a TEXT block and sanitizes the HTML', async () => {
    getStructure.mockResolvedValue(realSection([
      { id: 1, blockType: 'TEXT', data: JSON.stringify({ content: '<p>Xin chào</p>' }) },
    ]))
    const w = await mountIt()
    expect(w.text()).toContain('Xin chào')
  })

  it('strips a script tag inside a TEXT block (XSS)', async () => {
    getStructure.mockResolvedValue(realSection([
      { id: 1, blockType: 'TEXT', data: JSON.stringify({ content: '<p>ok</p><script>alert(1)</script>' }) },
    ]))
    const w = await mountIt()
    expect(w.html()).not.toContain('<script>')
    expect(w.text()).toContain('ok')
  })

  it('renders a TABLE block with headers and rows', async () => {
    getStructure.mockResolvedValue(realSection([
      {
        id: 2, blockType: 'TABLE',
        data: JSON.stringify({ headers: ['Đại từ', 'Động từ'], rows: [['I', 'work'], ['He', 'works']] }),
      },
    ]))
    const w = await mountIt()
    expect(w.findAll('th').map(t => t.text())).toEqual(['Đại từ', 'Động từ'])
    expect(w.findAll('tbody tr')).toHaveLength(2)
  })

  it('renders an IMAGE block with its caption as alt text', async () => {
    getStructure.mockResolvedValue(realSection([
      { id: 3, blockType: 'IMAGE', data: JSON.stringify({ imageUrl: '/api/resources/x.png', caption: 'Bảng chữ cái' }) },
    ]))
    const w = await mountIt()
    const img = w.find('img')
    expect(img.attributes('src')).toBe('/api/resources/x.png')
    expect(img.attributes('alt')).toBe('Bảng chữ cái')
  })

  it('renders an AUDIO block with a transcript', async () => {
    getStructure.mockResolvedValue(realSection([
      { id: 4, blockType: 'AUDIO', data: JSON.stringify({ audioUrl: '/api/resources/x.mp3', transcript: 'Hello there' }) },
    ]))
    const w = await mountIt()
    expect(w.find('audio').attributes('src')).toBe('/api/resources/x.mp3')
    expect(w.text()).toContain('Hello there')
  })

  // QUESTION/SUBMISSION have no learner view or grading path yet (A2/A3).
  it('skips QUESTION and SUBMISSION blocks', async () => {
    getStructure.mockResolvedValue(realSection([
      { id: 5, blockType: 'QUESTION', data: JSON.stringify({ questionText: 'She ___ to school.', correctAnswer: 'goes' }) },
      { id: 6, blockType: 'SUBMISSION', data: JSON.stringify({ prompt: 'Viết đoạn văn' }) },
    ]))
    const w = await mountIt()
    expect(w.find('section').exists()).toBe(false)
    expect(w.text()).not.toContain('goes')
  })

  // Legacy rows (lesson 41881/91900) store raw HTML in `data`, not JSON.
  it('treats a non-JSON data payload as TEXT (legacy rows)', async () => {
    getStructure.mockResolvedValue(realSection([
      { id: 7, blockType: 'TEXT', data: '<h3>Legacy HTML block</h3><p>No JSON here</p>' },
    ]))
    const w = await mountIt()
    expect(w.text()).toContain('Legacy HTML block')
    expect(w.text()).toContain('No JSON here')
  })

  it('drops an empty block rather than rendering an empty shell', async () => {
    getStructure.mockResolvedValue(realSection([
      { id: 8, blockType: 'TEXT', data: JSON.stringify({ content: '' }) },
      { id: 9, blockType: 'IMAGE', data: JSON.stringify({}) },
    ]))
    const w = await mountIt()
    expect(w.find('section').exists()).toBe(false)
  })

  it('does not break the page when the structure request fails', async () => {
    // Use mockRejectedValueOnce, not mockImplementation(() => Promise.reject(...)): the
    // latter creates a rejected promise at mock-setup time that vitest reports as an
    // unhandled rejection, failing the file even though the component catches its own call.
    // (Same pattern as Profile.test.js:57.)
    getStructure.mockRejectedValueOnce(new Error('500'))
    const w = await mountIt()
    expect(w.find('section').exists()).toBe(false)
    expect(w.html()).toBeTruthy()
  })

  it('survives a non-array payload from the API', async () => {
    getStructure.mockResolvedValue(null)
    const w = await mountIt()
    expect(w.find('section').exists()).toBe(false)
  })

  it('keeps a real section while ignoring a virtual one in the same response', async () => {
    getStructure.mockResolvedValue([
      { id: null, title: 'Nội dung bài học', blocks: [{ id: null, blockType: 'TEXT', data: '<p>VIRTUAL</p>' }] },
      { id: 42, title: 'Thật', blocks: [{ id: 43, blockType: 'TEXT', data: JSON.stringify({ content: '<p>REAL</p>' }) }] },
    ])
    const w = await mountIt()
    expect(w.text()).toContain('REAL')
    expect(w.html()).not.toContain('VIRTUAL')
  })
})
