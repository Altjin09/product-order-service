package com.ecommerce.store.config;

import com.ecommerce.store.middleware.SoapAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
public class SoapClientConfig {

    @Value("${soap.auth.url}")
    private String soapAuthUrl;

    @Bean
    public SoapAuthClient soapAuthClient() {
        SoapAuthClient client = new SoapAuthClient();
        client.setDefaultUri(soapAuthUrl);
        return client;
    }
}