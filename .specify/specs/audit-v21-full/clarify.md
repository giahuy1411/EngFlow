# audit-v21-full — clarify

Câu hỏi gỡ mơ hồ + quyết định (đã hỏi người dùng ở bước plan).

| # | Điểm mơ hồ | Quyết định |
|---|---|---|
| Q1 | Vòng 21 hay làm lại từ đầu? | **Vòng 21** — font + design system đã xong; verify sâu + lấp khoảng trống. |
| Q2 | Độ sâu audit? | **Toàn diện (như v18–v20) + chiều mới** (security, Lighthouse/CWV, bundle, index/query-plan), **đi sâu hơn vào UI/UX 2 MCP**. |
| Q3 | "Comment toàn bộ code" — làm lại hay lấp chỗ thiếu? | Người dùng chọn **"comment lại từ đầu NHƯNG từ từ từng phần nhỏ, test sau mỗi phần"**, và là **việc #1**. → Hiện thực: bù **77 file thiếu** (44 Java + 33 FE) theo lô nhỏ, gate full-test mỗi lô. Phần đã có comment tốt (152 Java + 85 FE) **giữ nguyên** để tránh rủi ro F-20-06 (không viết lại thứ đang đúng). |
| Q4 | Git? | **Giữ nhánh `audit-v15-full`, commit tại chỗ, không push, không PR** (thông lệ v16–v20). |
| Q5 | `taskstoissues`? | **Không** — `gh` chưa đăng nhập; repo có remote nhưng người dùng không yêu cầu issue. |
| Q6 | Xoá `.env.bak-*`? | **Không** — gitignored, cần rollback, chứa secret thật. |
| Q7 | Xoá `scripts/figma-export/node_modules`? | **Không** — `cls-probe.js:36` cố ý resolve `playwright-core` từ đó (AGENTS.md:75). |
| Q8 | Thêm polka/diagonal-stripe? | **Không** — OPTIONAL, 0 sai lệch đo được (hiến pháp: không feature không yêu cầu). |

## Ràng buộc phát hiện thêm

- `sweep/v12/api-sweep.js` có **default audit round thứ hai** — phải sửa **cả hai** nơi (F-20-02b).
- `docs/demo-engflow-4-chuc-nang.md` có **167 trích dẫn `File.java:số-dòng`** — comment đẩy dòng phải
  chạy lại `doc_citation_remap.py`.
- Backend test đọc số từ **run log**, KHÔNG đọc XML `target/surefire-reports` (XML stale từng làm sai +8).
