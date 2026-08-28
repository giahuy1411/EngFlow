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

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.DIRECT)
/**
 * class EngflowApplication.
 */
public class EngflowApplication {

	private static final Logger log = LoggerFactory.getLogger(EngflowApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(EngflowApplication.class, args);
	}

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
}
