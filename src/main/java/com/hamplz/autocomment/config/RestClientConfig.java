package com.hamplz.autocomment.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer timeoutRestClientCustomizer(HttpClientProperties httpClientProperties) {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(httpClientProperties.connectTimeout());
            requestFactory.setReadTimeout(httpClientProperties.readTimeout());
            builder.requestFactory(requestFactory);
        };
    }
}
