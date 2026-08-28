import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import paymentService from '@/services/paymentService'

export const usePremiumStore = defineStore('premium', () => {
  const isPremium = ref(false)
  const premiumExpiry = ref(null)
  const premiumStatus = ref(null)
  const loading = ref(false)

  async function checkStatus() {
    try {
      const data = await paymentService.getStatus()
      isPremium.value = data.isPremium
      premiumExpiry.value = data.premiumExpiry
      premiumStatus.value = data
    } catch {
      isPremium.value = false
      premiumExpiry.value = null
      premiumStatus.value = null
    }
  }

  async function createOrder(planType) {
    loading.value = true
    try {
      return await paymentService.createOrder(planType)
    } finally {
      loading.value = false
    }
  }

  return { isPremium, premiumExpiry, premiumStatus, loading, checkStatus, createOrder }
})
