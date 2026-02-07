package org.pavl.secretsharingapp.web.admin;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pavl.secretsharingapp.domain.Tier;
import org.pavl.secretsharingapp.db.ApiKey;
import org.pavl.secretsharingapp.repository.ApiKeyRepository;
import org.pavl.secretsharingapp.util.CryptoUtils;
import org.pavl.secretsharingapp.util.ServletUtils;

import java.io.IOException;
import java.util.*;

@WebServlet(name = "AdminKeysServlet", value = "/api/admin/keys")
public class AdminKeysServlet extends HttpServlet {

    private ApiKeyRepository apiKeyRepository;
    private Map<Tier, Integer> tierRateLimitMap;

    @Override
    public void init(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        apiKeyRepository = new ApiKeyRepository(entityManagerFactory);

        tierRateLimitMap = (Map<Tier, Integer>) servletContext.getAttribute("tierRateLimitMapping");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<ApiKey> apiKeys = apiKeyRepository.findAll();
        List<Map<String, Object>> responseMap = apiKeys.stream().map(apiKey -> {
            Map<String, Object> result = new HashMap<>();

            result.put("id", apiKey.getId());
            result.put("keyHash", apiKey.getKeyHash());
            result.put("ownerName", apiKey.getOwnerName());
            result.put("tier", apiKey.getTier().name());
            result.put("isActive", apiKey.isActive());

            return result;
        }).toList();
        ServletUtils.sendResponse(response, responseMap);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> requestMap = ServletUtils.getRequestJsonMap(request);

        if (requestMap == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid json input");
            return;
        }

        String ownerName = (String) requestMap.get("ownerName");
        if (ownerName == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ownerName not specified");
            return;
        }

        String tierAsString = (String) requestMap.get("tier");
        if (tierAsString == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "tier not specified");
            return;
        }

        Tier tier;
        try {
            tier = Tier.valueOf(tierAsString);
        } catch (IllegalArgumentException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid tier");
            return;
        }

        if (!requestMap.containsKey("isActive")) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "isActive field not specified");
            return;
        }

        boolean isActive;
        try {
            isActive = (boolean) requestMap.get("isActive");
        } catch (ClassCastException e) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid isActive field");
            return;
        }

        String apiKeyString = CryptoUtils.generateApiKey();
        String apiKeyStringHash = CryptoUtils.getHash(apiKeyString);

        ApiKey apiKey = ApiKey
                .builder()
                .ownerName(ownerName)
                .tier(tier)
                .isActive(isActive)
                .keyHash(apiKeyStringHash)
                .build();

        apiKeyRepository.save(apiKey);

        Map<String, Object> responseMap = Map.of(
                "apiKey", apiKeyString,
                "tier", tier.name(),
                "rateLimit", tierRateLimitMap.get(tier)
        );

        ServletUtils.sendResponse(response, responseMap);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String apiKeyId = request.getParameter("id");

        if (apiKeyId == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "id not provided");
            return;
        }

        ApiKey apiKey = apiKeyRepository.findById(UUID.fromString(apiKeyId));
        if (apiKey == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid api key");
            return;
        }

        Map<String, Object> requestMap = ServletUtils.getRequestJsonMap(request);
        if (requestMap == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid json input");
            return;
        }

        String newTierAsString = (String) requestMap.get("tier");
        if (newTierAsString != null) {
            Tier newTier;
            try {
                newTier = Tier.valueOf(newTierAsString);
            } catch (IllegalArgumentException e) {
                ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid tier value");
                return;
            }
            apiKey.setTier(newTier);
        }

        if (requestMap.containsKey("isActive")) {
            boolean isActive;
            try {
                isActive = (boolean) requestMap.get("isActive");
            } catch (ClassCastException e) {
                ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid isActive field");
                return;
            }

            apiKey.setActive(isActive);
        }

        apiKeyRepository.save(apiKey);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String apiKeyId = request.getParameter("id");

        if (apiKeyId == null) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "id not provided");
            return;
        }

        ApiKey apiKey = apiKeyRepository.findById(UUID.fromString(apiKeyId));
        apiKeyRepository.deleteById(apiKey.getId());
    }
}
