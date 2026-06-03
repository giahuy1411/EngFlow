# 05 — Git Workflow

---

## 1. Branch Strategy

```
🔴 Không commit trực tiếp lên main hoặc develop
🔴 Mọi tính năng/fix phải tạo branch riêng từ develop
🔴 Branch phải được merge qua Pull Request — không merge local
```

### Branch Structure

```
main          ← production code, chỉ merge từ release/* hoặc hotfix/*
develop       ← integration branch, base cho mọi feature
feature/*     ← tính năng mới
bugfix/*      ← fix bug trên develop
hotfix/*      ← fix bug khẩn cấp trực tiếp từ main
release/*     ← chuẩn bị release (freeze feature)
```

### Branch Naming

```
🔴 Dùng prefix chuẩn + kebab-case + mô tả ngắn
🟡 Thêm issue number nếu có ticket

feature/lesson-crud-api
feature/exercise-multiple-choice
feature/jwt-authentication
bugfix/login-token-expiry
bugfix/lesson-pagination-error
hotfix/critical-sql-injection-fix
release/v1.0.0

❌ Sai
feature/LessonAPI
new-feature
fix
my-branch-test
```

---

## 2. Commit Message

```
🔴 Theo chuẩn Conventional Commits
🔴 Subject line ≤ 72 ký tự
🔴 Dùng tiếng Anh hoặc tiếng Việt nhất quán trong team (chọn 1)
🔴 Commit làm 1 việc — không commit 5 thứ khác nhau trong 1 lần
```

### Format

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

### Types

| Type | Dùng Khi |
|------|---------|
| `feat` | Thêm tính năng mới |
| `fix` | Sửa bug |
| `refactor` | Refactor code (không fix bug, không thêm feature) |
| `test` | Thêm/sửa test |
| `docs` | Cập nhật documentation |
| `style` | Formatting, missing semicolons (không ảnh hưởng logic) |
| `chore` | Update dependencies, build scripts |
| `perf` | Cải thiện performance |
| `revert` | Revert commit trước |

### Scopes Của Dự Án

```
auth, user, lesson, exercise, vocabulary, grammar, progress,
security, database, docker, frontend, api
```

### Ví Dụ

```bash
# ✅ Đúng
feat(lesson): add pagination support for lesson list API
fix(auth): handle expired JWT token refresh correctly
refactor(exercise): extract answer validation to separate method
test(lesson): add unit tests for LessonServiceImpl
docs(api): update Swagger annotations for exercise endpoints
chore(deps): upgrade Spring Boot to 3.2.1
feat(frontend): implement MultipleChoice exercise component

# ✅ Với issue number
feat(vocabulary): add spaced repetition algorithm (#42)
fix(auth): fix 401 not returned on invalid token (#38)

# ❌ Sai
update code
fix bug
WIP
asdf
Thêm tính năng mới vào bài học và sửa lỗi login và cập nhật db  ← quá nhiều thứ
```

---

## 3. Pull Request Rules

```
🔴 Bắt buộc ít nhất 1 reviewer approve trước khi merge
🔴 PR phải pass tất cả CI checks (build, test, lint)
🔴 Không merge PR của chính mình (trừ khi solo developer)
🟡 PR size: tối đa 400 lines changed — nếu to hơn, tách nhỏ ra
🟡 PR phải có description mô tả what/why/how
🟢 Link tới issue/ticket liên quan
```

### PR Template

```markdown
## Mô Tả
<!-- Tính năng/fix này làm gì? Tại sao cần? -->

## Thay Đổi
- [ ] Backend: ...
- [ ] Frontend: ...
- [ ] Database migration: ...

## Test
- [ ] Unit tests đã pass
- [ ] Manual test: ...
- [ ] Screenshots (nếu có UI change)

## Checklist
- [ ] Code đúng convention
- [ ] Không có console.log / System.out.println
- [ ] Không có hardcoded credentials
- [ ] Migration file đã tạo (nếu có DB change)

Closes #<issue-number>
```

---

## 4. Code Review Guidelines

```
🔴 Reviewer phải check: logic đúng, security, performance, convention
🟡 Comment rõ ràng — nếu blocking phải giải thích lý do
🟢 Phân biệt blocking (🔴) vs suggestion (🟢) trong comment
🟢 Approve khi đồng ý, Request Changes khi có vấn đề nghiêm trọng
```

### Những Thứ Reviewer Phải Kiểm Tra

```
Security
  □ Không có hardcoded secret/password
  □ Input validation đầy đủ (@Valid)
  □ Authorization check đúng (không ai xem data của người khác)

Code Quality
  □ Không có logic trùng lặp
  □ Tên biến/method rõ nghĩa
  □ Exception handling đúng

Database
  □ Migration file có nếu thay đổi schema
  □ Không có N+1 query
  □ Index cho FK mới

Test
  □ Có unit test cho business logic mới
  □ Test cover happy path và edge case
```

---

## 5. Merge Strategy

```
🟡 Dùng Squash and Merge cho feature branches (clean history)
🟡 Dùng Merge Commit cho release và hotfix (preserve history)
🔴 Không force push lên shared branches (develop, main)
🔴 Delete feature branch sau khi merge
```

---

## 6. .gitignore — Những Gì Không Commit

```gitignore
# Java / Maven
target/
*.class
*.jar

# IDE
.idea/
*.iml
.vscode/
*.swp

# Environment / Secrets
.env
.env.local
application-dev.properties
application-prod.properties

# Node
node_modules/
dist/
.nuxt/

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/
```
