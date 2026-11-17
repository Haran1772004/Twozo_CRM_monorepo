package com.twozo.crm.client;

import com.twozo.crm.client.models.header.WebHeadersProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WebHeadersProperties.class)
public class CrmWebClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmWebClientApplication.class, args);
	}
}
