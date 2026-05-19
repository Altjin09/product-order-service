package com.ecommerce.store.middleware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

@Component
@Slf4j
public class SoapAuthClient extends WebServiceGatewaySupport {

    private static final String NS = "http://ecommerce.com/auth";

    @Value("${soap.auth.url}")
    private String soapAuthUrl;

    public TokenValidationResult validateToken(String token) {
        try {
            String requestXml =
                "<ns:ValidateTokenRequest xmlns:ns=\"" + NS + "\">" +
                "<ns:token>" + token + "</ns:token>" +
                "</ns:ValidateTokenRequest>";

            StringSource source = new StringSource(requestXml);
            StringResult result = new StringResult();

            getWebServiceTemplate().sendSourceAndReceiveToResult(soapAuthUrl, source, result);

            String response = result.toString();
            log.debug("SOAP response: {}", response);

            boolean valid = response.contains("<valid>true</valid>")
                    || response.contains("<ns2:valid>true</ns2:valid>");
            String username = extractTag(response, "username");
            String role = extractTag(response, "role");
            String userId = extractTag(response, "userId");

            return new TokenValidationResult(valid, username, role, userId);

        } catch (Exception e) {
            log.error("SOAP ValidateToken failed: {}", e.getMessage());
            return new TokenValidationResult(false, null, null, null);
        }
    }

    private String extractTag(String xml, String tag) {
        String[] patterns = {"<" + tag + ">", "<ns2:" + tag + ">", "<ns:" + tag + ">"};
        for (String open : patterns) {
            int start = xml.indexOf(open);
            if (start != -1) {
                start += open.length();
                int end = xml.indexOf("<", start);
                if (end != -1) return xml.substring(start, end);
            }
        }
        return null;
    }

    public record TokenValidationResult(boolean valid, String username, String role, String userId) {}
}