package com.galactique.gestionecole.security.infrastructure.config;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CorsConfigurationTests {
	private final MockMvc mockMvc;

	@Autowired
	CorsConfigurationTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void allowsAngularPreflightForBearerApiRequests() throws Exception {
		mockMvc.perform(options("/api/v1/establishments")
			.header("Origin", "http://localhost:4200")
			.header("Access-Control-Request-Method", "GET")
			.header("Access-Control-Request-Headers", "authorization,content-type"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
			.andExpect(header().string("Access-Control-Allow-Methods", containsStringIgnoringCase("GET")))
			.andExpect(header().string("Access-Control-Allow-Headers", containsStringIgnoringCase("authorization")));
	}

	@Test
	void rejectsPreflightFromAnUnconfiguredOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/establishments")
			.header("Origin", "https://untrusted.example")
			.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}