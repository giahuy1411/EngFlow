const VIDEO_SELECTORS = '#mediaplayer, #mediaplayer1, video, iframe[src*="youtube"], iframe[src*="mp4"], embed[src*="mp4"], object[type*="video"]'

function extractVideoHtml(doc) {
  const el = doc.querySelector(VIDEO_SELECTORS)
  if (!el) return ''
  if (el.tagName === 'VIDEO' || el.tagName === 'AUDIO') {
    el.querySelectorAll('source[src]').forEach(s => {
      let src = s.getAttribute('src') || ''
      if (src.startsWith('//')) src = 'https:' + src
      s.setAttribute('src', src)
    })
    let src = el.getAttribute('src') || ''
    if (src.startsWith('//')) src = 'https:' + src
    el.setAttribute('src', src)
    el.setAttribute('controls', '')
    el.setAttribute('style', 'width:100%;max-height:400px')
  }
  return el.outerHTML
}

function removeAll(container, selectors) {
  container.querySelectorAll(selectors).forEach(el => el.remove())
}

export function parseQuizContent(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const videoHtml = extractVideoHtml(doc)

  const clone = doc.body.cloneNode(true)
  removeAll(clone, VIDEO_SELECTORS + ', .qz_box')
  const staticHtml = clone.innerHTML

  const questions = []
  doc.querySelectorAll('.qz_content').forEach((el, idx) => {
    const titleEl = el.querySelector('.qz_st1')
    const options = []
    el.querySelectorAll('.qz_st2_item.qz_radio').forEach((optEl, oidx) => {
      const textEl = optEl.querySelector('.qz_st2_text')
      options.push({ index: oidx, text: textEl ? textEl.innerHTML : '' })
    })
    if (options.length > 0) {
      questions.push({
        index: idx,
        title: titleEl?.innerHTML || `Question ${idx + 1}`,
        options
      })
    }
  })

  return { staticHtml, videoHtml, questions }
}

export function parseListeningContent(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const videoHtml = extractVideoHtml(doc)

  const clone = doc.body.cloneNode(true)
  removeAll(clone, VIDEO_SELECTORS + ', .qz_box')
  const staticHtml = clone.innerHTML

  return { staticHtml, videoHtml }
}

export function parseReadingContent(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const videoHtml = extractVideoHtml(doc)

  const clone = doc.body.cloneNode(true)
  removeAll(clone, VIDEO_SELECTORS)
  const staticHtml = clone.innerHTML

  const questions = []
  doc.querySelectorAll('.tanc_read_item_ques').forEach((item, idx) => {
    const options = []
    item.querySelectorAll('.tanc_read_radio_r').forEach((optEl, oidx) => {
      options.push({ index: oidx, text: optEl.textContent.trim() })
    })
    const textInputs = []
    item.querySelectorAll('.tanc_part_blank').forEach(inputEl => {
      const correctValue = inputEl.value || ''
      inputEl.value = ''
      textInputs.push({ correctText: correctValue })
    })
    questions.push({ index: idx, options, textInputs })
  })

  return { staticHtml, videoHtml, questions }
}
