package org.pavl.secretsharingapp.web.Filter;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pavl.secretsharingapp.db.ApiKey;
import org.pavl.secretsharingapp.db.AuditLog;
import org.pavl.secretsharingapp.domain.ActionType;
import org.pavl.secretsharingapp.domain.Tier;
import org.pavl.secretsharingapp.repository.AuditLogRepository;
import org.pavl.secretsharingapp.util.ServletUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

public class RateLimitFilter extends HttpFilter {

    private AuditLogRepository auditLogRepository;

    private static final int TOO_MANY_REQUESTS_STATUS_CODE = 429;
    private Duration rateLimitDuration;
    private Map<Tier, Integer> tierRateLimitMap;

    @Override
    public void init(FilterConfig config) throws ServletException {
        ServletContext servletContext = config.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        auditLogRepository = new AuditLogRepository(entityManagerFactory);

        rateLimitDuration = (Duration) servletContext.getAttribute("rateLimitDuration");
        tierRateLimitMap = (Map<Tier, Integer>) servletContext.getAttribute("tierRateLimitMapping");
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (Objects.equals(request.getMethod(), "GET")) {
            chain.doFilter(request, response);
            return;
        }

        ApiKey apiKey = (ApiKey) request.getAttribute("apiKey");
        if (apiKey.getTier() == Tier.ADMIN) {
            chain.doFilter(request, response);
            return;
        }

        if (isRateLimitReached(apiKey)) {
            auditLogRepository.save(
                    AuditLog
                            .builder()
                            .timestamp(LocalDateTime.now())
                            .actionType(ActionType.RATE_LIMIT_HIT)
                            .apiKeyHash(apiKey.getKeyHash())
                            .build()
            );
            ServletUtils.sendErrorResponse(response, TOO_MANY_REQUESTS_STATUS_CODE, "Rate limit reached, try again later");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimitReached(ApiKey apiKey) {
        int eventCount = auditLogRepository.getEventCount(apiKey, rateLimitDuration);
        int rateLimit = tierRateLimitMap.get(apiKey.getTier());

        return eventCount + 1 == rateLimit;
    }
}
