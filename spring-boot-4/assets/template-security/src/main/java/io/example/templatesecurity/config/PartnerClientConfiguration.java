package io.example.templatesecurity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.RequestAttributePrincipalResolver;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class PartnerClientConfiguration {

    @Bean
    RestClient partnerRestClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        var oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2Interceptor.setPrincipalResolver(new RequestAttributePrincipalResolver());
        return RestClient.builder().requestInterceptor(oauth2Interceptor).build();
    }
}
