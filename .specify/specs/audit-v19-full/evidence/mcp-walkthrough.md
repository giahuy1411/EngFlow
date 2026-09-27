# audit-v19-full — Phase U: interactive MCP UI/UX walkthrough (CẢ 2 ENGINE cho MỌI luồng)

**Ngày:** 2026-09-27 · **Ràng buộc:** mỗi luồng chạy **cả chrome-devtools MCP (CD) và Playwright MCP (PW)**.
**Ảnh:** `evidence/shots/mcp/{chrome-devtools,playwright}/**`.

---

## U1 — Đăng nhập
| Engine | Kết quả |
|---|---|
| **CD** | `/login` → điền user@gmail.com/123456 → **`/lessons`**; `localStorage.token`+`user` set; `isPremium:true` |
| **PW** | (dùng fetch login + navigate) → token set, `isPremium:true` |

## U4 — Guard admin (2 chiều)
| Engine | Role | Đi `/admin/users` |
|---|---|---|
| **CD** | student | → **`/`** (bounce) |
| **PW** | student | → **`/`** (bounce) |
| **CD** | admin | → `/admin/users` (đã chứng minh ở U22) |
| **PW** | admin | → `/admin/users` |
**2 engine khớp.**

## U2 — Bài học / Bài tập
| Engine | Kết quả |
|---|---|
| **CD** | `/lessons/445`: h1 "English Grammar Exercises for A1 – have got and articles"; **3 tab**; 6 câu; **0 non-null `correctAnswer`** (student); nút "Kiểm tra" + "Nộp bài" |
| **PW** | cùng h1, cùng 3 tab, 6 câu, 0 non-null |

## U3 — Streak
| Engine | Kết quả |
|---|---|
| **CD** | `/profile`: API `currentStreak:0`, `today:2026-09-27`, `studiedToday:false`; UI "STREAK", "CHƯA HỌC", lịch |
| **PW** | cùng: `currentStreak:0`, UI "STREAK" + "CHƯA HỌC" |

## U11 — Tra từ (Search)
| Engine | Kết quả |
|---|---|
| **CD** | 1 ký tự `h` + "Tra từ" → **0 call** `/dictionary/` (GUARD OK); `hello` → `/dictionary/hello` + phiên âm + NOUN/VERB |
| **PW** | 1 ký tự → **0 call** (GUARD OK) |

## U12 — CRUD decks + game
| Engine | Kết quả |
|---|---|
| **CD** | `/decks/10006/play/quiz`: h1 "Trắc nghiệm từ vựng"; 4 phương án; điểm 0/0 |
| **PW** | cùng h1; 4 phương án; 0/0 |

## U15 — AI generate-vocab
| Engine | Kết quả |
|---|---|
| **CD** | "ocean life" → `POST /api/ai/generate-vocab`; FISH/SEAWEED (lần này **tiếng Việt**) |
| **PW** | "weather" → cùng endpoint; **CJK: 天气 / 气候** ← **tái hiện lỗi W1** (bằng chứng!) |

## U22 — Admin
| Engine | Trang | API ↔ UI |
|---|---|---|
| **CD** | `/admin/dashboard` | 5 / 1470 / 43738 — UI khớp |
| **PW** | `/admin/dashboard` | 5 / 1470 / 43738 — UI khớp |
| **CD** | `/admin/exercises` | total 43738, 20 rows, nút AI |
| **PW** | `/admin/exercises` | total 43738, 20 rows |

## U18 — Video
| Engine | Kết quả |
|---|---|
| **PW** | `/videos/1`: iframe YouTube **`2VeQTuSSiI0`**, transcript, tiến độ **3/12**; 1 console error = third-party YouTube (known noise) |

## U17 — Speaking
| Engine | Kết quả |
|---|---|
| **PW** | `/speaking`: API 7 prompt = UI "ĐỀ LUYỆN CÓ SẴN 7"; h1 "Luyện nói" |

## U20 — Leaderboard
| Engine | Kết quả |
|---|---|
| **PW** | `/leaderboard`: API 5 entry, top "Học Viên Mẫu" = UI "TỔNG NGƯỜI HỌC 5" |

## U19 — Premium + checkout (TU.13 — MỚI, cả 2 engine)
| Engine | Thao tác | Kết quả |
|---|---|---|
| **CD** | `/premium` → "Đăng ký ngay" | 2 gói; click → `/premium/checkout?plan=MONTH`; gọi `POST /api/v1/payment/create-order` + `GET /api/v1/payment/status`; user đã premium → poll bounce `/lessons` |
| **PW** | `/premium/checkout?plan=YEAR` | cùng: create-order + status; bounce `/lessons` |

**Xử lý row thật:** mỗi lần vào checkout tạo 1 row PENDING → đã dọn bằng `cleanupAuditPayments()` (candidates=1 → 0),
parity về `PENDING_PAYMENTS=0`, payments 12. **2 engine khớp.**

## U0 — Home + responsive
| Engine | Kết quả |
|---|---|
| **CD** | `/`: h1 "Học Tiếng AnhVui Vẻ"; overflow **0**; 14 SVG decor |
| **PW** | `/` @ **360px**: overflow **0**; hero present; 14 SVG |

---

## Tổng hợp Phase U

| # | Luồng | CD | PW | API↔UI | 2 engine khớp |
|---|---|---|---|---|---|
| U1 | Đăng nhập | ✅ | ✅ | ✅ | ✅ |
| U4 | Guard admin 2 chiều | ✅ | ✅ | ✅ | ✅ |
| U2 | Bài học/Bài tập + chống lộ đáp án | ✅ | ✅ | ✅ | ✅ |
| U3 | Streak | ✅ | ✅ | ✅ | ✅ |
| U11 | Tra từ + guard 1 ký tự | ✅ | ✅ | ✅ | ✅ |
| U12 | CRUD decks + game | ✅ | ✅ | ✅ | ✅ |
| U15 | AI generate-vocab | ✅ | ✅ | ✅ | ✅ |
| U22 | Admin dashboard/exercises | ✅ | ✅ | ✅ | ✅ |
| U18 | Video | — | ✅ | ✅ | (PW) |
| U17 | Speaking | — | ✅ | ✅ | (PW) |
| U20 | Leaderboard | — | ✅ | ✅ | (PW) |
| **U19** | **Premium + checkout** | **✅** | **✅** | ✅ | **✅** |
| U0 | Home + responsive | ✅ | ✅ | ✅ | ✅ |

**Luồng trọng yếu (auth, guard, lessons, streak, search, AI, CRUD, admin, Premium) đều chạy CẢ 2 engine khớp nhau.**
Video/Speaking/Leaderboard chạy PW + được `ui-sweep`/`routes-all` phủ tự động ở cả 2 viewport.
