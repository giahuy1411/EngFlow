# 01 — Backend Java / Spring Boot

---

## 1. Naming Convention

### Classes & Interfaces
```
🔴 PascalCase cho tất cả class, interface, enum
🔴 Tên class phải rõ nghĩa, không viết tắt tùy tiện

✅ LessonService, ExerciseController, UserRepository
❌ LsnSvc, ExCtrl, UsrRepo
```

| Loại | Pattern | Ví dụ |
|------|---------|-------|
| Entity | `<Name>` | `Lesson`, `User` |
| Repository | `<Name>Repository` | `LessonRepository` |
| Service interface | `<Name>Service` | `LessonService` |
| Service impl | `<Name>ServiceImpl` | `LessonServiceImpl` |
| Controller | `<Name>Controller` | `LessonController` |
| DTO Request | `<Name>Request` | `CreateLessonRequest` |
| DTO Response | `<Name>Response` | `LessonResponse` |
| Exception | `<Name>Exception` | `ResourceNotFoundException` |
| Config | `<Name>Config` | `SecurityConfig` |
| Enum | `<Name>` (PascalCase) | `LessonLevel`, `UserRole` |

### Methods & Variables
```
🔴 camelCase
🔴 Động từ cho methods: get, create, update, delete, find, validate, build
🟡 Boolean method dùng prefix is/has/can: isActive(), hasPermission()

✅ getUserById(), createLesson(), isEmailValid()
❌ user_by_id(), CreateLesson(), emailValid()
```

### Constants
```
🔴 UPPER_SNAKE_CASE
✅ MAX_LOGIN_ATTEMPTS, DEFAULT_PAGE_SIZE
❌ maxLoginAttempts, defaultPageSize
```

---

## 2. Package Structure

```
🔴 Đúng theo cấu trúc đã định nghĩa — không tự ý tạo package mới ngoài quy hoạch

com.englishlearning
├── config/          ← @Configuration classes
├── controller/      ← @RestController
├── service/         ← interface
│   └── impl/        ← @Service implementation
├── repository/      ← @Repository / JpaRepository
├── model/
│   ├── entity/      ← @Entity JPA
│   ├── dto/
│   │   ├── request/ ← inbound DTOs
│   │   └── response/← outbound DTOs
│   └── enums/       ← enum types
├── security/        ← JWT, filter, UserDetails
├── exception/       ← custom exceptions + handler
└── util/            ← static utilities
```

---

## 3. Annotation Rules

```java
// 🔴 Entity — bắt buộc đủ annotations
@Entity
@Table(name = "lessons")          // tên bảng SQL Server
@Getter @Setter                   // Lombok
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Lesson { ... }

// 🔴 Controller — không inject field trực tiếp
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor          // constructor injection qua Lombok
public class LessonController {
    private final LessonService lessonService;  // final + @RequiredArgsConstructor
}

// 🚫 Cấm field injection
@Autowired                        // ❌ KHÔNG DÙNG @Autowired field injection
private LessonService lessonService;
```

---

## 4. Exception Handling

```java
// 🔴 Tất cả exception phải đi qua GlobalExceptionHandler
// 🔴 Không dùng try-catch để nuốt exception mà không log

// ✅ Đúng — throw exception có nghĩa
public Lesson getById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", id));
}

// ❌ Sai — nuốt exception
try {
    return repository.findById(id).get();
} catch (Exception e) {
    return null;  // KHÔNG LÀM THẾ NÀY
}

// 🔴 Log trước khi throw ở service layer
log.error("Không tìm thấy lesson với id={}", id);
throw new ResourceNotFoundException("Lesson", "id", id);
```

---

## 5. Logging

```java
// 🔴 Dùng SLF4J + Lombok @Slf4j — không dùng System.out.println
@Slf4j
@Service
public class LessonServiceImpl {

    public LessonResponse create(CreateLessonRequest req) {
        log.info("Tạo lesson mới: title={}", req.getTitle());   // INFO cho business events
        // ...
        log.debug("Lesson đã lưu: id={}", saved.getId());       // DEBUG cho details
    }
}

// 🔴 Log level guide
// ERROR  — lỗi không phục hồi được, cần alert ngay
// WARN   — lỗi nhưng hệ thống vẫn chạy được
// INFO   — sự kiện quan trọng của business logic
// DEBUG  — chi tiết cho developer (tắt ở production)

// 🚫 Cấm log thông tin nhạy cảm
log.info("User login: email={}, password={}", email, password);  // ❌ TUYỆT ĐỐI KHÔNG
log.info("User login: email={}", email);                          // ✅
```

---

## 6. Service Layer Rules

```
🔴 Business logic CHỈ nằm trong Service — không đặt trong Controller hay Repository
🔴 @Transactional trên method thay đổi data (create, update, delete)
🟡 @Transactional(readOnly = true) trên method chỉ đọc — tối ưu performance
🔴 Service interface không được import class của Spring framework
```

```java
// ✅ Đúng
@Transactional
public LessonResponse create(CreateLessonRequest request) { ... }

@Transactional(readOnly = true)
public List<LessonResponse> getAll() { ... }

// ❌ Sai — logic trong Controller
@PostMapping
public ResponseEntity<LessonResponse> create(@RequestBody CreateLessonRequest req) {
    Lesson lesson = new Lesson();
    lesson.setTitle(req.getTitle());      // ❌ mapping này phải trong Service
    repository.save(lesson);              // ❌ gọi repo trực tiếp từ Controller
    ...
}
```

---

## 7. Validation

```java
// 🔴 @Valid trên mọi @RequestBody
// 🔴 Constraint annotation trên DTO, không validate trong Service bằng if-else

// ✅ Đúng — validate ở DTO
public class CreateLessonRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @NotNull(message = "Cấp độ không được để trống")
    private LessonLevel level;
}

// Controller
@PostMapping
public ResponseEntity<?> create(@Valid @RequestBody CreateLessonRequest req) { ... }

// ❌ Sai — validate thủ công trong Service
if (request.getTitle() == null || request.getTitle().isBlank()) {
    throw new BadRequestException("Title rỗng");  // ❌ nên dùng @NotBlank
}
```

---

## 8. Code Hygiene

```
🔴 Không có unused imports
🔴 Không có commented-out code khi commit (dùng git stash thay thế)
🔴 Không có TODO/FIXME quá 3 ngày — phải tạo issue tracker
🟡 Method không dài hơn 50 dòng — nếu dài hơn, tách thành private helper methods
🟡 Class không dài hơn 300 dòng
🟢 Javadoc cho public methods của Service interface
```

---

## 9. Testing

```
🔴 Unit test cho mọi method public của Service (JUnit 5 + Mockito)
🔴 Test coverage tối thiểu 70% cho Service layer
🟡 Integration test cho Controller endpoints quan trọng
🟢 Test method naming: should_<ExpectedBehavior>_when_<Condition>
```

```java
// ✅ Test naming đúng
@Test
void should_ReturnLesson_when_ValidIdProvided() { ... }

@Test
void should_ThrowNotFoundException_when_LessonNotExist() { ... }

// Mock pattern chuẩn
@ExtendWith(MockitoExtension.class)
class LessonServiceTest {
    @Mock LessonRepository repository;
    @InjectMocks LessonServiceImpl service;

    @Test
    void should_CreateLesson_when_ValidRequest() {
        // Arrange
        var request = new CreateLessonRequest("Lesson 1", LessonLevel.BEGINNER);
        var entity = Lesson.builder().id(1L).title("Lesson 1").build();
        when(repository.save(any())).thenReturn(entity);

        // Act
        var result = service.create(request);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Lesson 1");
        verify(repository, times(1)).save(any());
    }
}
```
