package org.pavl.secretsharingapp.repository;

import jakarta.persistence.*;
import org.pavl.secretsharingapp.db.ApiKey;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApiKeyRepository extends GenericRepository<ApiKey> {

    public ApiKeyRepository(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public ApiKey findByHash(String apiKeyHash) {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        ApiKey apiKey;
        try {
            entityTransaction.begin();
            TypedQuery<ApiKey> query = entityManager.createQuery(
                    "SELECT a FROM ApiKey a WHERE a.keyHash = :apiKeyHash",
                    ApiKey.class
            );
            apiKey = query
                    .setParameter("apiKeyHash", apiKeyHash)
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
        return apiKey;
    }

    public void deleteById(UUID id) {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        try {
            entityTransaction.begin();
            entityManager.createQuery(
                    "DELETE FROM ApiKey a WHERE a.id = :apiKeyId"
            )
                    .setParameter("apiKeyId", id)
                    .executeUpdate();

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
    }

    public List<ApiKey> findAll() {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        List<ApiKey> apiKeys;
        try {
            entityTransaction.begin();
            TypedQuery<ApiKey> query = entityManager.createQuery(
                    "SELECT a FROM ApiKey a",
                    ApiKey.class
            );
            apiKeys = query.getResultList();

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
        return apiKeys;
    }

    public ApiKey findById(UUID apiKeyId) {
        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.validation.mode", "NONE");

        EntityManager entityManager = entityManagerFactory.createEntityManager(props);
        EntityTransaction entityTransaction = entityManager.getTransaction();

        ApiKey apiKey;
        try {
            entityTransaction.begin();
            TypedQuery<ApiKey> query = entityManager.createQuery(
                    "SELECT a FROM ApiKey a WHERE a.id = :apiKeyId",
                    ApiKey.class
            );
            apiKey = query
                    .setParameter("apiKeyId", apiKeyId)
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
        return apiKey;
    }
}
