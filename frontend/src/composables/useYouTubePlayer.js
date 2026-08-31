import { onUnmounted, ref } from 'vue'

let loaderPromise = null

function loadYouTubeApi() {
  if (window.YT?.Player) return Promise.resolve(window.YT)
  if (loaderPromise) return loaderPromise
  loaderPromise = new Promise((resolve, reject) => {
    // Reuse the tag if something already requested the API, otherwise the
    // browser downloads it twice and two widget ids collide.
    let tag = document.querySelector('script[src*="youtube.com/iframe_api"]')
    if (!tag) {
      tag = document.createElement('script')
      tag.src = 'https://www.youtube.com/iframe_api'
      tag.onerror = () => { loaderPromise = null; reject(new Error('Không tải được YouTube player')) }
      document.head.appendChild(tag)
    }
    const previous = window.onYouTubeIframeAPIReady
    window.onYouTubeIframeAPIReady = () => {
      previous?.()
      if (window.YT?.Player) resolve(window.YT)
      else { loaderPromise = null; reject(new Error('YouTube player không sẵn sàng')) }
    }
  })
  return loaderPromise
}

let mountSeq = 0

/** Map YouTube IFrame API error codes to a message a Vietnamese learner can act on. */
function describePlayerError(code) {
  switch (code) {
    case 2: return 'Link video không hợp lệ.'
    case 5:
    case 100: return 'Video không còn tồn tại hoặc để riêng tư.'
    case 101:
    case 150: return 'Chủ kênh đã tắt nhúng video này. Chọn video khác cho bài học.'
    case 153: return 'Trình duyệt chặn nhúng video (thiếu thông tin giới thiệu). Thử mở bằng Chrome thường.'
    default: return `Video không phát được (lỗi ${code ?? 'không rõ'}).`
  }
}

/**
 * Thin wrapper around the YouTube IFrame Player API.
 *
 * <p>The caller passes a Vue template ref instead of an element id: the player
 * host lives inside a `v-else-if` branch, so it only exists after lesson data
 * arrives, and querying by id from module scope is timing-fragile.</p>
 *
 * <p>Only embeds the official player — never downloads or re-hosts video
 * (YouTube API Services Developer Policies, "Audiovisual Content").</p>
 */
export function useYouTubePlayer() {
  const player = ref(null)
  const ready = ref(false)
  const error = ref('')
  const currentTime = ref(0)
  let pollId = null
  let destroyed = false
  let creating = false

  async function create(hostEl, videoId, startSeconds = 0) {
    if (creating || player.value) return
    creating = true
    try {
      if (!hostEl) { error.value = 'Không tìm thấy vùng chứa player'; return }
      const YT = await loadYouTubeApi()
      if (destroyed) return
      // The IFrame API replaces its target element with an iframe, so hand it a
      // dedicated child div and keep the Vue-rendered host intact.
      hostEl.innerHTML = ''
      const mount = document.createElement('div')
      mount.className = 'w-full h-full'
      // The API resolves a string id via getElementById, which can pick up a
      // stale node; a freshly unique id plus the live element avoids that.
      mount.id = `engflow-yt-mount-${++mountSeq}`
      hostEl.appendChild(mount)
      player.value = new YT.Player(mount.id, {
        videoId,
        startSeconds,
        playerVars: { origin: window.location.origin, rel: 0, modestbranding: 1 },
        events: {
          onReady: () => {
            if (destroyed) return
            ready.value = true
            error.value = ''
            pollId = window.setInterval(() => {
              if (player.value?.getCurrentTime) currentTime.value = player.value.getCurrentTime()
            }, 250)
          },
          onError: (event) => {
            // A stale widget can fire an error after a good player is up; never
            // let it overwrite a working session.
            if (ready.value) return
            error.value = describePlayerError(event?.data)
          }
        }
      })
    } catch (cause) {
      error.value = cause.message || 'Lỗi tải player'
    } finally {
      creating = false
    }
  }

  function seekTo(seconds, play = true) {
    if (!player.value?.seekTo) return
    player.value.seekTo(seconds, true)
    if (play) player.value.playVideo?.()
  }

  function play() { player.value?.playVideo?.() }
  function pause() { player.value?.pauseVideo?.() }

  function destroy() {
    destroyed = true
    window.clearInterval(pollId)
    try { player.value?.destroy?.() } catch { /* already gone */ }
    player.value = null
    ready.value = false
  }

  onUnmounted(destroy)

  return { player, ready, error, currentTime, create, seekTo, play, pause, destroy }
}
