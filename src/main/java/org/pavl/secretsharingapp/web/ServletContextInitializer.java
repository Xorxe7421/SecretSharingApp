package org.pavl.secretsharingapp.web;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.pavl.secretsharingapp.domain.Tier;
import org.pavl.secretsharingapp.repository.SecretRepository;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@WebListener
public class ServletContextInitializer implements ServletContextListener {

    private final ScheduledExecutorService scheduler;

    public ServletContextInitializer() {
        scheduler = new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String masterKey = System.getenv("SECRET_SHARING_APP_MASTER_KEY");
        ServletContext servletContext = sce.getServletContext();
        servletContext.setAttribute("masterKey", masterKey);

        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("myPU");
        SecretRepository secretRepository = new SecretRepository(entityManagerFactory);
        scheduler.scheduleAtFixedRate(secretRepository::deleteExpired, 0L, 10, TimeUnit.MINUTES);

        servletContext.setAttribute("entityManagerFactory", entityManagerFactory);
        servletContext.setAttribute("validatorFactory", Validation.buildDefaultValidatorFactory());

        servletContext.setAttribute(
                "tierRateLimitMapping",
                Map.of(
                        Tier.FREE, 20,
                        Tier.PREMIUM, 300
                )
        );

        servletContext.setAttribute("rateLimitDuration", Duration.ofMinutes(1));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        EntityManagerFactory entityManagerFactory = (EntityManagerFactory) servletContext.getAttribute("entityManagerFactory");
        entityManagerFactory.close();

        ValidatorFactory validatorFactory = (ValidatorFactory) servletContext.getAttribute("validatorFactory");
        validatorFactory.close();

        scheduler.close();
    }
}
