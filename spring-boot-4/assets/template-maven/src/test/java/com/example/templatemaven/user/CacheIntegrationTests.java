package com.example.templatemaven.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.templatemaven.PostgresTestcontainersConfiguration;
import com.example.templatemaven.RedisTestcontainersConfiguration;
import com.example.templatemaven.user.api.UpdateUserRequest;
import com.example.templatemaven.user.application.UserService;
import com.example.templatemaven.user.persistence.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class})
class CacheIntegrationTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    com.example.templatemaven.notification.application.EmailNotificationService emailNotificationService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    UserService userService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    void doesNotEvictCachedUserWhenUpdateTransactionRollsBack() throws Exception {
        var email = "rollback-cache-" + java.util.UUID.randomUUID() + "@example.com";
        var created = mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"displayName\":\"Before rollback\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        var userId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        var id = ((Number) userId).longValue();
        userService.findById(id);

        transactionTemplate.executeWithoutResult(status -> {
            userService.update(id, new UpdateUserRequest(email, "Rolled back", 0));
            status.setRollbackOnly();
        });

        assertEquals("Before rollback", userService.findById(id).displayName());
    }

    @Test
    void cachesUserResponsesAndEvictsThemAfterServiceUpdate() throws Exception {
        var email = "cache-" + java.util.UUID.randomUUID() + "@example.com";
        var created = mvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"displayName\":\"Initial\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        var userId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Initial"));

        var entity = userMapper.selectById((Number) userId);
        entity.setDisplayName("Changed outside cache");
        userMapper.updateById(entity);

        mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Initial"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/{id}", userId)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email
                                + "\",\"displayName\":\"Updated through service\",\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        mvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated through service"));
    }
}
