// Illustrative MVC slice: web mapping with a mocked collaborator.
// For authorization assertions include the application's real security configuration.
// When Spring Security filters are active and CSRF is enabled, writes need .with(csrf());
// @WithMockUser only authenticates and does not provide a CSRF token.
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService service;
}
