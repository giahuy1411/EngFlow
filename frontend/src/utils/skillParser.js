export function parseQuizContent(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const clone = doc.body.cloneNode(true)
  const quizBoxes = clone.querySelectorAll('.qz_box')
  quizBoxes.forEach(el => el.remove())
  const staticHtml = clone.innerHTML

  const questions = []
  const quizContents = doc.querySelectorAll('.qz_content')
  quizContents.forEach((el, idx) => {
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

  return { staticHtml, questions }
}

export function parseReadingContent(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const staticHtml = doc.body.innerHTML

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

  return { staticHtml, questions }
}

export function parseListeningContent(html) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  const videoEl = doc.querySelector('#mediaplayer, #mediaplayer1')
  const videoHtml = videoEl ? videoEl.outerHTML : ''

  const clone = doc.body.cloneNode(true)
  const players = clone.querySelectorAll('#mediaplayer, #mediaplayer1')
  players.forEach(el => el.remove())
  const staticHtml = clone.innerHTML

  return { staticHtml, videoHtml }
}
