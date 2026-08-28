import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import AppAlert from '../AppAlert.vue'
import AppToast from '../AppToast.vue'

describe('AppAlert', () => {
  it('renders title and message', () => {
    const wrapper = mount(AppAlert, {
      props: { title: 'Lỗi', message: 'Vui lòng thử lại', type: 'error' },
    })
    expect(wrapper.text()).toContain('Lỗi')
    expect(wrapper.text()).toContain('Vui lòng thử lại')
  })

  it('applies type class', () => {
    const wrapper = mount(AppAlert, {
      props: { type: 'warning' },
    })
    expect(wrapper.classes()).toContain('app-alert--warning')
  })

  it('dismisses when dismissible', async () => {
    const wrapper = mount(AppAlert, {
      props: { dismissible: true },
      slots: { default: 'Đóng thử' },
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('dismiss')).toBeTruthy()
  })

  it('hides close button when not dismissible', () => {
    const wrapper = mount(AppAlert, { props: { dismissible: false } })
    expect(wrapper.find('[aria-label="Đóng"]').exists()).toBe(false)
  })

  it('uses aria-live for accessibility', () => {
    const wrapper = mount(AppAlert, { props: { type: 'info' } })
    expect(wrapper.find('[aria-live]').exists()).toBe(true)
  })
})

describe('AppToast', () => {
  it('renders message with dismiss button', () => {
    const wrapper = mount(AppToast, {
      props: { message: 'Đã lưu', type: 'success', dismissible: true },
    })
    expect(wrapper.text()).toContain('Đã lưu')
  })

  it('emits close with message id', async () => {
    const wrapper = mount(AppToast, {
      props: { message: 'Hello', type: 'info', dismissible: true, id: 42 },
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
    expect(wrapper.emitted('close')[0][0]).toBe(42)
  })

  it('exposes accessible name on close button', () => {
    const wrapper = mount(AppToast, {
      props: { message: 'X', dismissible: true },
    })
    expect(wrapper.find('button[aria-label]').exists()).toBe(true)
  })
})
