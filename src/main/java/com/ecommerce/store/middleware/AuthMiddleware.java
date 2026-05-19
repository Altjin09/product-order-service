package com.ecommerce.store.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Lab06: Authentication Middleware
 * Every request (GET, POST, PUT, DELETE) goes through here.
 * Flow: Request -> Middleware -> SOAP ValidateToken -> allow/deny
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AuthMiddleware implements Filter {

    private final SoapAuthClient soapAuthClient;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        // Public endpoints - no auth required
        if (isPublicEndpoint(path, request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {}", path);
            sendUnauthorized(response, "Missing token");
            return;
        }

        String token = authHeader.substring(7);

        // Call SOAP Auth Service to validate (Lab06 core flow)
        SoapAuthClient.TokenValidationResult result = soapAuthClient.validateToken(token);

        if (!result.valid()) {
            log.warn("Invalid token for path: {}", path);
            sendUnauthorized(response, "Invalid or expired token");
            return;
        }

        // Lab06 Bonus RBAC: Check role-based access
        if (!hasPermission(result.role(), request.getMethod(), path)) {
            log.warn("Forbidden: user {} (role={}) tried {} {}", result.username(), result.role(), request.getMethod(), path);
            sendForbidden(response, "Insufficient permissions");
            return;
        }

        // Pass user info downstream via headers
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Id".equals(name)) return result.userId();
                if ("X-Username".equals(name)) return result.username();
                if ("X-User-Role".equals(name)) return result.role();
                return super.getHeader(name);
            }
        };

        log.debug("Auth passed: {} {} [user={}]", request.getMethod(), path, result.username());
        chain.doFilter(wrapper, res);
    }

    private boolean isPublicEndpoint(String path, String method) {
        // GET /products is public (browsing) - others require auth
        return "GET".equals(method) && path.startsWith("/products");
    }

    /**
     * Lab06 Bonus RBAC rules:
     * CUSTOMER: can read products, place orders, view own orders
     * SELLER:   can create/update/delete own products
     * ADMIN:    full access
     */
    private boolean hasPermission(String role, String method, String path) {
        if ("ADMIN".equals(role)) return true;

        if (path.startsWith("/products")) {
            if ("SELLER".equals(role)) return true;
            // CUSTOMER: read-only on products
            return "GET".equals(method);
        }

        if (path.startsWith("/orders")) {
            // Both CUSTOMER and SELLER can interact with orders
            return "CUSTOMER".equals(role) || "SELLER".equals(role);
        }

        return false;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
