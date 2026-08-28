import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppSkeleton from '../AppSkeleton.vue'
import AppEmptyState from '../AppEmptyState.vue'
import AppErrorState from '../AppErrorState.vue'

describe('AppSkeleton', () => {
  it('renders skeleton block', () => {
    const wrapper = mount(AppSkeleton)
    expect(wrapper.find('.app-skeleton').exists()).toBe(true)
  })

  it('applies width and height', () => {
    const wrapper = mount(AppSkeleton, {
      props: { width: '100%', height: '2rem' },
    })
    expect(wrapper.attributes('style')).toContain('width')
    expect(wrapper.attributes('style')).toContain('height')
  })
})

describe('AppEmptyState', () => {
  it('renders title and description', () => {
    const wrapper = mount(AppEmptyState, {
      props: { title: 'Chưa có dữ liệu', description: 'Hãy tạo bài học đầu tiên' },
    })
    expect(wrapper.text()).toContain('Chưa có dữ liệu')
    expect(wrapper.text()).toContain('Hãy tạo bài học đầu tiên')
  })

  it('renders action slot', () => {
    const wrapper = mount(AppEmptyState, {
      slots: { action: '<button>Tạo mới</button>' },
    })
    expect(wrapper.find('button').text()).toBe('Tạo mới')
  })
})

describe('AppErrorState', () => {
  it('renders title and description', () => {
    const wrapper = mount(AppErrorState, {
      props: { title: 'Có lỗi', description: 'Không thể tải dữ liệu' },
    })
    expect(wrapper.text()).toContain('Có lỗi')
    expect(wrapper.text()).toContain('Không thể tải dữ liệu')
  })

  it('renders action slot', () => {
    const wrapper = mount(AppErrorState, {
      slots: { action: '<button>Thử lại</button>' },
    })
    expect(wrapper.find('button').text()).toBe('Thử lại')
  })
})
