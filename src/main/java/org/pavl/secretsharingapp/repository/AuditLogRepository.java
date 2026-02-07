package org.pavl.secretsharingapp.repository;

import jakarta.persistence.*;
import org.pavl.secretsharingapp.db.ApiKey;
import org.pavl.secretsharingapp.db.AuditLog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditLogRepository extends GenericRepository<AuditLog> {

    public AuditLogRepository(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public List<AuditLog> getAll() {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        List<AuditLog> auditLogs;
        try {
            entityTransaction.begin();
            TypedQuery<AuditLog> query = entityManager.createQuery(
                    "SELECT a FROM AuditLog a",
                    AuditLog.class
            );
            auditLogs = query.getResultList();

            entityTransaction.commit();
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            throw new RuntimeException("Operation failed", e);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return auditLogs;
    }

    public int getEventCount(ApiKey apiKey, Duration duration) {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        int eventCount;
        LocalDateTime targetTime = LocalDateTime.now().minus(duration);
        try {
            entityTransaction.begin();
            TypedQuery<Long> query = entityManager.createQuery(
                    """
                        SELECT count(*)
                        FROM AuditLog a
                        where a.timestamp >= :targetTime
                            and a.apiKeyHash = :apiKeyHash
                            and (a.actionType = 'SECRET_CREATED' or a.actionType = 'SECRET_REMOVED')""",
                    Long.class
            );
            eventCount = query
                    .setParameter("targetTime", targetTime)
                    .setParameter("apiKeyHash", apiKey.getKeyHash())
                    .getSingleResult()
                    .intValue();

            entityTransaction.commit();
        } catch (Exception e) {
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            throw new RuntimeException("Operation failed", e);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return eventCount;
    }
}
