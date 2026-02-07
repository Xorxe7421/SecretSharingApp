package org.pavl.secretsharingapp.web.admin;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pavl.secretsharingapp.repository.StatsRepository;
import org.pavl.secretsharingapp.util.ServletUtils;

import java.io.IOException;
import java.util.Map;

@WebServlet(name = "AdminStatsServlet", value = "/api/admin/stats")
public class AdminStatsServlet extends HttpServlet {

    private StatsRepository statsRepository;

    @Override
    public void init(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        statsRepository = new StatsRepository(entityManagerFactory);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletUtils.sendResponse(
                response,
                Map.of(
                        "totalSecrets", statsRepository.getTotalSecrets(),
                        "activeKeys", statsRepository.getActiveKeys(),
                        "nonActiveKeys", statsRepository.getNonActiveKeys()
                )
        );
    }
}
