package org.pavl.secretsharingapp.web.Filter;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pavl.secretsharingapp.db.AuditLog;
import org.pavl.secretsharingapp.domain.ActionType;
import org.pavl.secretsharingapp.domain.Tier;
import org.pavl.secretsharingapp.db.ApiKey;
import org.pavl.secretsharingapp.repository.ApiKeyRepository;
import org.pavl.secretsharingapp.repository.AuditLogRepository;
import org.pavl.secretsharingapp.util.CryptoUtils;
import org.pavl.secretsharingapp.util.ServletUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class SecurityFilter extends HttpFilter {

    private static final String X_API_KEY_HEADER = "X-API-Key";
    private ApiKeyRepository apiKeyRepository;
    private AuditLogRepository auditLogRepository;

    @Override
    public void init(FilterConfig filterConfig) {
        ServletContext servletContext = filterConfig.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        apiKeyRepository = new ApiKeyRepository(entityManagerFactory);
        auditLogRepository = new AuditLogRepository(entityManagerFactory);
    }

    @Override
    public void doFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        if (Objects.equals(request.getRequestURI(), "/api/secrets")
                && Objects.equals(request.getMethod(), "GET")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(X_API_KEY_HEADER);
        if (apiKey == null) {
            saveAuthFailedAuditLog(null);
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Api key not provided");
            return;
        }

        String hashedApiKey = CryptoUtils.getHash(apiKey);
        ApiKey apiKeyEntity = apiKeyRepository.findByHash(hashedApiKey);

        if (apiKeyEntity == null || !apiKeyEntity.isActive()) {
            saveAuthFailedAuditLog(hashedApiKey);
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Invalid api key");
            return;
        }

        if (shouldRestrictAdminEndpoint(request, apiKeyEntity)) {
            saveAuthFailedAuditLog(hashedApiKey);
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Admin endpoint restricted");
            return;
        }

        request.setAttribute("apiKey", apiKeyEntity);
        chain.doFilter(request, response);
    }

    private boolean shouldRestrictAdminEndpoint(HttpServletRequest request, ApiKey apiKey) {
        return apiKey.getTier() != Tier.ADMIN
                && request.getRequestURI().startsWith("/api/admin");
    }

    private void saveAuthFailedAuditLog(String apiKeyHash) {
        auditLogRepository.save(
                AuditLog
                        .builder()
                        .timestamp(LocalDateTime.now())
                        .actionType(ActionType.AUTH_FAILED)
                        .apiKeyHash(apiKeyHash)
                        .build()
        );
    }
}
