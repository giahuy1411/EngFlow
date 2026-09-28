package com.datn.engflow.config;

/*
 * ============================================================================
 * WebConfig — CỐ Ý COMMENT-OUT, KHÔNG PHẢI CODE CHẾT DO SƠ SUẤT.
 * ============================================================================
 *
 * Lý do: cấu hình CORS bị TRÙNG LẶP. Nếu bật lại class này, Spring MVC sẽ đăng ký
 * thêm một mapping CORS qua WebMvcConfigurer#addCorsMappings, song song với bean
 * CorsConfigurationSource trong SecurityConfig — hai nguồn CORS chồng nhau, xử lý
 * preflight/header không nhất quán và dễ sinh lỗi khó tìm.
 *
 * Toàn bộ CORS hiện được quản lý tại MỘT nơi duy nhất: SecurityConfig.java, bean
 * corsConfigurationSource(), đọc danh sách origin từ cấu hình `cors.allowed-origins`.
 * Muốn đổi origin/phương thức được phép → sửa ở SecurityConfig, KHÔNG bỏ comment ở đây.
 *
 * Giữ file lại (thay vì xoá hẳn) để lịch sử quyết định còn nguyên và để bất kỳ ai
 * tìm kiếm "WebConfig" cũng thấy ngay vì sao nó không hoạt động.
 * ============================================================================
 */

// Deprecated: This class is commented out to resolve the duplicate CORS configuration issue.
// All CORS settings are now configured entirely in SecurityConfig.java, via its
// corsConfigurationSource() bean — re-enabling this block would register a second,
// conflicting CORS mapping.
/*
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:80")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
*/
