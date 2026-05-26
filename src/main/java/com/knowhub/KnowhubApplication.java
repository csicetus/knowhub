package com.knowhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KnowhubApplication {

	// 帮我查询 userId=1 的知识库列表
	public static void main(String[] args) {
		SpringApplication.run(KnowhubApplication.class, args);
	}
}
