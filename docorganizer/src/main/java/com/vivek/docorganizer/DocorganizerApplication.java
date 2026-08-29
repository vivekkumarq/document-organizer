package com.vivek.docorganizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Authentication is entirely JWT-based, so Spring Boot's default in-memory user is excluded.
 * Otherwise every startup logs a generated password for an account that is never used.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DocorganizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocorganizerApplication.class, args);
	}

}
