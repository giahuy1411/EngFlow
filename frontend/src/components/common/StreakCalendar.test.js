import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StreakCalendar from './StreakCalendar.vue'

describe('StreakCalendar study status', () => {
  it('marks the calendar card when today is not studied', () => {
    const wrapper = mount(StreakCalendar, {
      props: { currentStreak: 1, history: ['2026-09-17'], today: '2026-09-18' },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    expect(wrapper.find('.streak-calendar').classes()).toContain('border-tertiary')
    expect(wrapper.text()).toContain('Hôm nay chưa học')
    expect(wrapper.find('a[href="/lessons"]').text()).toContain('Học ngay')
  })

  it('does not mark the calendar card after studying today', () => {
    const wrapper = mount(StreakCalendar, {
      props: {
        currentStreak: 2,
        history: ['2026-09-17', '2026-09-18'],
        today: '2026-09-18'
      },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    expect(wrapper.find('.streak-calendar').classes()).not.toContain('border-tertiary')
    expect(wrapper.text()).not.toContain('Hôm nay chưa học')
    expect(wrapper.text()).not.toContain('Học ngay')
  })

  it('distinguishes a missed past day from the current day', () => {
    const wrapper = mount(StreakCalendar, {
      props: { currentStreak: 0, history: [], today: '2026-09-19' },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const yesterday = wrapper.get('[data-date="2026-09-18"]')
    const today = wrapper.get('[data-date="2026-09-19"]')
    expect(yesterday.attributes('aria-label')).toContain('Không học')
    expect(today.attributes('aria-label')).toContain('Chưa học hôm nay')
  })

  /**
   * Trước cutover, lịch sử truy cập cũ chỉ tồn tại trong aria-label: về mặt thị
   * giác nó bị tô y hệt một ngày bỏ học. Người không dùng screen reader thấy lịch
   * sử của mình bị xoá sạch.
   */
  it('paints a legacy access day distinctly instead of as a missed day', () => {
    const wrapper = mount(StreakCalendar, {
      props: {
        currentStreak: 0,
        history: [],
        legacyHistory: ['2026-09-18'],
        effectiveFrom: '2026-09-20',
        today: '2026-09-20'
      },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const legacy = wrapper.get('[data-date="2026-09-18"]')
    expect(legacy.classes()).toContain('border-dashed')
    // Không được mang đồng thời class của "ngày bỏ học": object `:class` của Vue
    // emit mọi key truthy, nên nếu nhánh missed không loại trừ legacy thì ô này
    // có cả hai nền và màu thắng do thứ tự stylesheet quyết định.
    expect(legacy.classes()).not.toContain('bg-muted')
    expect(legacy.attributes('aria-label')).toContain('Lịch sử truy cập')
  })

  /**
   * Regression cho bẫy Vue: object `:class` emit MỌI key truthy, không tự phân
   * giải xung đột. Nếu nhánh "missed" không loại trừ ngày legacy thì element sẽ
   * mang cả `bg-muted` lẫn nền mới, và màu thắng do thứ tự stylesheet quyết định.
   */
  it('keeps a pre-cutover day with no legacy activity looking missed', () => {
    const wrapper = mount(StreakCalendar, {
      props: {
        currentStreak: 0,
        history: [],
        legacyHistory: ['2026-09-18'],
        effectiveFrom: '2026-09-20',
        today: '2026-09-20'
      },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const missed = wrapper.get('[data-date="2026-09-17"]')
    expect(missed.classes()).toContain('bg-muted')
    expect(missed.classes()).not.toContain('border-dashed')
  })

  /**
   * Câu giải thích cutover phải hướng người học, không phải ngôn ngữ migration
   * ("lịch sử truy cập", "ngày hoàn thành học") vốn chỉ có nghĩa với lập trình viên.
   */
  it('explains the cutover without migration jargon', () => {
    const wrapper = mount(StreakCalendar, {
      props: {
        currentStreak: 0,
        history: [],
        legacyHistory: [],
        effectiveFrom: '2026-09-20',
        today: '2026-09-20'
      },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    expect(wrapper.text()).not.toContain('không phải ngày hoàn thành học')
    expect(wrapper.text()).toContain('hoàn thành bài tập')
  })
})
