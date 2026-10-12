package com.example.todo.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.support.ApiSliceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ApiSliceTest
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void validationFailureListsEachInvalidField() throws Exception {
        mvc.perform(post("/api/v1/_test/validate").with(user("u"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    @Test
    void unknownPathReturns404ProblemDocument() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist").with(user("u")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.type").exists());
    }

    @Test
    void unexpectedFailureReturnsGeneric500WithoutInternals() throws Exception {
        String body = mvc.perform(get("/api/v1/_test/boom").with(user("u")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("com.example", "IllegalStateException", "secret internal detail", "at ");
    }

    @Test
    void applicationExceptionCarriesItsCode() throws Exception {
        mvc.perform(get("/api/v1/_test/api-error").with(user("u")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SAMPLE_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Sample conflict."));
    }

    @Test
    void unauthenticatedRequestReturns401ProblemDocument() throws Exception {
        mvc.perform(get("/api/v1/_test/api-error"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void missingRoleReturns403ProblemDocument() throws Exception {
        mvc.perform(get("/api/v1/_test/admin").with(user("u").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
