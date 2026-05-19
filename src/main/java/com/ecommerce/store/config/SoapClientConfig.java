package com.ecommerce.store.config;

import com.ecommerce.store.middleware.SoapAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class SoapClientConfig {

    @Value("${soap.auth.url}")
    private String soapAuthUrl;

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.ecommerce.store.middleware");
        return marshaller;
    }

    @Bean
    public SoapAuthClient soapAuthClient(Jaxb2Marshaller marshaller) {
        SoapAuthClient client = new SoapAuthClient();
        client.setDefaultUri(soapAuthUrl);
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}
