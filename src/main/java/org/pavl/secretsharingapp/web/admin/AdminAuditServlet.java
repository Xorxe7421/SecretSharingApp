package org.pavl.secretsharingapp.web.admin;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pavl.secretsharingapp.db.AuditLog;
import org.pavl.secretsharingapp.repository.AuditLogRepository;
import org.pavl.secretsharingapp.util.ServletUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "AdminAuditServlet", value = "/api/admin/audit")
public class AdminAuditServlet extends HttpServlet {

    private AuditLogRepository auditLogRepository;

    @Override
    public void init(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        auditLogRepository = new AuditLogRepository(entityManagerFactory);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<AuditLog> auditLogs = auditLogRepository.getAll();
        List<Map<String, Object>> responseMap = auditLogs.stream().map(auditLog -> {
            Map<String, Object> result = new HashMap<>();

            result.put("timestamp", auditLog.getTimestamp());
            result.put("actionType", auditLog.getActionType());
            result.put("secretAccessTokenHash", auditLog.getSecretAccessTokenHash());

            return result;
        }).toList();
        ServletUtils.sendResponse(response, responseMap);
    }
}
