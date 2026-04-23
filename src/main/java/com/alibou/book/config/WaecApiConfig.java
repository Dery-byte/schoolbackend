package com.alibou.book.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class WaecApiConfig {

    private static final String AUTH_TOKEN = "cmtncndyZ206UHpxUTJQcURNRg==";

    @Bean
    public RestTemplate waecApiRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Add authorization header interceptor
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("Authorization", "Basic " + AUTH_TOKEN);
            return execution.execute(request, body);
        });

        // Add error handler
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().is4xxClientError() ||
                        response.getStatusCode().is5xxServerError();
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                System.out.println("Error from WAEC API:");
                System.out.println("Status Code: " + response.getStatusCode().value());
                System.out.println("Headers: " + response.getHeaders());
                System.out.println("Body: " + new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
            }
        });

        return restTemplate;
    }
}