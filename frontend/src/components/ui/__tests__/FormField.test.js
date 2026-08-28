import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import FormField from '../FormField.vue'
import AppInput from '../AppInput.vue'

describe('FormField', () => {
  it('renders label associated with control id', () => {
    const wrapper = mount(FormField, {
      props: { id: 'email', label: 'Email' },
      slots: { default: '<AppInput id="email" />' },
      global: { components: { AppInput } },
    })
    expect(wrapper.find('label').attributes('for')).toBe('email')
    expect(wrapper.text()).toContain('Email')
  })

  it('renders hint and wires aria-describedby', () => {
    const wrapper = mount(FormField, {
      props: { id: 'email', label: 'Email', hint: 'Không chia sẻ' },
      slots: { default: '<AppInput id="email" />' },
      global: { components: { AppInput } },
    })
    expect(wrapper.text()).toContain('Không chia sẻ')
    const hint = wrapper.find('[id$="-hint"]')
    expect(hint.exists()).toBe(true)
  })

  it('renders error and exposes invalid state', () => {
    const wrapper = mount(FormField, {
      props: { id: 'email', label: 'Email', error: 'Sai định dạng' },
      slots: { default: '<AppInput id="email" />' },
      global: { components: { AppInput } },
    })
    expect(wrapper.text()).toContain('Sai định dạng')
  })

  it('renders required marker', () => {
    const wrapper = mount(FormField, {
      props: { id: 'name', label: 'Tên', required: true },
      slots: { default: '<AppInput id="name" />' },
      global: { components: { AppInput } },
    })
    expect(wrapper.find('[aria-hidden="true"]').exists()).toBe(true)
  })
})
