package com.hti;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrganisationApplication {

	private static final Logger logger = LoggerFactory.getLogger("tracklogger");

	public static void main(String[] args) {
		SpringApplication.run(OrganisationApplication.class, args);
		logger.info("Organisation Service Started Successfully");
	}
}