package com.datn.engflow;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class EngflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(EngflowApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			System.out.println("=========================================");
			System.out.println("=== CHECKING FLYWAY BEANS IN CONTEXT ===");
			String[] beanNames = ctx.getBeanDefinitionNames();
			long count = Arrays.stream(beanNames)
					.filter(name -> name.toLowerCase().contains("flyway"))
					.peek(name -> System.out.println(" - " + name))
					.count();
			System.out.println("Total Flyway beans: " + count);
			System.out.println("=========================================");
		};
	}
}
