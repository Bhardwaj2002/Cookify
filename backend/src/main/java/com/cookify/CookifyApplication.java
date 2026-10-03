package com.cookify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CookifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(CookifyApplication.class, args);
	}

}
