package org.pavl.secretsharingapp.web;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pavl.secretsharingapp.db.Secret;
import org.pavl.secretsharingapp.repository.SecretRepository;
import org.pavl.secretsharingapp.util.ServletUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@WebServlet(name = "secretMetadataServlet", value = "/api/secrets/meta")
public class SecretMetadataServlet extends HttpServlet {

    private SecretRepository secretRepository;

    @Override
    public void init(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        this.secretRepository = new SecretRepository(entityManagerFactory);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Secret secret = ServletUtils.getSecret(request, response, secretRepository);

        if (secret == null) {
            return;
        }

        Map<String, Object> responseMap = createResponseMap(secret);
        ServletUtils.sendResponse(response, responseMap);
    }

    private Map<String, Object> createResponseMap(Secret secret) {
        Map<String, Object> result = new HashMap<>();

        Optional.ofNullable(secret.getExpirationTime())
                .ifPresent(expirationTime -> result.put("expiresAt", expirationTime));

        Optional.ofNullable(secret.getViewCount())
                .ifPresent(viewCount -> result.put("viewsRemaining", viewCount));

        return result;
    }
}
