package com.mysupply.phase4.peppolstandalone;

import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.mysupply.phase4.domain.PeppolReportingItemWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PeppolReportingItemWrapperTests {
    @Test
    public void testEquals() {
        PeppolReportingItem peppolReportingItem = new PeppolReportingItem(OffsetDateTime.now(),
                EReportingDirection.SENDING,
                "c2ID",
                "c3ID",
                "docTypeIDScheme",
                "docTypeIDValue",
                "processIDScheme",
                "processIDValue",
                "transportProtocol",
                "c1CountryCode",
                "c4CountryCode",
                "endUserID");
        PeppolReportingItemWrapper peppolReportingItemWrapper = new PeppolReportingItemWrapper(peppolReportingItem);

        assertEquals(peppolReportingItemWrapper, peppolReportingItem);
        assertTrue(peppolReportingItemWrapper.equals(peppolReportingItem));
    }
}
