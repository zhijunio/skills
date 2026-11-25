package io.example.templatesecurity.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PartnerInventoryClient {

    private static final String CLIENT_REGISTRATION_ID = "partner-inventory";

    private final RestClient restClient;
    private final String baseUrl;

    public PartnerInventoryClient(
            RestClient partnerRestClient, @Value("${partners.inventory.base-url}") String baseUrl) {
        this.restClient = partnerRestClient;
        this.baseUrl = baseUrl;
    }

    public InventoryStock getStock() {
        return restClient
                .get()
                .uri(baseUrl + "/api/stock")
                .attributes(RequestAttributeClientRegistrationIdResolver.clientRegistrationId(CLIENT_REGISTRATION_ID))
                .attributes(RequestAttributePrincipalResolver.principal("template-security"))
                .retrieve()
                .body(InventoryStock.class);
    }

    public record InventoryStock(String sku, int available) {}
}
