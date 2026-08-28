import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppInput from '../AppInput.vue'

describe('AppInput', () => {
  it('renders input', () => {
    const wrapper = mount(AppInput, { props: { id: 'name' } })
    expect(wrapper.find('input').exists()).toBe(true)
  })

  it('binds v-model', async () => {
    const wrapper = mount(AppInput, {
      props: { id: 'name', modelValue: '' },
      attachTo: document.body,
    })
    await wrapper.find('input').setValue('Anna')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0][0]).toBe('Anna')
  })

  it('marks invalid', () => {
    const wrapper = mount(AppInput, { props: { id: 'email', invalid: true } })
    expect(wrapper.find('input').attributes('aria-invalid')).toBe('true')
  })

  it('forwards disabled', () => {
    const wrapper = mount(AppInput, { props: { id: 'email', disabled: true } })
    expect(wrapper.find('input').attributes('disabled')).toBeDefined()
  })
})
