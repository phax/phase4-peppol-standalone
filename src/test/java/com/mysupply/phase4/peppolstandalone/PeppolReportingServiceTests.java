package com.mysupply.phase4.peppolstandalone;

import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.mysupply.phase4.domain.PeppolReportingItemWrapper;
import com.mysupply.phase4.services.PeppolReportingService;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PeppolReportingServiceTests {

    @Mock
    private EntityManagerFactory mockEntityManagerFactory;
    @Mock
    private EntityManager mockEntityManager;
    @Mock
    private EntityTransaction mockTransaction;
    @Mock
    private Query mockQuery;
    @Mock
    private CriteriaBuilder mockCriteriaBuilder;
    @Mock
    private CriteriaQuery<PeppolReportingItemWrapper> mockCriteriaQuery;
    @Mock
    private Root<PeppolReportingItemWrapper> mockRoot;
    @Mock
    private Predicate mockPredicate;
    @Mock
    private TypedQuery<PeppolReportingItemWrapper> mockTypedQuery;

    private PeppolReportingService reportingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(this.mockEntityManagerFactory.createEntityManager()).thenReturn(this.mockEntityManager);
        when(this.mockEntityManager.getTransaction()).thenReturn(this.mockTransaction);
        when(this.mockEntityManager.getCriteriaBuilder()).thenReturn(this.mockCriteriaBuilder);
        when(this.mockEntityManager.createQuery(any(CriteriaQuery.class))).thenReturn(this.mockTypedQuery);

        when(this.mockCriteriaBuilder.createQuery(PeppolReportingItemWrapper.class)).thenReturn(this.mockCriteriaQuery);
        when(this.mockCriteriaQuery.from(PeppolReportingItemWrapper.class)).thenReturn(this.mockRoot);
        when(this.mockCriteriaBuilder.between(any(Expression.class), any(Expression.class), any(Expression.class))).thenReturn(this.mockPredicate);
        when(this.mockCriteriaQuery.select(this.mockRoot)).thenReturn(this.mockCriteriaQuery);
        when(this.mockCriteriaQuery.where(this.mockPredicate)).thenReturn(this.mockCriteriaQuery);

        when(this.mockTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        doNothing().when(this.mockTransaction).begin();
        doNothing().when(this.mockTransaction).commit();

        this.reportingService = new PeppolReportingService(this.mockEntityManagerFactory);
    }

    @Test
    public void testIsInitialized() {
        when(this.mockEntityManager.createNativeQuery(anyString())).thenReturn(this.mockQuery);
        when(mockEntityManager.getTransaction()).thenReturn(mockTransaction);

        doNothing().when(mockTransaction).begin();
        doNothing().when(mockTransaction).commit();
        when(this.mockQuery.getSingleResult()).thenReturn(1);

        boolean initialized = this.reportingService.isInitialized();

        assertTrue(initialized, "Service should be initialized");

        verify(this.mockEntityManager, times(2)).getTransaction();
        verify(this.mockEntityManager, times(1)).createNativeQuery("SELECT 1");
        verify(this.mockQuery, times(1)).getSingleResult();
    }

    @Test
    public void testStoreReportingItem() {
        PeppolReportingItem item = createMockReportingItem();

        assertDoesNotThrow(() -> this.reportingService.storeReportingItem(item));

        verify(this.mockEntityManager).persist(any(PeppolReportingItemWrapper.class));
    }

    @Test
    public void testIterateReportingItems_Empty() throws PeppolReportingBackendException {
        Iterable<PeppolReportingItem> results = reportingService.iterateReportingItems(
                OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().minusDays(1),
                OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().plusDays(1));

        assertNotNull(results, "Results should not be null");
        assertFalse(results.iterator().hasNext(), "Results should be empty");

        verify(mockEntityManager, times(1)).createQuery(mockCriteriaQuery);
        verify(mockEntityManager, times(1)).close();
    }

    @Test
    public void testIterateReportingItems_DataInInterval() throws PeppolReportingBackendException {
        PeppolReportingItemWrapper wrapper1 = this.createReportingItemWrapper();
        PeppolReportingItemWrapper wrapper2 = this.createReportingItemWrapper();

        List<PeppolReportingItemWrapper> mockWrapperList = List.of(wrapper1, wrapper2);
        when(mockTypedQuery.getResultList()).thenReturn(mockWrapperList);

        Iterable<PeppolReportingItem> results = reportingService.iterateReportingItems(
                OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().minusDays(1),
                OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().plusDays(1));

        assertNotNull(results, "Results should not be null");
        List<PeppolReportingItem> resultList = (List<PeppolReportingItem>) ((List<?>) results);
        assertEquals(2, resultList.size(), "Results should contain two items");

        verify(mockEntityManager, times(1)).createQuery(mockCriteriaQuery);
        verify(mockEntityManager, times(1)).close();
    }

    @Test
    public void testShutdownBackend() {
        assertDoesNotThrow(() -> this.reportingService.shutdownBackend());
    }

    private PeppolReportingItem createMockReportingItem() {
        return new PeppolReportingItem(
                OffsetDateTime.now(ZoneOffset.UTC),
                EReportingDirection.SENDING,
                "C2IDExample",
                "C3IDExample",
                "DocTypeScheme",
                "DocTypeValue",
                "ProcessScheme",
                "ProcessValue",
                "TransportProtocol",
                "C1CountryCode",
                "C4CountryCode",
                "EndUserIDExample"
        );
    }

    private PeppolReportingItemWrapper createReportingItemWrapper() {
        return new PeppolReportingItemWrapper(
                OffsetDateTime.now(ZoneOffset.UTC),
                EReportingDirection.SENDING,
                "C2IDExample",
                "C3IDExample",
                "DocTypeScheme",
                "DocTypeValue",
                "ProcessScheme",
                "ProcessValue",
                "TransportProtocol",
                "C1CountryCode",
                "C4CountryCode",
                "EndUserIDExample"
        );
    }
}