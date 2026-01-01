package com.alphaskyport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.alphaskyport")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.alphaskyport")
public class AlphaSkyportApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlphaSkyportApplication.class, args);
	}

}
