package org.pavl.secretsharingapp.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pavl.secretsharingapp.db.Secret;
import org.pavl.secretsharingapp.repository.SecretRepository;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.Map;

public class ServletUtils {

    private static final ObjectMapper objectMapper = createObjectMapper();
    private static final String NO_ACCESS_TOKEN = "Access token not provided";
    private static final String SECRET_NOT_FOUND = "Secret related to provided access token wasn't found";
    private static final String SECRET_EXPIRED = "Secret is expired";

    public static Secret getSecret(HttpServletRequest request, HttpServletResponse response, SecretRepository secretRepository) throws IOException {
        String accessToken = request.getParameter("accessToken");

        if (accessToken == null) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, NO_ACCESS_TOKEN);
            return null;
        }

        String accessTokenHash = CryptoUtils.getHash(accessToken);
        Secret secret = secretRepository.findByAccessTokenHash(accessTokenHash);

        if (secret == null) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, SECRET_NOT_FOUND);
            return null;
        }

        if (secret.getExpirationTime().isBefore(LocalDateTime.now())) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, SECRET_EXPIRED);
            return null;
        }

        return secret;
    }

    public static Map<String, Object> getRequestJsonMap(HttpServletRequest request) throws IOException {
        Reader reader = request.getReader();
        String requestBody = reader.readAllAsString();

        Map<String, Object> requestMap;

        try {
            requestMap = objectMapper.readValue(requestBody, new TypeReference<>() {});
        } catch (JsonMappingException | JsonParseException e) {
            return null;
        }

        if (ServletUtils.isNestedJson(requestMap)) {
            return null;
        }

        return requestMap;
    }

    public static boolean isNestedJson(Map<String, Object> jsonMap) {
        return jsonMap.entrySet().stream().anyMatch(entry -> entry.getValue() instanceof Map);
    }

    public static void sendInvalidJsonErrorResponse(HttpServletResponse response) throws IOException {
        sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid json input");
    }

    public static void sendResponse(HttpServletResponse response, Object responseObject) throws IOException {
        String jsonResponse;
        try {
            jsonResponse = objectMapper.writeValueAsString(responseObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        response.setContentType("application/json");
        response.getWriter().write(jsonResponse);
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    public static void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> errorResponseMap = Map.of("message", message);
        sendResponse(response, errorResponseMap);
    }
}
