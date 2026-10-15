package com.example.sb4oauth2login.oauth;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class OAuthEndpointChecker {
    private final RestClient client;

    OAuthEndpointChecker() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().requestFactory(requests).build();
    }

    OAuthEndpointCheck check(String name, String uri) {
        if (uri == null || uri.isBlank()) {
            return new OAuthEndpointCheck(name, true, null, "未配置，已跳过");
        }
        try {
            int status = client.get()
                    .uri(uri)
                    .exchange((request, response) -> response.getStatusCode().value());
            boolean reachable = status < 500;
            return new OAuthEndpointCheck(name, reachable, status, reachable ? "端点已响应" : "端点返回服务端错误");
        } catch (RestClientException exception) {
            return new OAuthEndpointCheck(name, false, null, "连接失败或超时");
        }
    }
}
