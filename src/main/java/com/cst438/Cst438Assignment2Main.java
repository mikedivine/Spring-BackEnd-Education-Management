package com.cst438;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RsaKeyProperties.class)
public class Cst438Assignment2Main {

	public static void main(String[] args) {
		SpringApplication.run(Cst438Assignment2Main.class, args);
	}

}
