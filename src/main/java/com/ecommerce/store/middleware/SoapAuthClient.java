package com.ecommerce.store.middleware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * Lab06: SOAP Client
 * JSON Service -> SOAP ValidateToken -> allow/deny request
 * This is the service-to-service communication core of Lab06
 */
@Component
@Slf4j
public class SoapAuthClient extends WebServiceGatewaySupport {

    private static final String NS = "http://ecommerce.com/auth";

    @Value("${soap.auth.url}")
    private String soapAuthUrl;

    public TokenValidationResult validateToken(String token) {
        try {
            // Build SOAP XML request manually
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element request = doc.createElementNS(NS, "ValidateTokenRequest");
            Element tokenEl = doc.createElement("token");
            tokenEl.setTextContent(token);
            request.appendChild(tokenEl);
            doc.appendChild(request);

            // Send to SOAP Auth Service
            DOMSource source = new DOMSource(doc);
            DOMSource responseSource = (DOMSource) getWebServiceTemplate()
                    .sendSourceAndReceive(soapAuthUrl, source,
                            message -> {}, // no extra headers needed
                            (WebServiceMessage msg) -> {
                                try {
                                    return new DOMSource(
                                            ((org.springframework.ws.WebServiceMessage) msg)
                                                    .getPayloadSource() instanceof DOMSource ds ? ds.getNode() : null
                                    );
                                } catch (Exception e) {
                                    return null;
                                }
                            });

            // Parse response
            if (responseSource != null && responseSource.getNode() != null) {
                Node root = responseSource.getNode();
                boolean valid = getBooleanValue(root, "valid");
                String username = getTextValue(root, "username");
                String role = getTextValue(root, "role");
                String userId = getTextValue(root, "userId");
                return new TokenValidationResult(valid, username, role, userId);
            }

        } catch (Exception e) {
            log.error("SOAP ValidateToken call failed: {}", e.getMessage());
        }
        return new TokenValidationResult(false, null, null, null);
    }

    private boolean getBooleanValue(Node root, String tag) {
        NodeList list = ((Element) root).getElementsByTagName(tag);
        if (list.getLength() > 0) {
            return "true".equalsIgnoreCase(list.item(0).getTextContent());
        }
        return false;
    }

    private String getTextValue(Node root, String tag) {
        NodeList list = ((Element) root).getElementsByTagName(tag);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return null;
    }

    public record TokenValidationResult(boolean valid, String username, String role, String userId) {}
}
