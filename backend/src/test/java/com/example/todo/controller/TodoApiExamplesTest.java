package com.example.todo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.todo.entity.Role;
import com.example.todo.entity.User;
import com.example.todo.repository.TodoItemRepository;
import com.example.todo.repository.UserRepository;
import com.example.todo.security.JwtService;
import com.example.todo.support.PostgresTestSupport;
import com.example.todo.support.RedisTestSupport;
import com.example.todo.support.TestSecrets;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs, verbatim, the examples shown in backend/README.md under "TODO endpoints". */
@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestSupport.class, RedisTestSupport.class})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt-secret=" + TestSecrets.JWT_SECRET,
        "app.frontend-origin=http://frontend.test"
})
class TodoApiExamplesTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    TodoItemRepository items;
    @Autowired
    JwtService jwtService;
    @Autowired
    CacheManager cacheManager;

    String token;

    @BeforeEach
    void seed() {
        items.deleteAll();
        users.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        token = "Bearer " + jwtService.issue(users.save(new User("Ana", "ana@example.com", "hash", Role.USER))).token();
    }

    private MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request, String body) {
        return request.header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void createEditCompleteRestoreDeleteExamples() throws Exception {
        // Create (201)
        String created = mvc.perform(withBody(post("/api/v1/todos"), "{\"text\":\"  Buy milk  \",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.text").value("Buy milk"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        // Edit (200)
        mvc.perform(withBody(put("/api/v1/todos/" + id), "{\"text\":\"Buy oat milk\",\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Buy oat milk"))
                .andExpect(jsonPath("$.priority").value("LOW"));

        // Complete (200), repeat (200), restore (200)
        mvc.perform(withBody(patch("/api/v1/todos/" + id + "/completion"), "{\"completed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        mvc.perform(withBody(patch("/api/v1/todos/" + id + "/completion"), "{\"completed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        mvc.perform(withBody(patch("/api/v1/todos/" + id + "/completion"), "{\"completed\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(false));

        // List shows the item; delete (204, empty body) removes it
        mvc.perform(get("/api/v1/todos").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/v1/todos/" + id).header("Authorization", token))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/v1/todos").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidBodyExample() throws Exception {
        mvc.perform(withBody(post("/api/v1/todos"), "{\"text\":\"\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"type":"https://todo.example.com/problems/validation-failed","title":"Bad Request","status":400,
                         "detail":"Request validation failed.","instance":"/api/v1/todos","code":"VALIDATION_FAILED",
                         "errors":[{"message":"must not be blank","field":"text"}]}""", true));
    }

    @Test
    void unknownPriorityExample() throws Exception {
        mvc.perform(withBody(post("/api/v1/todos"), "{\"text\":\"x\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("priority"))
                .andExpect(jsonPath("$.errors[0].message").value("has an invalid value"));
    }

    @Test
    void notFoundExample() throws Exception {
        String id = "3f2b8c1e-5a47-4d8e-9a60-1b7c2d9e4f10";
        mvc.perform(delete("/api/v1/todos/" + id).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().json("""
                        {"type":"https://todo.example.com/problems/todo-not-found","title":"Not Found","status":404,
                         "detail":"TODO item not found.","instance":"/api/v1/todos/3f2b8c1e-5a47-4d8e-9a60-1b7c2d9e4f10",
                         "code":"TODO_NOT_FOUND"}""", true));
    }

    @Test
    void errorCodesTableForAuthenticationAndRoles() throws Exception {
        mvc.perform(get("/api/v1/todos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        String adminToken = "Bearer " + jwtService.issue(users.save(new User("Boss", "boss@example.com", "hash", Role.ADMIN))).token();
        mvc.perform(get("/api/v1/todos").header("Authorization", adminToken)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void malformedIdIs400() throws Exception {
        mvc.perform(delete("/api/v1/todos/not-a-uuid").header("Authorization", token)).andExpect(status().isBadRequest());
    }
}
