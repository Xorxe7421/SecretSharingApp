package org.pavl.secretsharingapp.web;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.pavl.secretsharingapp.db.ApiKey;
import org.pavl.secretsharingapp.domain.ActionType;
import org.pavl.secretsharingapp.db.AuditLog;
import org.pavl.secretsharingapp.domain.Tier;
import org.pavl.secretsharingapp.repository.AuditLogRepository;
import org.pavl.secretsharingapp.util.CryptoUtils;
import org.pavl.secretsharingapp.db.Secret;
import org.pavl.secretsharingapp.repository.SecretRepository;
import org.pavl.secretsharingapp.util.ServletUtils;

@WebServlet(name = "SecretServlet", value = "/api/secrets")
public class SecretServlet extends HttpServlet {

    private String masterKey;
    private SecretRepository secretRepository;
    private AuditLogRepository auditLogRepository;
    private Validator validator;

    @Override
    public void init(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");

        this.secretRepository = new SecretRepository(entityManagerFactory);
        this.auditLogRepository = new AuditLogRepository(entityManagerFactory);
        this.masterKey = (String) servletContext.getAttribute("masterKey");

        ValidatorFactory validatorFactory = (ValidatorFactory) servletContext.getAttribute("validatorFactory");
        this.validator = validatorFactory.getValidator();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Secret secret = ServletUtils.getSecret(request, response, secretRepository);

        if (secret == null) {
            return;
        }

        if (secret.getViewCount() != null) {
            if (secret.getViewCount() == 1) {
                auditLogRepository.save(
                        AuditLog
                                .builder()
                                .timestamp(LocalDateTime.now())
                                .actionType(ActionType.SECRET_BURNED)
                                .secretAccessTokenHash(secret.getAccessTokenHash())
                                .build()
                );

                secretRepository.delete(secret);
                secret.setViewCount(secret.getViewCount() - 1);
            }else {
                secret.setViewCount(secret.getViewCount() - 1);
                secretRepository.save(secret);

                auditLogRepository.save(
                        AuditLog
                                .builder()
                                .timestamp(LocalDateTime.now())
                                .actionType(ActionType.SECRET_VIEWED)
                                .secretAccessTokenHash(secret.getAccessTokenHash())
                                .build()
                );
            }
        }

        Map<String, Object> responseMap = createResponseMap(secret);

        ServletUtils.sendResponse(response, responseMap);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Secret secret = createSecret(request);

        if (secret == null) {
            ServletUtils.sendInvalidJsonErrorResponse(response);
            return;
        }

        String accessToken = CryptoUtils.generateAccessToken();
        String accessTokenHash = CryptoUtils.getHash(accessToken);
        secret.setAccessTokenHash(accessTokenHash);
        secret.setApiKey((ApiKey) request.getAttribute("apiKey"));

        Set<ConstraintViolation<Secret>> constraintViolations = validator.validate(secret);

        if (!constraintViolations.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, Object> errorResponseMap = Map.of(
                    "errors",
                    constraintViolations
                            .stream()
                            .map(a -> a.getPropertyPath().iterator().next().getName() + " - " + a.getMessage())
                            .toList()
            );
            ServletUtils.sendResponse(response, errorResponseMap);
            return;
        }

        secretRepository.save(secret);
        auditLogRepository.save(
                AuditLog
                        .builder()
                        .timestamp(LocalDateTime.now())
                        .actionType(ActionType.SECRET_CREATED)
                        .secretAccessTokenHash(secret.getAccessTokenHash())
                        .apiKeyHash(((ApiKey) request.getAttribute("apiKey")).getKeyHash())
                        .build()
        );

        Map<String, Object> responseMap = Map.of("accessToken", accessToken);
        ServletUtils.sendResponse(response, responseMap);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Secret secret = ServletUtils.getSecret(request, response, secretRepository);

        if (secret == null) {
            return;
        }

        ApiKey apiKey = (ApiKey) request.getAttribute("apiKey");
        if (apiKey.getTier() != Tier.ADMIN && !apiKey.getId().equals(secret.getApiKey().getId())) {
            ServletUtils.sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        ActionType actionType = apiKey.getTier() == Tier.ADMIN ? ActionType.SECRET_PURGED : ActionType.SECRET_REMOVED;

        auditLogRepository.save(
                AuditLog
                        .builder()
                        .timestamp(LocalDateTime.now())
                        .actionType(actionType)
                        .secretAccessTokenHash(secret.getAccessTokenHash())
                        .apiKeyHash(apiKey.getKeyHash())
                        .build()
        );
        secretRepository.delete(secret);
    }

    private Map<String, Object> createResponseMap(Secret secret) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", CryptoUtils.decryptContent(secret.getEncryptedContent(), masterKey));

        Optional.ofNullable(secret.getExpirationTime()).ifPresent(expirationTime -> responseMap.put("expirationTime", expirationTime));
        Optional.ofNullable(secret.getViewCount()).ifPresent(viewCount -> responseMap.put("viewCount", viewCount));
        return responseMap;
    }

    private Secret createSecret(HttpServletRequest request) throws IOException {
        Map<String, Object> requestJsonMap = ServletUtils.getRequestJsonMap(request);

        if (requestJsonMap == null) {
            return null;
        }

        String content = (String) requestJsonMap.get("content");
        String encryptedContent = CryptoUtils.encryptContent(content, masterKey);

        LocalDateTime expirationTime = LocalDateTime.parse((String) requestJsonMap.get("expirationTime"));
        Integer viewCount = (Integer) requestJsonMap.get("viewCount");

        return Secret.builder()
                .encryptedContent(encryptedContent)
                .expirationTime(expirationTime)
                .viewCount(viewCount)
                .build();
    }
}