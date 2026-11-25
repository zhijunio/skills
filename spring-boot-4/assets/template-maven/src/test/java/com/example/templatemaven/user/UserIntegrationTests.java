package com.example.templatemaven.user;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.templatemaven.PostgresTestcontainersConfiguration;
import com.example.templatemaven.RedisTestcontainersConfiguration;
import com.example.templatemaven.user.persistence.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class})
class UserIntegrationTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    com.example.templatemaven.notification.application.EmailNotificationService emailNotificationService;

    @Autowired
    UserMapper userMapper;

    @Test
    void createsAndFindsUser() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"Ada@example.com\",\"displayName\":\"Ada\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.createdAt").isNumber());
    }

    @Test
    void openApiDocumentIncludesControllerJavaDoc() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users'].post.description").value("Creates a user account."));
    }

    @Test
    void rejectsInvalidUserRequest() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"bad\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors[0].field").exists());
    }

    @Test
    void duplicateEmailReturnsConflictFromDatabaseConstraint() throws Exception {
        var email = "duplicate-" + java.util.UUID.randomUUID() + "@example.com";
        createUser(email);

        mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"displayName\":\"Duplicate\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User already exists"));
    }

    @Test
    void validationMessagesFollowAcceptLanguage() throws Exception {
        mvc.perform(post("/api/users")
                        .header("Accept-Language", "zh-CN")
                        .contentType("application/json")
                        .content("{\"email\":\"bad\",\"displayName\":\"Ada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value("邮箱格式不正确。"));
    }

    @Test
    void rejectsNonPositivePathIdWithProblemDetail() throws Exception {
        mvc.perform(get("/api/users/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void listsFirstAndSubsequentSpringDataPages() throws Exception {
        createUser("page-one-" + java.util.UUID.randomUUID() + "@example.com");
        createUser("page-two-" + java.util.UUID.randomUUID() + "@example.com");

        var firstPage = mvc.perform(get("/api/users?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andReturn();
        var secondPage = mvc.perform(get("/api/users?page=1&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andReturn();

        var firstId =
                com.jayway.jsonpath.JsonPath.read(firstPage.getResponse().getContentAsString(), "$.content[0].id");
        var secondId =
                com.jayway.jsonpath.JsonPath.read(secondPage.getResponse().getContentAsString(), "$.content[0].id");
        assertNotEquals(firstId, secondId);
    }

    @Test
    void rejectsInvalidPageNumberWithProblemDetail() throws Exception {
        mvc.perform(get("/api/users?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void returnsNotFoundForMissingUser() throws Exception {
        mvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"));
    }

    @Test
    void updatesUserWithCurrentVersionAndRejectsStaleVersion() throws Exception {
        var created = mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"grace@example.com\",\"displayName\":\"Grace\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        var userId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/{id}", userId)
                        .contentType("application/json")
                        .content("{\"email\":\"grace@example.com\",\"displayName\":\"Grace Hopper\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.displayName").value("Grace Hopper"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/{id}", userId)
                        .contentType("application/json")
                        .content("{\"email\":\"grace@example.com\",\"displayName\":\"Stale update\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User update conflict"));
    }

    private void createUser(String email) throws Exception {
        mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"displayName\":\"Pagination user\"}"))
                .andExpect(status().isCreated());
    }
}
