package com.appointmentbooking.security;

import com.appointmentbooking.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CorsPreflightTest.DummyController.class)
@Import({SecurityConfig.class, CorsPreflightTest.TestConfig.class})
class CorsPreflightTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AppProperties appProperties() {
            AppProperties p = new AppProperties();
            p.getCors().setAllowedOrigins("http://localhost:*,http://127.0.0.1:*");
            return p;
        }
    }

    @RestController
    static class DummyController {
        @GetMapping("/api/v1/branches")
        String branches() {
            return "[]";
        }
    }

    @Test
    void preflight_shouldReturn200_forAnyLocalhostPort() throws Exception {
        mockMvc.perform(options("/api/v1/branches")
                        .header("Origin", "http://localhost:3001")
                        .header("Access-Control-Request-Method", "GET"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
