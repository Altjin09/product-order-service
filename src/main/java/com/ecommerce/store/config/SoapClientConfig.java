package com.ecommerce.store.config;

import com.ecommerce.store.middleware.SoapAuthClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

@Configuration
public class SoapClientConfig {

    @Value("${soap.auth.url}")
    private String soapAuthUrl;

    @Bean
    public SoapAuthClient soapAuthClient() {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setSocketTimeout(10000)
            .build();

        HttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(config)
            .build();

        HttpComponentsMessageSender sender = new HttpComponentsMessageSender(httpClient);

        SoapAuthClient client = new SoapAuthClient();
        client.setDefaultUri(soapAuthUrl);
        client.setMessageSender(sender);
        return client;
    }
}