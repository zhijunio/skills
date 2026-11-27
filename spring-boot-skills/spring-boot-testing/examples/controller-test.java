// GOOD — controller slice: only the web layer, collaborators mocked with @MockitoBean.
// When Spring Security filters are active and CSRF is enabled, writes need .with(csrf());
// @WithMockUser only authenticates and does not provide a CSRF token.
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService service;
}
