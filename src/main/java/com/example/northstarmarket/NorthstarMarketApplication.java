package com.example.northstarmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NorthstarMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(NorthstarMarketApplication.class, args);
	}

}
