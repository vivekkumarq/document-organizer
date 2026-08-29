package com.vivek.docorganizer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DocorganizerApplicationTests {

	@Autowired
	private WebApplicationContext context;

	@Test
	void contextLoads() {
		assertThat(context).isNotNull();
		assertThat(context.getBean(com.vivek.docorganizer.service.DocumentService.class)).isNotNull();
	}
}
