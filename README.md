# HRMS Backend

**Stack:** Java 21 · Spring Boot 4.1 · Spring Security (JWT tự phát hành, OAuth2 Resource Server) · Spring Data JPA (Hibernate 7) · PostgreSQL · Flyway · springdoc-openapi · Lombok · MapStruct · Testcontainers

## Chạy local

Yêu cầu: JDK 21, Docker.

```bash
cp .env.example .env          # chỉnh lại nếu cần
docker compose up -d          # PostgreSQL ở cổng 5432
./mvnw spring-boot:run        # http://localhost:8080, đăng nhập admin / Admin@123
```

- Swagger UI: http://localhost:8080/swagger-ui.html (tắt ở profile `prod`)
- Health: http://localhost:8080/actuator/health
- Test: `./mvnw verify` (cần Docker cho Testcontainers)
- Build image: `docker build -t hrms .`

File `.env` được cả Spring Boot (`spring.config.import`) và `docker compose` đọc. Biến môi trường thật luôn được ưu tiên hơn `.env`.

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` / `prod` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `localhost:5432/hrms`, `hrms`, `hrms` | Kết nối PostgreSQL |
| `JPA_DDL_AUTO` | `validate` | `validate` / `none` (schema do Flyway quản lý) |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | `true` khi DB đã có bảng nhưng chưa có lịch sử Flyway |
| `JWT_SECRET` | (dev có sẵn) | **Bắt buộc** ở môi trường thật, tối thiểu 64 ký tự: `openssl rand -base64 64` |
| `JWT_ACCESS_TOKEN_TTL`, `JWT_REFRESH_TOKEN_TTL` | `15m`, `7d` | Thời hạn token |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL` | `admin`, (dev: `Admin@123`) | Tài khoản admin tạo lúc khởi động; bỏ trống mật khẩu để không tạo |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Origin frontend, hỗ trợ pattern `https://*.wetech.vn` |

## Cấu trúc

```
com.wetech.hrms
├── common                      # Dùng chung, không phụ thuộc nghiệp vụ
│   ├── constant/ApiPaths       # Prefix /api/v1
│   ├── entity/                 # BaseEntity (UUID v7, version, audit), AuditableEntity
│   ├── exception/              # ErrorCode, CommonErrorCode, AppException, GlobalExceptionHandler
│   ├── response/               # ApiResponse, PageResponse, FieldViolation
│   ├── util/SecurityUtils      # Lấy user hiện tại từ JWT
│   └── web/RequestIdFilter     # X-Request-Id + MDC cho log
├── config
│   ├── AppProperties           # Cấu hình app.* (CORS, JWT, admin khởi tạo, public paths)
│   ├── JpaAuditingConfig       # Tự điền created_by/updated_by
│   ├── OpenApiConfig
│   ├── SchedulingConfig        # Bật @Scheduled (dọn refresh token hết hạn)
│   └── security/               # SecurityConfig, JwtConfig (ký/xác thực JWT), xử lý 401/403
└── modules                     # Mỗi nghiệp vụ một package
    ├── auth                    # login / refresh / logout / me, RefreshToken
    └── user                    # User, Role, DataInitializer (tạo role + admin)
```

Một module nghiệp vụ nên có dạng:

```
modules/employee
├── controller/EmployeeController.java
├── service/EmployeeService.java
├── repository/EmployeeRepository.java
├── entity/Employee.java          # extends BaseEntity
├── dto/                          # *Request (có @Valid), *Response
├── mapper/EmployeeMapper.java    # @Mapper của MapStruct (đã cấu hình componentModel=spring)
└── exception/EmployeeErrorCode.java   # enum implements ErrorCode
```

## Quy ước

### Response

Mọi API trả về `ApiResponse`:

```json
{ "success": true, "code": "SUCCESS", "message": "Thành công", "data": { }, "timestamp": "..." }
```

```json
{ "success": false, "code": "VALIDATION_ERROR", "message": "Dữ liệu không hợp lệ",
  "errors": [{ "field": "email", "message": "must not be blank" }], "timestamp": "..." }
```

Danh sách phân trang trả `ApiResponse<PageResponse<T>>`; nhận `Pageable` ở controller (`?page=0&size=20&sort=createdAt,desc`, tối đa 100 bản ghi/trang) và dùng `PageResponse.of(page, mapper::toResponse)`.

### Lỗi

Service chỉ ném `AppException` với một `ErrorCode`; không tự tạo `ResponseEntity` lỗi trong controller.

```java
throw AppException.notFound("nhân viên", id);
throw new AppException(EmployeeErrorCode.EMAIL_EXISTED);
```

Mỗi module khai báo enum mã lỗi riêng:

```java
@Getter
@RequiredArgsConstructor
public enum EmployeeErrorCode implements ErrorCode {
	EMAIL_EXISTED(HttpStatus.CONFLICT, "Email đã được sử dụng");

	private final HttpStatus httpStatus;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
```

`GlobalExceptionHandler` đã xử lý sẵn: validation (body, param, `@Validated` service), JSON sai định dạng, sai kiểu tham số, 404, 405, 415, 413, 403 từ `@PreAuthorize`, optimistic lock (409), vi phạm ràng buộc DB (409) và lỗi không lường trước (500, có log).

### Xác thực & phân quyền

Ứng dụng tự phát hành token:

- **Access token**: JWT ký HS512 bằng `JWT_SECRET`, sống `JWT_ACCESS_TOKEN_TTL` (mặc định 15 phút). Gửi kèm header `Authorization: Bearer <token>`.
- **Refresh token**: chuỗi ngẫu nhiên, DB chỉ lưu SHA-256. Mỗi lần refresh, token cũ bị thu hồi và cấp token mới. Nếu một token đã thu hồi bị dùng lại (dấu hiệu bị đánh cắp), toàn bộ phiên của user đó bị thu hồi.

| Endpoint | Cần token | Mô tả |
|---|---|---|
| `POST /api/v1/auth/login` | – | `{username, password}` → `{accessToken, refreshToken, expiresIn, ...}` |
| `POST /api/v1/auth/refresh` | – | `{refreshToken}` → cặp token mới |
| `POST /api/v1/auth/logout` | – | `{refreshToken}` → thu hồi refresh token |
| `GET /api/v1/auth/me` | ✓ | Thông tin người dùng hiện tại |

Mọi endpoint khác đều yêu cầu token, trừ health, Swagger và các path trong `app.security.public-paths`.

Khi khởi động, `DataInitializer` tạo role `ADMIN`, `USER` và tài khoản admin (`ADMIN_USERNAME` / `ADMIN_PASSWORD`) nếu chưa có. Ở profile `dev` mặc định là `admin` / `Admin@123`.

Role của user nằm trong claim `roles` của access token và được map thành authority `ROLE_xxx`:

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
```

Thay đổi role chỉ có hiệu lực từ lần cấp token tiếp theo; logout không vô hiệu hoá access token còn hạn.

Lấy người dùng hiện tại: `SecurityUtils.getCurrentUserId()` (UUID, claim `sub`) và `SecurityUtils.getCurrentUsername()`.

### Database

- Schema do **Flyway** quản lý: script SQL đặt trong `src/main/resources/db/migration`, tự chạy khi ứng dụng khởi động. Hibernate chỉ `validate` (entity lệch với bảng thì ứng dụng không khởi động).
- Thêm/sửa bảng: tạo file mới `V<số tiếp theo>__<mô_tả>.sql` (vd. `V2__create_employees.sql`, hai dấu gạch dưới), viết DDL khớp với entity. **Không sửa** file đã chạy trên bất kỳ môi trường nào (Flyway kiểm checksum và sẽ dừng khởi động); muốn đổi thì viết migration mới.
- Khoá ngoại, index, unique đặt tên rõ ràng theo dạng `fk_<bảng>_<cột>`, `idx_<bảng>_<cột>`, `uk_<bảng>_<cột>`. Nhớ tạo index cho cột khoá ngoại (PostgreSQL không tự tạo).
- DB dev đã có bảng từ trước khi dùng Flyway: reset bằng `docker compose down -v && docker compose up -d`, hoặc đặt `FLYWAY_BASELINE_ON_MIGRATE=true` để bỏ qua V1.
- Lịch sử migration nằm ở bảng `flyway_schema_history`.
- Entity kế thừa `BaseEntity` tự có các cột `id` (UUID v7), `version`, `created_at`, `created_by`, `updated_at`, `updated_by`.
- Thời gian lưu kiểu `Instant` / `TIMESTAMPTZ` theo UTC; frontend tự đổi sang giờ địa phương.
- `open-in-view` đã tắt: lazy loading phải nằm trong `@Transactional` của service.

### Log

Mỗi request có `X-Request-Id` (lấy từ header client gửi lên hoặc tự sinh), được trả lại trong response và in trong mọi dòng log của request đó. Khi báo lỗi, gửi kèm giá trị này để tra log.

## Dùng làm base cho dự án mới

1. Copy repo, đổi `groupId`/`artifactId`/`name` trong `pom.xml`.
2. Đổi tên package `com.wetech.hrms` và class `HrmsApplication`.
3. Đổi `spring.application.name`, tên database trong `compose.yaml` / `.env.example`.
4. Đặt `JWT_SECRET` và `ADMIN_PASSWORD` riêng cho từng môi trường.
5. Xoá các module trong `modules/` không dùng tới (giữ `auth` và `user`) cùng migration tương ứng; giữ `V1__init_schema.sql`.
