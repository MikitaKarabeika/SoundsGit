package com.ape.friction.api.mok.repositories;

import com.ape.friction.api.common.AbstractEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Repository
@Transactional(propagation = Propagation.MANDATORY, transactionManager = "mokTransactionManager")
public class MOKRefreshRepository {

    @PersistenceContext(unitName = "mokPersistenceUnit")
    private EntityManager em;

    public void refresh (AbstractEntity entity) {
        em.refresh(em.merge(entity));
    };
}
