# audit-v16-full — Falsify-first ledger (mỗi hạng mục có cách test lại)

Quy tắc V4: **mọi kết luận v1–v15 là GIẢ THUYẾT**, phải kiểm chứng lại bằng đo trong phiên này.

| # | Giả thuyết (từ v15/AGENTS.md) | Cách phản chứng | Kỳ vọng nếu đúng | Kỳ vọng nếu sai |
|---|---|---|---|---|
| H1 | Baseline backend = 515/0/0/11 | chạy `mvnw.cmd test`, đếm từ log | 515/0 | số khác → finding |
| H2 | Baseline frontend = 178/1 (29 file) | `npx vitest run` | 178/1 | khác → finding |
| H3 | Build entry = 177.74 kB | `npx vite build` | 177.74 | khác → finding |
| H4 | Parity = `1470\|43738\|5\|118\|29\|4\|3\|12\|10` | `p16-parity.sql` | khớp | lệch → điều tra |
| H5 | 0 orphan trên mọi FK | query `LEFT JOIN ... WHERE fk IS NULL` | 0 | >0 → finding |
| H6 | Design: 0 Outfit/Jakarta, BVP duy nhất | grep + `design-v2.js` | 0 | >0 → finding |
| H7 | Token khớp prompt (accent/secondary/tertiary/quaternary) | `design-v2.js` token check | 0 mismatch | mismatch → finding |
| H8 | Contrast AA mọi route | contrast probe (composite) | 0 fail | >0 → finding |
| H9 | `prefers-reduced-motion` được tôn trọng | emulate media | có | không → finding |
| H10 | Không overflow 5 viewport | `design-v2.js` | 0 | >0 → finding |
| H11 | Mọi endpoint đều có probe | reconciliation inventory vs api-sweep | 100% | thiếu → bổ sung |
| H12 | Rate-limit bucket trỏ đúng route | burst thô | 429 đúng ngưỡng | không → finding (lớp F83) |
| H13 | Probe tự dọn (parity không đổi sau sweep) | parity before/after | khớp | lệch → finding (lớp F-15-09) |
| H14 | Timezone naive-VN nhất quán | đếm writer + so cột | nhất quán | lệch → finding |
| H15 | AI local hoạt động (Ollama) | call generate-vocab | 200/429 | 5xx → điều tra |
| H16 | `.agents/mcp_config.json` chứa secret đã commit | secret-scan | có secret | không → ghi nhận |
| H17 | `scripts/figma-export/node_modules` đã commit | `git ls-files` | có | không → bỏ |
| H18 | Sort `?sort=` hoạt động trên endpoint paged | so asc vs desc | khác nhau | giống → finding (lớp F-13-11) |
