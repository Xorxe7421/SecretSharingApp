package org.pavl.secretsharingapp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.HashMap;
import java.util.Map;

public abstract class GenericRepository<T> {

    protected final EntityManagerFactory entityManagerFactory;

    protected GenericRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void save(T entity) {
        if (entity == null) {
            return;
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        try {
            entityTransaction.begin();

            Object id = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
            if (id == null) {
                entityManager.persist(entity);
            } else {
                entityManager.merge(entity);
            }

            entityTransaction.commit();
        } catch (Exception e) {
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            throw new RuntimeException("Save failed", e);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    public void delete(T entity) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        try {
            entityTransaction.begin();
            Object id = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(entity);
            T managedEntity = entityManager.getReference((Class<T>) entity.getClass(), id);
            entityManager.remove(managedEntity);
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
