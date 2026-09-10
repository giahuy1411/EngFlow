import { beforeEach, describe, expect, it, vi } from 'vitest'

const authMock = vi.hoisted(() => ({ logout: vi.fn() }))
const routerMock = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('@/router', () => ({ default: routerMock }))
vi.mock('@/store/modules/auth', () => ({ useAuthStore: () => authMock }))

import api from '@/services/api'

/**
 * Backend trả lỗi theo RFC 7807 (ProblemDetail): thông báo tiếng Việt nằm ở
 * trường `detail`, trong khi hàng chục component đọc `err.response.data.message`
 * (vd quota 403 "Bạn đã dùng hết 5 lượt… hôm nay"). Response interceptor phải
 * chuẩn hoá về MỘT chỗ: sao `detail` sang `message` để UI hiện đúng thông báo,
 * thay vì sửa từng call-site.
 */
function runErrorInterceptor(error) {
  const handlers = api.interceptors.response.handlers
  const rejected = handlers[handlers.length - 1].rejected
  return rejected(error)
}

describe('api response interceptor — ProblemDetail normalization', () => {
  beforeEach(() => {
    authMock.logout.mockClear()
    routerMock.push.mockClear()
  })

  it('backfills message from ProblemDetail detail for 400 validation', async () => {
    const err = {
      response: {
        status: 400,
        data: { type: 'about:blank', title: 'Validation Failed', status: 400, detail: 'Dữ liệu đầu vào không hợp lệ' }
      }
    }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(err.response.data.message).toBe('Dữ liệu đầu vào không hợp lệ')
    expect(authMock.logout).not.toHaveBeenCalled()
  })

  it('backfills the daily AI quota 403 without logging the user out', async () => {
    const err = {
      response: {
        status: 403,
        data: { title: 'AI Generation Quota Exceeded', status: 403, detail: 'Bạn đã dùng hết 5 lượt sinh từ vựng AI miễn phí hôm nay.' }
      }
    }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(err.response.data.message).toContain('5 lượt')
    // Hết quota KHÔNG phải phiên hết hạn — không được đá về /login.
    expect(authMock.logout).not.toHaveBeenCalled()
  })

  it('keeps an existing message untouched (JWT filter shape)', async () => {
    const err = { response: { status: 401, data: { status: 401, message: 'Token đã hết hạn. Vui lòng đăng nhập lại.', errors: null } } }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(err.response.data.message).toBe('Token đã hết hạn. Vui lòng đăng nhập lại.')
    // 401 'hết hạn' vẫn phải logout như thiết kế F7-BUG02.
    expect(authMock.logout).toHaveBeenCalled()
    expect(routerMock.push).toHaveBeenCalledWith('/login')
  })

  it('tolerates non-object bodies', async () => {
    const err = { response: { status: 500, data: 'Internal Server Error' } }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
  })
})
