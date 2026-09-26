# audit-v16-full — Phase 2: secret-scan

**Mục tiêu:** phát hiện secret/key/PII lọt vào repo. **Không in giá trị secret** (R7) — chỉ nêu vị trí.

## Kết quả

| Ứng viên | Tracked? | Gitignored? | Từng commit? | Kết luận |
|---|---|---|---|---|
| `.agents/mcp_config.json` (Figma OAuth `clientSecret`) | **KHÔNG** | **CÓ** (`.gitignore:63-64`) | **CHƯA** (`git log --all` rỗng) | **An toàn** — không rò rỉ |
| `.env` | KHÔNG | CÓ (`.gitignore:30-31`) | — | An toàn |
| `.env.example` | CÓ (chủ ý) | — | — | Chỉ placeholder (`CHANGE_ME`, `your_...`, `demo`) — an toàn |
| `frontend/.env.example` | CÓ (chủ ý) | — | — | Placeholder — an toàn |
| `node_modules/**` | **KHÔNG** (0 file) | CÓ (`.gitignore:35`) | — | An toàn |
| `.playwright-mcp/**` | KHÔNG | CÓ (`.gitignore:70`) | — | An toàn |
| Backup `.bak` (PII học viên) | KHÔNG | giữ ngoài repo | — | An toàn |

## Bác bỏ một kết luận SAI (kỷ luật V4/R1)

Một cuộc khảo sát trước đó **kết luận sai** rằng `.agents/mcp_config.json` là "secret đã commit".
**Đo lại dứt khoát:**
```
$ git ls-files --error-unmatch .agents/mcp_config.json
error: pathspec ... did not match any file(s) known to git     → NOT TRACKED
$ git check-ignore -v .agents/mcp_config.json
.gitignore:64:.agents/mcp_config.json                          → IGNORED
$ git log --oneline --all -- .agents/mcp_config.json           → (rỗng: chưa từng commit)
```
→ **Không phải finding bảo mật.** Ghi lại để không lặp lại kết luận chưa kiểm chứng.

## Khuyến nghị (phòng ngừa, không khẩn)

- `.env.example` hiện dùng placeholder tốt — giữ nguyên.
- Không cần thay đổi code bảo mật nào từ vòng này.
- (Nếu muốn chắc chắn tuyệt đối) rotate Figma OAuth secret định kỳ theo chính sách — nhưng **không do repo rò rỉ**.
