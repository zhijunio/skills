# MVC testing

Inspect the actual controller, DTO, advice and security configuration. Use a slice when HTTP
mapping is the target and a mocked collaborator is sufficient. If policy is under test, include
the application's applicable security configuration and method-security boundary; do not assume
an auto-configured default filter chain is the application's chain. Import required collaborators
or use a full-context test where the real policy cannot be exercised in a slice. The following
fragment tests HTTP mapping, not the full security contract. See [security testing](security-testing.md).

## Slice Tests — Controllers (@WebMvcTest)

```java
// When Spring Security is on the test classpath and its filters are enabled,
// POSTs need .with(csrf()) if CSRF protection is enabled; @WithMockUser only authenticates.
// This fragment illustrates HTTP mapping; import the actual security configuration
// and supporting beans when testing application-specific authorization.
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@WebMvcTest(OrderController.class)
@Import(ProblemDetailExceptionHandler.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired tools.jackson.databind.json.JsonMapper jsonMapper;
    @MockitoBean OrderService orderService;

    @Test
    @WithMockUser(roles = "USER")
    void createOrder_withValidRequest_shouldReturn201() throws Exception {
        // Given
        var request = new CreateOrderRequest("user@example.com",
            List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 1))); // items are @NotEmpty
        var order = Order.create("user@example.com");
        when(orderService.createOrder(any())).thenReturn(order);

        // When / Then
        mockMvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerEmail").value("user@example.com"));
    }

    @Test
    @WithMockUser
    void createOrder_withInvalidRequest_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")) // missing required fields
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        verifyNoInteractions(orderService);
    }
}
```

Use the focused [controller slice example](../examples/controller-test.java) for MVC behavior.
The [full-context controller test](../examples/controller-test-full-context.java) illustrates
the extra startup and fixture coupling to avoid for a simple endpoint.

The response shape and error code are illustrative, not a mandate to change an existing API.
Use @MockitoBean/@MockitoSpyBean where supported by the actual framework; @MockBean is removed
in Boot 4. Authentication with @WithMockUser is not token decoding or proof of ownership.
