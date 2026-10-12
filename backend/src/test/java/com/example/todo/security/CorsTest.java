package com.example.todo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.support.ApiSliceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@ApiSliceTest
class CorsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void configuredOriginIsAllowedIncludingAuthorizationHeader() throws Exception {
        mvc.perform(options("/api/v1/health")
                        .header("Origin", "http://frontend.test")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://frontend.test"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization"));
    }

    @Test
    void simpleRequestFromConfiguredOriginGetsAllowOriginHeader() throws Exception {
        mvc.perform(get("/api/v1/health").header("Origin", "http://frontend.test"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://frontend.test"));
    }

    @Test
    void foreignOriginIsRejectedWithoutCorsAllowance() throws Exception {
        var result = mvc.perform(options("/api/v1/health")
                        .header("Origin", "http://evil.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();

        var actual = mvc.perform(get("/api/v1/health").header("Origin", "http://evil.test"))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(actual.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
