package com.mysupply.phase4.services;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.state.ESuccess;
import com.helger.commons.timing.StopWatch;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.mysupply.phase4.domain.PeppolReportingItemWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@IsSPIImplementation
public class PeppolReportingService implements IPeppolReportingBackendSPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolReportingService.class);
    private final String instanceId = this.getClass().getName() + " - " + UUID.randomUUID();
    private final EntityManagerFactory entityManagerFactory;

    public PeppolReportingService() {
        LOGGER.debug("Created new instance of " + this.getClass().getName() + "with instanceId: " + this.instanceId);
        this.entityManagerFactory = Persistence.createEntityManagerFactory("persister");
    }

    public PeppolReportingService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return this.instanceId;
    }

    @Nonnull
    @Override
    public ESuccess initBackend(@Nonnull IConfig aConfig) {
        StopWatch sw = StopWatch.createdStarted();
        while (!isInitialized()) {
            if (sw.getMillis() > 30000) {
                LOGGER.error("The backend could not be initialized within 30 seconds");
                return ESuccess.FAILURE;
            }
        }
        LOGGER.info("The backend was initialized successfully in " + sw.stopAndGetMillis() + " ms");
        return ESuccess.SUCCESS;
    }

    @Override
    public boolean isInitialized() {
        EntityManager entityManager = null;
        try {
            entityManager = this.entityManagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            Query query = entityManager.createNativeQuery("SELECT 1");
            query.getSingleResult();

            // Commit the transaction
            entityManager.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            LOGGER.error("Error while checking if the backend is initialized", ex);
            return false;
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Override
    public void shutdownBackend() {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            entityManager.close();
        } catch (Exception ex) {
            LOGGER.error("Error while shutting down the backend", ex);
        }
    }

    @Override
    public void storeReportingItem(@Nonnull PeppolReportingItem aReportingItem) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            PeppolReportingItemWrapper reportingItemToPersist = new PeppolReportingItemWrapper(aReportingItem);

            entityManager.getTransaction().begin();
            entityManager.persist(reportingItemToPersist);
            entityManager.getTransaction().commit();

            entityManager.close();
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    @Nonnull
    @Override
    public Iterable<PeppolReportingItem> iterateReportingItems(@Nonnull LocalDate aStartDateIncl, @Nonnull LocalDate aEndDateIncl) throws PeppolReportingBackendException {
        List<PeppolReportingItemWrapper> wrapperResults = null;
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<PeppolReportingItemWrapper> criteriaQuery = criteriaBuilder.createQuery(PeppolReportingItemWrapper.class);
            Root<PeppolReportingItemWrapper> root = criteriaQuery.from(PeppolReportingItemWrapper.class);

            // Assuming exchangeDateTimeUTC is of type OffsetDateTime in PeppolReportingItemWrapper
            Path<OffsetDateTime> datePath = root.get("exchangeDateTimeUTC");

            // Convert LocalDate to OffsetDateTime at start of day (e.g., using system default time zone for simplicity)
            OffsetDateTime startDateTime = aStartDateIncl.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            OffsetDateTime endDateTime = aEndDateIncl.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);

            Predicate datePredicate = criteriaBuilder.between(datePath, startDateTime, endDateTime);
            criteriaQuery.select(root).where(datePredicate);

            wrapperResults = entityManager.createQuery(criteriaQuery).getResultList();

            entityManager.close();

            List<PeppolReportingItem> results = new ArrayList<PeppolReportingItem>();
            for (PeppolReportingItemWrapper wrapper : wrapperResults) {
                PeppolReportingItem item = wrapper.toPeppolReportingItem();
                results.add(item);
            }

            return results;
        } catch (Exception ex) {
            LOGGER.error("Error while iterating over reporting items.", ex);
            throw ex;
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
