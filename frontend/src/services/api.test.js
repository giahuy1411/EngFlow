import { beforeEach, describe, expect, it, vi } from 'vitest'

const authMock = vi.hoisted(() => ({ logout: vi.fn() }))
// audit-v13 F-13-20: `currentRoute` must be present so the interceptor can carry the
// destination. It defaults to undefined here to also prove the helper tolerates a
// router that does not expose it (older mock / defensive path).
const routerMock = vi.hoisted(() => ({ push: vi.fn(), currentRoute: undefined }))

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
    // Most cases below don't care about the current route; the F-13-20 block sets it.
    routerMock.currentRoute = undefined
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
    // audit-v13 F-13-20: push now carries the destination as a query object.
    expect(routerMock.push).toHaveBeenCalledWith({ path: '/login', query: {} })
  })

  it('tolerates non-object bodies', async () => {
    const err = { response: { status: 500, data: 'Internal Server Error' } }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
  })

  /**
   * audit-v8 Round 1 — F94. 20 endpoint (6 controller: AiVocabController,
   * GameController, DeckController, SrsController, SpeakingPromptController;
   * LessonStructureController đã gỡ ở audit-v15) trả lỗi theo shape CŨ `{"error": "..."}` chứ không
   * phải ProblemDetail. Đo sống bằng sweep/v8/p13_error_contract.js: 3/10 response
   * lỗi là LEGACY{error}. Interceptor chỉ sao `detail` → `message`, nên call-site
   * đọc `.detail`/`.message` (vd AiVocabGenerator.generate) hiện chuỗi generic
   * "Sinh từ thất bại" thay vì "topic không được để trống".
   * Chuẩn hoá luôn `error` tại cùng một chỗ.
   */
  it('backfills message and detail from the legacy {error} shape', async () => {
    const err = { response: { status: 400, data: { error: 'topic không được để trống' } } }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(err.response.data.message).toBe('topic không được để trống')
    expect(err.response.data.detail).toBe('topic không được để trống')
  })

  it('legacy {error} on a 403 does not log the user out', async () => {
    const err = { response: { status: 403, data: { error: 'Bạn không có quyền' } } }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(err.response.data.message).toBe('Bạn không có quyền')
    expect(authMock.logout).not.toHaveBeenCalled()
  })

  it('prefers an existing detail over legacy error', async () => {
    const err = {
      response: { status: 400, data: { detail: 'Thông báo chuẩn', error: 'thông báo cũ' } }
    }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(err.response.data.message).toBe('Thông báo chuẩn')
    expect(err.response.data.detail).toBe('Thông báo chuẩn')
  })

  it('ignores a non-string error field', async () => {
    const err = { response: { status: 400, data: { error: { nested: true } } } }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(err.response.data.message).toBeUndefined()
  })
})

/**
 * audit-v13 F-13-20 — phần còn hở. Khi phiên hết hạn GIỮA CHỪNG, interceptor phải
 * mang theo đích đến, nếu không người dùng mất trang họ đang làm. Đích được
 * Login.vue/Register.vue tiêu thụ qua safeRedirect() nên không cần validate ở đây.
 */
describe('api interceptor — F-13-20 carries the destination on auth bounce', () => {
  const expiredJwt = () => {
    const header = btoa(JSON.stringify({ alg: 'HS512' }))
    const payload = btoa(JSON.stringify({ sub: '2', exp: Math.floor(Date.now() / 1000) - 60 }))
    return `${header}.${payload}.sig`
  }

  function runRequestInterceptor() {
    const handlers = api.interceptors.request.handlers
    return handlers[handlers.length - 1].fulfilled({ url: '/api/x', headers: {} })
  }

  beforeEach(() => {
    authMock.logout.mockClear()
    routerMock.push.mockClear()
    localStorage.clear()
  })

  it('keeps the current route when an expired token is detected', async () => {
    routerMock.currentRoute = { value: { fullPath: '/decks/42?tab=quiz' } }
    localStorage.setItem('token', expiredJwt())
    await expect(runRequestInterceptor()).rejects.toThrow()
    expect(routerMock.push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/decks/42?tab=quiz' },
    })
  })

  it('does not self-reference when already on /login', async () => {
    routerMock.currentRoute = { value: { fullPath: '/login' } }
    localStorage.setItem('token', expiredJwt())
    await expect(runRequestInterceptor()).rejects.toThrow()
    expect(routerMock.push).toHaveBeenCalledWith({ path: '/login', query: {} })
  })

  it('does not self-reference when on /login with a query', async () => {
    routerMock.currentRoute = { value: { fullPath: '/login?redirect=%2Fprofile' } }
    localStorage.setItem('token', expiredJwt())
    await expect(runRequestInterceptor()).rejects.toThrow()
    expect(routerMock.push).toHaveBeenCalledWith({ path: '/login', query: {} })
  })

  it('does not throw when the router exposes no currentRoute', async () => {
    routerMock.currentRoute = undefined
    localStorage.setItem('token', expiredJwt())
    await expect(runRequestInterceptor()).rejects.toThrow()
    expect(routerMock.push).toHaveBeenCalledWith({ path: '/login', query: {} })
  })

  it('carries the destination on a mid-session 401 as well', async () => {
    routerMock.currentRoute = { value: { fullPath: '/speaking/7/record' } }
    const err = {
      response: {
        status: 401,
        data: { status: 401, message: 'Token đã hết hạn. Vui lòng đăng nhập lại.' },
      },
    }
    await expect(runErrorInterceptor(err)).rejects.toBe(err)
    expect(routerMock.push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/speaking/7/record' },
    })
  })
})
