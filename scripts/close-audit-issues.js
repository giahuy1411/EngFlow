const { execFileSync } = require('child_process');

function getGithubToken() {
  // execFileSync with an argument array — no shell, nothing to inject.
  const out = execFileSync('git', ['credential', 'fill'], {
    input: 'protocol=https\nhost=github.com\n\n',
    encoding: 'utf8'
  });
  const line = out.split('\n').find(l => l.startsWith('password='));
  if (!line) throw new Error('no stored github credential');
  return line.slice('password='.length).trim();
}

const REPO = 'giahuy1411/EngFlow';
const COMMIT = '0702698';

const COMMENTS = {
  5: `**Đã fix — audit-v12 Phase 9 (F147), verify lại Phase 10.**

Hướng đã chốt: \`vocabulary\` là **từ điển chung** (0 cột \`owner\`), nên quyền sở hữu nằm ở \`decks.owner_id\` + \`deck_words\`. Lỗi là **đường ghi của student bỏ qua tầng sở hữu**, không phải schema.

**Fix** (\`service/VocabularyService.java\` mới): \`createScoped(request, deckId, userId, isAdmin)\`
- non-admin **bắt buộc** có \`deckId\` → nếu thiếu: **400**
- gọi \`deckService.addWordToDeck(...)\` **trong cùng \`@Transactional\`** → atomic
- dedupe theo \`word\` chưa gắn lesson
- \`PUT\`/\`DELETE\` vẫn admin-only (không đổi)

**Đo lại (probe live, 3 role):**

| Probe | Kết quả |
|---|---|
| Student **không** \`deckId\` | **400** \`"Cần chọn bộ từ để lưu từ vựng…"\` |
| Student có deckId của mình | **200**; vocab row **và** link \`deck_words\` cùng transaction |
| Student gắn deck **người khác** (IDOR) | **400** + **row đã rollback** (0 leak) |
| Lưu cùng từ 2 lần | **1** row, **2** link (dedupe) |

Regression: \`VocabularyServiceTest\` (6 test) + 5 assert trong \`sweep/v12/api-sweep.js\` — tất cả PASS.

Đóng issue. Chi tiết: \`.specify/specs/audit-v12-full/evidence/phase-9-open-items.md\` §F147.`,

  6: `**Đã fix đủ — audit-v12 Phase 9 (thuật toán) + Phase 10 (UI).**

Issue nêu 2 việc: (a) thuật toán SRS bị phân mảnh, (b) không UI nào gọi. **Cả hai xong.**

**(a) Hợp nhất thuật toán (Phase 9, F148).** Trước đây có **2 thuật toán ghi cùng bảng** \`user_vocabulary_progress\`: \`SrsService.reviewWord\` (SM-2 đầy đủ) và \`FlashcardService\` (bảng ngày cố định 1/3/7/14, **không** ghi \`ease_factor\`/\`repetitions\`). \`FlashcardService\` nay uỷ quyền \`srsService.reviewWord\` → \`SrsService\` là **nguồn sự thật duy nhất**. Kèm sửa 3 nút Lại/Tiếp/Dễ gửi **quality 1/4/5** (trước đây "Dễ" và "Tiếp theo" gửi request **giống hệt nhau**).

**(b) Dựng UI "ôn từ đến hạn" (Phase 10, Item A).** \`GET /api/srs/due/{deckId}\` nay có caller thật:
- \`frontend/src/services/srsService.js\` — map \`vocabId/definitionVi/pronunciation/exampleSentence\` → shape thẻ
- \`frontend/src/views/luyentu/DueReview.vue\` — 4 trạng thái: loading / rỗng / thẻ / hoàn thành
- route \`/decks/:id/review\` + nút "Ôn từ đến hạn" ở \`DeckDetail.vue\`
- \`grep -rn "api/srs" frontend/src\` → **có hit** (trước: 0)

**Đo lại (live, đăng nhập \`user@gmail.com\`):** deck 10006 → "7 từ đến hạn"; bấm Lại/Tiếp/Dễ → \`POST /api/srs/review\` body \`{"vocabId":…,"quality":1/4/5}\`; ôn hết → "Đã ôn xong 7 từ!"; mở lại → "Không có từ nào đến hạn" (**E2E thật**: server đã dời lịch 7 từ đó). Lighthouse route mới: Accessibility **100**.

**Phát hiện thêm khi làm việc này — F151 (đã fix luôn):** \`GET /api/srs/due/{deckId}\` **không kiểm quyền sở hữu** → student đọc được **nội dung** deck private của người khác (\`200\` + \`word\`/\`definitionVi\`/\`exampleSentence\`), trong khi \`GET /api/decks/{id}\` đúng đắn từ chối \`400\`. Đã uỷ quyền \`DeckService.getDeckById(deckId, userId)\`; probe 9/9 PASS. Không tạo issue riêng vì đóng luôn trong cùng commit.

Đóng issue. Chi tiết: \`evidence/phase-9-open-items.md\` §F148 + \`evidence/phase-10-f151-ui-cleanup.md\`.`,

  7: `**Đã fix — audit-v12 Phase 9 (F150).**

**Gốc rễ (đo, không suy đoán):** shell \`min-h-screen\` cho footer first-paint ở **y≈827** — *trong* viewport 867px. Chunk Home (lazy) về sau làm \`<main>\` giãn lên ~3351px → footer bị đẩy ra y≈3415. Khối 96px **đang thấy** rời màn hình = **CLS 0.104**.

**Một lần thử SAI đã bị revert (ghi để không lặp):** \`calc(100vh - 64px - 96px)\` = **707px** → footer ở \`64+707=771\` < 867 ⇒ **vẫn trong màn hình** ⇒ chỉ 0.098. Giá trị này **sai**.

**Fix (giá trị ĐÚNG)** — \`frontend/src/assets/app-layout.css\`:
\`\`\`css
#main-content { min-height: 100vh; }   /* footer first-paint ở 64+867=931 > 867 = ngoài màn hình */
\`\`\`

**Đo lại (Playwright \`PerformanceObserver\`, production build, 5 lần):**
\`\`\`
trước:  0.104 / 0.111 / 0.104 / 0.104
sau:    0.0001 / 0.00008 / 0.0001 / 0.00005 / 0.00013
\`\`\`
Lighthouse xác nhận độc lập (chrome-devtools MCP, \`/\`): \`cumulative-layout-shift\` **0.103 → 0.001** (score 1); Accessibility vẫn **100**.

Đóng issue. Chi tiết: \`evidence/phase-9-open-items.md\` §F150.`,

  8: `**Đã fix — audit-v12 Phase 9 (C6).**

**Xoá:**
- \`pom.xml\`: \`com.microsoft.cognitiveservices.speech:client-sdk:1.51.1\` (~15 MB)
- \`application.properties\`: \`azure.speech.key\` / \`azure.speech.region\` (không class nào đọc)
- \`PronunciationAssessmentResult.java\` (record chết)

**⚠️ Một phụ thuộc ẨN bị lộ ra — và đã sửa đúng cách (không rollback).** Xoá Azure xong, **1 test đỏ**:
\`\`\`
SpeakingSubmissionResponseTest.fromExposesOnlySafeUserSummary
  Java 8 date/time type java.time.LocalDateTime not supported by default
\`\`\`
Nguyên nhân: Azure SDK đang **cung cấp ngầm \`jackson-datatype-jsr310\`** cho classpath. Xoá nó ⇒ mất \`JavaTimeModule\`. Đã **khai báo tường minh** dependency đó trong \`pom.xml\` để app không phụ thuộc một thư viện không dùng.

Đây chính là lý do quy trình bắt buộc chạy **full test** sau khi xoá dependency — nếu chỉ \`mvn -o compile\` thì lỗi này lọt.

**Đo lại:**

| Kiểm | Kết quả |
|---|---|
| Azure trong dependency tree | **0** |
| Class \`cognitiveservices\` trong JAR | **0** |
| \`jackson-datatype-jsr310\` trong tree | \`2.21.2:compile\` ✓ |
| Backend suite | **496 run / 0 fail / 0 error / 11 skipped — BUILD SUCCESS** |

Đóng issue. Chi tiết: \`evidence/phase-9-open-items.md\` §C6.`
};

async function main() {
  const token = getGithubToken();
  const h = {
    'Authorization': 'token ' + token,
    'User-Agent': 'engflow-audit',
    'Accept': 'application/vnd.github+json',
    'Content-Type': 'application/json; charset=utf-8'
  };
  for (const n of Object.keys(COMMENTS).map(Number)) {
    const body = COMMENTS[n] + `\n\n---\nCommit: \`${COMMIT}\` · Branch: \`audit-streak-review\``;
    const c = await fetch(`https://api.github.com/repos/${REPO}/issues/${n}/comments`, {
      method: 'POST', headers: h, body: JSON.stringify({ body })
    });
    const cd = await c.json();
    console.log(`#${n} comment: ${c.status} ${cd.html_url || JSON.stringify(cd).slice(0, 160)}`);

    const p = await fetch(`https://api.github.com/repos/${REPO}/issues/${n}`, {
      method: 'PATCH', headers: h, body: JSON.stringify({ state: 'closed', state_reason: 'completed' })
    });
    const pd = await p.json();
    console.log(`#${n} close:   ${p.status} state=${pd.state} reason=${pd.state_reason}`);
    await new Promise(r => setTimeout(r, 900));
  }
}
main().catch(e => { console.error('FATAL:', e.message); process.exit(1); });
