package org.pavl.secretsharingapp.web;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
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
import java.util.Map;

@WebServlet(name = "registerServlet", value = "/api/keys/register")
public class RegisterServlet extends HttpServlet {

    private ApiKeyRepository apiKeyRepository;
    private AuditLogRepository auditLogRepository;
    private Map<Tier, Integer> tierRateLimitMap;

    @Override
    public void init(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();

        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        apiKeyRepository = new ApiKeyRepository(entityManagerFactory);
        auditLogRepository = new AuditLogRepository(entityManagerFactory);

        tierRateLimitMap = (Map<Tier, Integer>) servletContext.getAttribute("tierRateLimitMapping");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ownerName = getOwnerName(request, response);

        if (ownerName == null) {
            return;
        }

        String apiKeyString = CryptoUtils.generateApiKey();
        String apiKeyStringHash = CryptoUtils.getHash(apiKeyString);

        ApiKey apiKey = ApiKey
                .builder()
                .keyHash(apiKeyStringHash)
                .ownerName(ownerName)
                .tier(Tier.FREE)
                .isActive(true)
                .build();

        apiKeyRepository.save(apiKey);
        auditLogRepository.save(
                AuditLog
                        .builder()
                        .timestamp(LocalDateTime.now())
                        .actionType(ActionType.API_KEY_CREATED)
                        .apiKeyHash(apiKeyStringHash)
                        .build()
        );

        Map<String, Object> responseMap = Map.of(
                "apiKey", apiKeyString,
                "tier", Tier.FREE.name(),
                "rateLimit", tierRateLimitMap.get(Tier.FREE)
        );

        ServletUtils.sendResponse(response, responseMap);
    }

    private String getOwnerName(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> requestJsonMap = ServletUtils.getRequestJsonMap(request);

        if (requestJsonMap == null) {
            ServletUtils.sendInvalidJsonErrorResponse(response);
            return null;
        }

        if (!requestJsonMap.containsKey("ownerName")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, Object> errorResponseMap = Map.of("message", "Owner name not provided");
            ServletUtils.sendResponse(response, errorResponseMap);
            return null;
        }

        return (String) requestJsonMap.get("ownerName");
    }
}