package com.datn.engflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.Arrays;

/**
 * Điểm khởi động của ứng dụng EngFlow: quét component {@code com.datn.engflow},
 * nạp cấu hình và bật ba tính năng hạ tầng cho toàn bộ bean.
 *
 * <p>Tầng khởi động. {@code @EnableScheduling} mở scheduler cho các job
 * {@code @Scheduled} (quét SePay, nhắc streak, hết hạn premium), {@code @EnableAsync}
 * mở luồng chạy nền, còn {@code @EnableSpringDataWebSupport} ép Spring Data
 * trả về {@code Page} dạng JSON thay vì đối tượng đệ quy.
 *
 * <p>Bean {@link #clock()} là đồng hồ hệ thống dùng chung cho toàn app: các service
 * nhận {@link Clock} qua constructor nên test có thể thay bằng đồng hồ tĩnh.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.DIRECT)
public class EngflowApplication {

	private static final Logger log = LoggerFactory.getLogger(EngflowApplication.class);

	/**
	 * Khởi chạy Spring context với các tham số dòng lệnh.
	 *
	 * @param args tham số dòng lệnh được chuyển tiếp cho {@link SpringApplication}
	 */
	public static void main(String[] args) {
		SpringApplication.run(EngflowApplication.class, args);
	}

	/**
	 * Runner chạy một lần lúc context dựng xong, chỉ để ghi log xem hạ tầng nào
	 * đã được nạp — không có tác dụng nghiệp vụ.
	 *
	 * @param ctx application context vừa được tạo
	 * @return runner ghi số bean có tên chứa "flyway" vào log khởi động
	 */
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			String[] beanNames = ctx.getBeanDefinitionNames();
			long count = Arrays.stream(beanNames)
					.filter(name -> name.toLowerCase().contains("flyway"))
					.count();
			log.info("Flyway beans detected: {}", count);
		};
	}

	/** Clock hệ thống cho scheduler (inject được → test giờ tĩnh). */
	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
