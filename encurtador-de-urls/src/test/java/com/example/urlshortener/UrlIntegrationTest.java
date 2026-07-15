package com.example.urlshortener;

import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.dto.CreateUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.testsupport.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UrlIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        // Limpar cache Redis antes de cada teste
        redis.delete("*");
    }

    @Test
    void shouldShortenAndRedirectWithCache() throws Exception {
        CreateUrlRequest req = new CreateUrlRequest("https://example.com/some/long/path", 60, null);

        MvcResult created = mockMvc.perform(post("/api/urls")
                        .with(req1 -> { req1.setRemoteAddr("203.0.113.1"); return req1; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.shortUrl").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();

        CreateUrlResponse body = objectMapper.readValue(
                created.getResponse().getContentAsString(), CreateUrlResponse.class);

        // Testar redirecionamento
        mockMvc.perform(get("/" + body.shortCode()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/some/long/path"));

        // Segundo acesso (cache hit)
        mockMvc.perform(get("/" + body.shortCode()))
                .andExpect(status().isFound());

        // Verificar cache
        String cached = redis.opsForValue().get("redirect:" + body.shortCode());
        assertThat(cached).isEqualTo("https://example.com/some/long/path");

        // Verificar estatísticas
        MvcResult statsResult = mockMvc.perform(get("/api/urls/" + body.shortCode() + "/stats"))
                .andExpect(status().isOk())
                .andReturn();

        UrlStatsResponse stats = objectMapper.readValue(statsResult.getResponse().getContentAsString(), UrlStatsResponse.class);
        assertThat(stats.totalAccesses()).isEqualTo(2);
        assertThat(stats.dailyAccesses()).isNotEmpty();
    }

    @Test
    void shouldReturn404ForUnknownShortCode() throws Exception {
        mockMvc.perform(get("/nonexist123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn400ForInvalidUrl() throws Exception {
        CreateUrlRequest req = new CreateUrlRequest("not-a-url", 60, null);
        mockMvc.perform(post("/api/urls")
                        .with(req1 -> { req1.setRemoteAddr("203.0.113.2"); return req1; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}