package org.pavl.secretsharingapp.repository;

import jakarta.persistence.*;
import org.pavl.secretsharingapp.domain.ActionType;
import org.pavl.secretsharingapp.db.AuditLog;
import org.pavl.secretsharingapp.db.Secret;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretRepository extends GenericRepository<Secret> {

    public SecretRepository(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public Secret findByAccessTokenHash(String accessTokenHash) {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        Secret secret;
        try {
            entityTransaction.begin();
            TypedQuery<Secret> query = entityManager.createQuery(
                    "SELECT s FROM Secret s WHERE s.accessTokenHash = :accessTokenHash",
                    Secret.class
            );
            secret = query
                    .setParameter("accessTokenHash", accessTokenHash)
                    .getSingleResult();

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
        return secret;
    }

    public void deleteExpired() {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        try {
            entityTransaction.begin();

            TypedQuery<Secret> query = entityManager
                    .createQuery("SELECT s FROM Secret s WHERE s.expirationTime <= :now", Secret.class);

            List<Secret> secrets = query
                    .setParameter("now", LocalDateTime.now())
                    .getResultList();

            for (Secret s : secrets) {
                entityManager.persist(
                        AuditLog
                                .builder()
                                .timestamp(LocalDateTime.now())
                                .actionType(ActionType.SECRET_EXPIRED)
                                .secretAccessTokenHash(s.getAccessTokenHash())
                                .build()
                );

                entityManager.remove(s);
            }

            entityTransaction.commit();
        } catch (Exception e) {
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            throw new RuntimeException("Deletion failed", e);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
