package com.example.urlshortener;

import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.testsupport.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RateLimitIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void shouldBlockEleventhRequestInOneMinute() throws Exception {
        CreateUrlRequest req = new CreateUrlRequest("https://example.com/" + System.nanoTime(), 60);
        byte[] body = objectMapper.writeValueAsBytes(req);

        // Fazer 10 requisições (dentro do limite)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/urls")
                            .with(req1 -> { req1.setRemoteAddr("198.51.100.7"); return req1; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        // 11ª requisição deve ser bloqueada
        mockMvc.perform(post("/api/urls")
                        .with(req1 -> { req1.setRemoteAddr("198.51.100.7"); return req1; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}