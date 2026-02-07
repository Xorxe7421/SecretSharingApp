package org.pavl.secretsharingapp.repository;

import jakarta.persistence.*;
import org.pavl.secretsharingapp.db.ApiKey;

import java.util.HashMap;
import java.util.Map;

public class StatsRepository {

    protected final EntityManagerFactory entityManagerFactory;

    public StatsRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public int getTotalSecrets() {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        int result;
        try {
            entityTransaction.begin();
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT count(*) FROM Secret s",
                    Long.class
            );
            result = query.getSingleResult().intValue();

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
        return result;
    }

    public int getActiveKeys() {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        int result;
        try {
            entityTransaction.begin();
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT count(*) FROM ApiKey a where a.isActive = true",
                    Long.class
            );
            result = query.getSingleResult().intValue();

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
        return result;
    }

    public int getNonActiveKeys() {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        int result;
        try {
            entityTransaction.begin();
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT count(*) FROM ApiKey a where a.isActive = false",
                    Long.class
            );
            result = query.getSingleResult().intValue();

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
        return result;
    }
}
