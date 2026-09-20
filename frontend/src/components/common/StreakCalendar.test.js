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
   * Lịch chỉ có hai trạng thái nền: đã học và chưa học. Ngày đã qua không học và
   * ngày chưa tới phải trông y hệt nhau, nếu không người dùng lại phải học thêm
   * một quy ước màu nữa để đọc được lịch.
   */
  it('paints a past missed day and a future day identically', () => {
    // today là Thứ Hai 21/9 → tuần hiện tại 21–27 vẫn còn ngày chưa tới trong lưới.
    const wrapper = mount(StreakCalendar, {
      props: { currentStreak: 0, history: [], today: '2026-09-21' },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const missed = wrapper.get('[data-date="2026-09-17"]')
    const future = wrapper.get('[data-date="2026-09-25"]')

    for (const cell of [missed, future]) {
      expect(cell.classes()).toContain('bg-muted')
      expect(cell.classes()).not.toContain('bg-white')
      expect(cell.classes()).not.toContain('opacity-50')
      expect(cell.classes()).not.toContain('bg-quaternary')
    }
  })

  /**
   * Ngày đã học dùng cùng hình vuông bo góc với mọi ô khác; chỉ khác nền và độ
   * đậm chữ. Không còn bo blob riêng, không còn dấu tick trong ô.
   */
  it('marks a studied day with a green fill and no extra glyph', () => {
    const wrapper = mount(StreakCalendar, {
      props: { currentStreak: 1, history: ['2026-09-17'], today: '2026-09-20' },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const studied = wrapper.get('[data-date="2026-09-17"]')
    expect(studied.classes()).toContain('bg-quaternary')
    expect(studied.classes()).toContain('rounded-md')
    expect(studied.classes()).toContain('font-black')
    expect(studied.classes()).not.toContain('bg-muted')
    expect(studied.classes()).not.toContain('rounded-blob')
    // Ô chỉ còn đúng con số, không thêm ký tự nào.
    expect(studied.text()).toBe('17')
    // Trạng thái vẫn phải đọc được với screen reader (WCAG 1.4.1).
    expect(studied.attributes('aria-label')).toContain('đã học')
  })

  /**
   * "Hôm nay" trước đây chỉ là viền 2px nên rất dễ lẫn. Viền phải dày (ring-4)
   * và có khe trắng (ring-offset) để tách khỏi ô bên cạnh.
   */
  it('makes today stand out with a thick offset ring', () => {
    const wrapper = mount(StreakCalendar, {
      props: { currentStreak: 1, history: ['2026-09-23'], today: '2026-09-23' },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const today = wrapper.get('[data-date="2026-09-23"]')
    expect(today.classes()).toContain('ring-4')
    expect(today.classes()).toContain('ring-accent')
    expect(today.classes()).toContain('ring-offset-2')
    // Hôm nay vẫn phải mang trạng thái học của chính nó, không phải màu thứ ba.
    expect(today.classes()).toContain('bg-quaternary')

    const other = wrapper.get('[data-date="2026-09-22"]')
    expect(other.classes()).not.toContain('ring-4')
  })

  /**
   * Giao diện chỉ còn ba trạng thái. Mọi khái niệm về "cách tính cũ" — mốc ngày
   * cutover, lịch sử truy cập — phải biến mất khỏi những gì người học đọc được.
   */
  it('says nothing about the old access-day policy', () => {
    const wrapper = mount(StreakCalendar, {
      props: { currentStreak: 0, history: [], today: '2026-09-20' },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const text = wrapper.text()
    expect(text).not.toContain('cách tính mới')
    expect(text).not.toContain('lịch sử truy cập')
    expect(text).not.toContain('Lịch sử truy cập')
    expect(text).not.toContain('trước khi áp dụng')
    expect(text).not.toContain('20/9/2026')
    // Nhưng vẫn phải nói rõ thế nào là một ngày học.
    expect(text).toContain('hoàn thành bài tập')
  })

  /**
   * Lưới tự nó đã đủ rõ: một ô xanh duy nhất nổi bật trên nền xám. Bỏ hàng chú
   * thích ô màu để mắt chỉ còn phải xử lý đúng một thứ.
   */
  it('no longer renders the colour-swatch legend', () => {
    const wrapper = mount(StreakCalendar, {
      props: { currentStreak: 1, history: ['2026-09-20'], today: '2026-09-20' },
      global: { stubs: { RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] } } }
    })

    const text = wrapper.text()
    expect(text).not.toContain('Đã học')
    expect(text).not.toContain('Chưa học')
    // Chỉ còn đúng một câu hướng dẫn bên dưới lưới.
    expect(wrapper.findAll('p').filter(p => p.text().includes('hoàn thành bài tập'))).toHaveLength(1)
  })
})
