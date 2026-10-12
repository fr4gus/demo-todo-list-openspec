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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The TODO endpoints end to end (real security chain, PostgreSQL and Redis). Examples in backend/README.md mirror these. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestSupport.class, RedisTestSupport.class})
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt-secret=" + TestSecrets.JWT_SECRET,
        "app.frontend-origin=http://frontend.test"
})
class TodoControllerTest {

    static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    TodoItemRepository items;
    @Autowired
    JwtService jwtService;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    org.springframework.cache.CacheManager cacheManager;

    User ana;
    User bob;
    User admin;

    @BeforeEach
    void seed() {
        items.deleteAll();
        users.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        ana = users.save(new User("Ana", "ana@example.com", "hash", Role.USER));
        bob = users.save(new User("Bob", "bob@example.com", "hash", Role.USER));
        admin = users.save(new User("Boss", "boss@example.com", "hash", Role.ADMIN));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.issue(user).token();
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", bearer(user));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String todoBody(String text, String priority) {
        return "{\"text\":\"%s\",\"priority\":\"%s\"}".formatted(text, priority);
    }

    private String create(User user, String text, String priority) throws Exception {
        String response = mvc.perform(json(as(user, post("/api/v1/todos")), todoBody(text, priority)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions complete(User user, String id, boolean completed) throws Exception {
        return mvc.perform(json(as(user, patch("/api/v1/todos/" + id + "/completion")), "{\"completed\":" + completed + "}"));
    }

    // --- create ---

    @Test
    void createReturns201WithTheNewActiveItem() throws Exception {
        mvc.perform(json(as(ana, post("/api/v1/todos")), todoBody("  Buy milk  ", "MEDIUM_HIGH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchesPattern(UUID_PATTERN)))
                .andExpect(jsonPath("$.text").value("Buy milk"))
                .andExpect(jsonPath("$.priority").value("MEDIUM_HIGH"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void createOwnsTheItemByTheCallerEvenIfTheBodyNamesAnotherUser() throws Exception {
        mvc.perform(json(as(ana, post("/api/v1/todos")),
                        "{\"text\":\"mine\",\"priority\":\"LOW\",\"userId\":\"%s\"}".formatted(bob.getId())))
                .andExpect(status().isCreated());

        mvc.perform(as(bob, get("/api/v1/todos"))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as(ana, get("/api/v1/todos"))).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void blankTextIs400WithTheFieldListed() throws Exception {
        mvc.perform(json(as(ana, post("/api/v1/todos")), todoBody("   ", "HIGH")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("text"));
        mvc.perform(as(ana, get("/api/v1/todos"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void textOver500CharactersIs400ButExactly500Passes() throws Exception {
        mvc.perform(json(as(ana, post("/api/v1/todos")), todoBody("x".repeat(501), "HIGH")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("text"));
        mvc.perform(json(as(ana, post("/api/v1/todos")), todoBody("x".repeat(500), "HIGH")))
                .andExpect(status().isCreated());
    }

    @Test
    void whitespaceAroundA500CharacterTextDoesNotCount() throws Exception {
        mvc.perform(json(as(ana, post("/api/v1/todos")), todoBody("  " + "x".repeat(500) + "  ", "HIGH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("x".repeat(500)));
    }

    @Test
    void missingPriorityIs400WithTheFieldListed() throws Exception {
        mvc.perform(json(as(ana, post("/api/v1/todos")), "{\"text\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("priority"));
    }

    @Test
    void unknownPriorityIs400WithTheFieldListed() throws Exception {
        mvc.perform(json(as(ana, post("/api/v1/todos")), todoBody("x", "URGENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("priority"));
        mvc.perform(as(ana, get("/api/v1/todos"))).andExpect(jsonPath("$", hasSize(0)));
    }

    // --- list ---

    @Test
    void emptyListIs200WithAnEmptyArray() throws Exception {
        mvc.perform(as(ana, get("/api/v1/todos")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));
    }

    @Test
    void listIsOrderedActiveFirstThenPriorityThenOldestFirst() throws Exception {
        String lowOld = create(ana, "low-old", "LOW");
        String mediumLow = create(ana, "medium-low", "MEDIUM_LOW");
        String highDone = create(ana, "high-done", "HIGH");
        String highNew = create(ana, "high-new", "HIGH");
        String mediumHigh = create(ana, "medium-high", "MEDIUM_HIGH");
        String lowDone = create(ana, "low-done", "LOW");
        String highOld = create(ana, "high-old", "HIGH");
        // Pin creation times: "high-old" is the oldest high-priority active item even though it was inserted last.
        int minute = 0;
        for (String id : new String[] {highOld, lowOld, mediumLow, highDone, mediumHigh, lowDone, highNew}) {
            jdbc.update("UPDATE todo_items SET created_at = now() - interval '1 day' + ? * interval '1 minute' WHERE id = ?::uuid",
                    minute++, id);
        }
        complete(ana, highDone, true).andExpect(status().isOk());
        complete(ana, lowDone, true).andExpect(status().isOk());

        mvc.perform(as(ana, get("/api/v1/todos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].text").value(org.hamcrest.Matchers.contains(
                        "high-old", "high-new", "medium-high", "medium-low", "low-old", "high-done", "low-done")))
                .andExpect(jsonPath("$[*].completed").value(org.hamcrest.Matchers.contains(
                        false, false, false, false, false, true, true)));
    }

    @Test
    void mixedListFromTheSpecScenarioPutsCompletedLast() throws Exception {
        String oldLow = create(ana, "old low", "LOW");
        create(ana, "new high", "HIGH");
        String doneHigh = create(ana, "done high", "HIGH");
        jdbc.update("UPDATE todo_items SET created_at = now() - interval '1 day' WHERE id = ?::uuid", oldLow);
        complete(ana, doneHigh, true);

        mvc.perform(as(ana, get("/api/v1/todos")))
                .andExpect(jsonPath("$[*].text").value(org.hamcrest.Matchers.contains("new high", "old low", "done high")));
    }

    @Test
    void listsAreSeparatePerUser() throws Exception {
        create(ana, "ana-item", "HIGH");
        create(bob, "bob-item", "HIGH");

        mvc.perform(as(ana, get("/api/v1/todos"))).andExpect(jsonPath("$[*].text").value(org.hamcrest.Matchers.contains("ana-item")));
        mvc.perform(as(bob, get("/api/v1/todos"))).andExpect(jsonPath("$[*].text").value(org.hamcrest.Matchers.contains("bob-item")));
    }

    // --- edit ---

    @Test
    void editReturns200WithTheUpdatedItemAndTheListReflectsIt() throws Exception {
        String id = create(ana, "old", "LOW");
        create(ana, "other", "MEDIUM_LOW");

        mvc.perform(json(as(ana, put("/api/v1/todos/" + id)), todoBody("new text", "HIGH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.text").value("new text"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false));

        mvc.perform(as(ana, get("/api/v1/todos")))
                .andExpect(jsonPath("$[*].text").value(org.hamcrest.Matchers.contains("new text", "other")));
    }

    @Test
    void editingACompletedItemKeepsItCompleted() throws Exception {
        String id = create(ana, "old", "LOW");
        complete(ana, id, true).andExpect(status().isOk());

        mvc.perform(json(as(ana, put("/api/v1/todos/" + id)), todoBody("edited", "HIGH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.text").value("edited"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void invalidEditIs400AndChangesNothing() throws Exception {
        String id = create(ana, "keep", "LOW");

        mvc.perform(json(as(ana, put("/api/v1/todos/" + id)), todoBody("", "LOW")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("text"));

        mvc.perform(as(ana, get("/api/v1/todos"))).andExpect(jsonPath("$[0].text").value("keep"));
    }

    // --- completion ---

    @Test
    void completeThenRestoreMovesTheItemAfterAndBackToItsPriorityPosition() throws Exception {
        String high = create(ana, "high", "HIGH");
        create(ana, "low", "LOW");

        complete(ana, high, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
        mvc.perform(as(ana, get("/api/v1/todos")))
                .andExpect(jsonPath("$[*].text").value(org.hamcrest.Matchers.contains("low", "high")));

        complete(ana, high, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));
        mvc.perform(as(ana, get("/api/v1/todos")))
                .andExpect(jsonPath("$[*].text").value(org.hamcrest.Matchers.contains("high", "low")));
    }

    @Test
    void repeatingACompletionStateIs200AndChangesNothing() throws Exception {
        String id = create(ana, "x", "LOW");
        complete(ana, id, true).andExpect(status().isOk());
        String completedAt = jdbc.queryForObject("SELECT completed_at::text FROM todo_items WHERE id = ?::uuid", String.class, id);

        complete(ana, id, true).andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        complete(ana, create(ana, "y", "LOW"), false).andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(false));

        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("SELECT completed_at::text FROM todo_items WHERE id = ?::uuid", String.class, id))
                .isEqualTo(completedAt);
    }

    @Test
    void completionWithoutAFlagIs400() throws Exception {
        String id = create(ana, "x", "LOW");

        mvc.perform(json(as(ana, patch("/api/v1/todos/" + id + "/completion")), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("completed"));
    }

    // --- delete ---

    @Test
    void deleteReturns204AndTheItemIsGoneFromTheList() throws Exception {
        String active = create(ana, "active", "HIGH");
        String done = create(ana, "done", "LOW");
        complete(ana, done, true);

        mvc.perform(as(ana, delete("/api/v1/todos/" + active))).andExpect(status().isNoContent());
        mvc.perform(as(ana, delete("/api/v1/todos/" + done))).andExpect(status().isNoContent());

        mvc.perform(as(ana, get("/api/v1/todos"))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as(ana, delete("/api/v1/todos/" + active))).andExpect(status().isNotFound());
    }

    // --- privacy ---

    @Test
    void anotherUsersItemIs404ForEditCompleteAndDeleteAndIsLeftUntouched() throws Exception {
        String id = create(ana, "ana-private", "HIGH");

        mvc.perform(json(as(bob, put("/api/v1/todos/" + id)), todoBody("hacked", "LOW")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"));
        complete(bob, id, true).andExpect(status().isNotFound());
        mvc.perform(as(bob, delete("/api/v1/todos/" + id))).andExpect(status().isNotFound());

        mvc.perform(as(ana, get("/api/v1/todos")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text").value("ana-private"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void anUnknownIdLooksTheSameAsAnotherUsersId() throws Exception {
        String foreign = create(ana, "ana-private", "HIGH");

        String foreignBody = mvc.perform(as(bob, delete("/api/v1/todos/" + foreign)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String unknownBody = mvc.perform(as(bob, delete("/api/v1/todos/" + UUID.randomUUID())))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(JsonPath.<String>read(foreignBody, "$.detail"))
                .isEqualTo(JsonPath.<String>read(unknownBody, "$.detail"));
    }

    @Test
    void aMalformedIdIs400NotA500() throws Exception {
        mvc.perform(as(ana, delete("/api/v1/todos/not-a-uuid"))).andExpect(status().isBadRequest());
    }

    // --- authentication and roles ---

    @Test
    void noTokenIs401OnEveryEndpoint() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get("/api/v1/todos")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/v1/todos"), todoBody("x", "LOW"))).andExpect(status().isUnauthorized());
        mvc.perform(json(put("/api/v1/todos/" + id), todoBody("x", "LOW"))).andExpect(status().isUnauthorized());
        mvc.perform(json(patch("/api/v1/todos/" + id + "/completion"), "{\"completed\":true}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/todos/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminTokenIs403OnEveryEndpointAndSeesNoContent() throws Exception {
        String id = create(ana, "ana-private", "HIGH");

        mvc.perform(as(admin, get("/api/v1/todos"))).andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ana-private"))));
        mvc.perform(json(as(admin, post("/api/v1/todos")), todoBody("x", "LOW"))).andExpect(status().isForbidden());
        mvc.perform(json(as(admin, put("/api/v1/todos/" + id)), todoBody("x", "LOW"))).andExpect(status().isForbidden());
        complete(admin, id, true).andExpect(status().isForbidden());
        mvc.perform(as(admin, delete("/api/v1/todos/" + id))).andExpect(status().isForbidden());

        mvc.perform(as(ana, get("/api/v1/todos"))).andExpect(jsonPath("$", hasSize(1)));
    }
}
