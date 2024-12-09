package com.mysupply.phase4.domain;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import jakarta.persistence.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Class mimicking the {@link PeppolReportingItem} PeppolReportingItem class from Philip Helger's Peppol reporting library.
 * We mimic this class to be able to store the PeppolReportingItem objects in a database, since Java ORM's are not happy to persist complex objects from third party libraries.
 */
@Entity
@Table(name = "PeppolReportingItems")
public class PeppolReportingItemWrapper {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private OffsetDateTime exchangeDateTimeUTC;
    @Enumerated(EnumType.STRING)
    private EReportingDirection direction;
    private String c2ID;
    private String c3ID;
    private String docTypeIDScheme;
    private String docTypeIDValue;
    private String processIDScheme;
    private String processIDValue;
    private String transportProtocol;
    private String c1CountryCode;
    private String c4CountryCode;
    private String endUserID;

    // Default constructor for ORM
    public PeppolReportingItemWrapper() {
        this.exchangeDateTimeUTC = null;
        this.direction = null;
        this.c2ID = null;
        this.c3ID = null;
        this.docTypeIDScheme = null;
        this.docTypeIDValue = null;
        this.processIDScheme = null;
        this.processIDValue = null;
        this.transportProtocol = null;
        this.c1CountryCode = null;
        this.c4CountryCode = null;
        this.endUserID = null;
    }

    public PeppolReportingItemWrapper(@Nonnull final OffsetDateTime exchangeDateTimeUTC,
                                      @Nonnull final EReportingDirection direction,
                                      @Nonnull @Nonempty final String c2ID,
                                      @Nonnull @Nonempty final String c3ID,
                                      @Nonnull @Nonempty final String docTypeIDScheme,
                                      @Nonnull @Nonempty final String docTypeIDValue,
                                      @Nonnull @Nonempty final String processIDScheme,
                                      @Nonnull @Nonempty final String processIDValue,
                                      @Nonnull @Nonempty final String transportProtocol,
                                      @Nonnull @Nonempty final String c1CountryCode,
                                      @Nullable final String c4CountryCode,
                                      @Nonnull @Nonempty final String endUserID) {
        this.exchangeDateTimeUTC = exchangeDateTimeUTC;
        this.direction = direction;
        this.c2ID = c2ID;
        this.c3ID = c3ID;
        this.docTypeIDScheme = docTypeIDScheme;
        this.docTypeIDValue = docTypeIDValue;
        this.processIDScheme = processIDScheme;
        this.processIDValue = processIDValue;
        this.transportProtocol = transportProtocol;
        this.c1CountryCode = c1CountryCode;
        this.c4CountryCode = c4CountryCode;
        this.endUserID = endUserID;
    }

    // Constructor converting from the third-party object
    public PeppolReportingItemWrapper(@Nonnull PeppolReportingItem reportingItem) {
        this.exchangeDateTimeUTC = reportingItem.getExchangeDTUTC();
        this.direction = reportingItem.getDirection();
        this.c2ID = reportingItem.getC2ID();
        this.c3ID = reportingItem.getC3ID();
        this.docTypeIDScheme = reportingItem.getDocTypeIDScheme();
        this.docTypeIDValue = reportingItem.getDocTypeIDValue();
        this.processIDScheme = reportingItem.getProcessIDScheme();
        this.processIDValue = reportingItem.getProcessIDValue();
        this.transportProtocol = reportingItem.getTransportProtocol();
        this.c1CountryCode = reportingItem.getC1CountryCode();
        this.c4CountryCode = reportingItem.getC4CountryCode();
        this.endUserID = reportingItem.getEndUserID();
    }

    // Method to convert back to the third-party object
    public PeppolReportingItem toPeppolReportingItem() {
        return new PeppolReportingItem(
                this.exchangeDateTimeUTC,
                this.direction, // assuming directional enum
                this.c2ID,
                this.c3ID,
                this.docTypeIDScheme,
                this.docTypeIDValue,
                this.processIDScheme,
                this.processIDValue,
                this.transportProtocol,
                this.c1CountryCode,
                this.c4CountryCode,
                this.endUserID
        );
    }

    public OffsetDateTime getExchangeDateTimeUTC() {
        return exchangeDateTimeUTC;
    }

    public void setExchangeDateTimeUTC(OffsetDateTime exchangeDateTimeUTC) {
        this.exchangeDateTimeUTC = exchangeDateTimeUTC;
    }

    public EReportingDirection getDirection() {
        return direction;
    }

    public void setDirection(EReportingDirection direction) {
        this.direction = direction;
    }

    public String getC2ID() {
        return c2ID;
    }

    public void setC2ID(String c2ID) {
        this.c2ID = c2ID;
    }

    public String getC3ID() {
        return c3ID;
    }

    public void setC3ID(String c3ID) {
        this.c3ID = c3ID;
    }

    public String getDocTypeIDScheme() {
        return docTypeIDScheme;
    }

    public void setDocTypeIDScheme(String docTypeIDScheme) {
        this.docTypeIDScheme = docTypeIDScheme;
    }

    public String getDocTypeIDValue() {
        return docTypeIDValue;
    }

    public void setDocTypeIDValue(String docTypeIDValue) {
        this.docTypeIDValue = docTypeIDValue;
    }

    public String getProcessIDScheme() {
        return processIDScheme;
    }

    public void setProcessIDScheme(String processIDScheme) {
        this.processIDScheme = processIDScheme;
    }

    public String getProcessIDValue() {
        return processIDValue;
    }

    public void setProcessIDValue(String processIDValue) {
        this.processIDValue = processIDValue;
    }

    public String getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public String getC1CountryCode() {
        return c1CountryCode;
    }

    public void setC1CountryCode(String c1CountryCode) {
        this.c1CountryCode = c1CountryCode;
    }

    public String getC4CountryCode() {
        return c4CountryCode;
    }

    public void setC4CountryCode(String c4CountryCode) {
        this.c4CountryCode = c4CountryCode;
    }

    public String getEndUserID() {
        return endUserID;
    }

    public void setEndUserID(String endUserID) {
        this.endUserID = endUserID;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (object == null) {
            return false;
        } else if (object instanceof PeppolReportingItem) {
            PeppolReportingItem reportingItem = (PeppolReportingItem) object;
            return equalsPeppolReportingItem(reportingItem);
        } else if (object instanceof PeppolReportingItemWrapper) {
            PeppolReportingItemWrapper reportingItemWrapper = (PeppolReportingItemWrapper) object;
            return this.equalsPeppolReportingItemWrapper(reportingItemWrapper);
        } else {
            return false;
        }
    }

    private boolean equalsPeppolReportingItem(PeppolReportingItem reportingItem) {
        return this.exchangeDateTimeUTC.equals(reportingItem.getExchangeDTUTC()) &&
                this.direction.equals(reportingItem.getDirection()) &&
                this.c2ID.equals(reportingItem.getC2ID()) &&
                this.c3ID.equals(reportingItem.getC3ID()) &&
                this.docTypeIDScheme.equals(reportingItem.getDocTypeIDScheme()) &&
                this.docTypeIDValue.equals(reportingItem.getDocTypeIDValue()) &&
                this.processIDScheme.equals(reportingItem.getProcessIDScheme()) &&
                this.processIDValue.equals(reportingItem.getProcessIDValue()) &&
                this.transportProtocol.equals(reportingItem.getTransportProtocol()) &&
                this.c1CountryCode.equals(reportingItem.getC1CountryCode()) &&
                EqualsHelper.equals(this.c4CountryCode, reportingItem.getC4CountryCode()) &&
                this.endUserID.equals(reportingItem.getEndUserID());
    }

    private boolean equalsPeppolReportingItemWrapper(PeppolReportingItemWrapper reportingItem) {
        return this.id.equals(reportingItem.getId()) &&
                this.exchangeDateTimeUTC.equals(reportingItem.getExchangeDateTimeUTC()) &&
                this.direction.equals(reportingItem.getDirection()) &&
                this.c2ID.equals(reportingItem.getC2ID()) &&
                this.c3ID.equals(reportingItem.getC3ID()) &&
                this.docTypeIDScheme.equals(reportingItem.getDocTypeIDScheme()) &&
                this.docTypeIDValue.equals(reportingItem.getDocTypeIDValue()) &&
                this.processIDScheme.equals(reportingItem.getProcessIDScheme()) &&
                this.processIDValue.equals(reportingItem.getProcessIDValue()) &&
                this.transportProtocol.equals(reportingItem.getTransportProtocol()) &&
                this.c1CountryCode.equals(reportingItem.getC1CountryCode()) &&
                EqualsHelper.equals(this.c4CountryCode, reportingItem.getC4CountryCode()) &&
                this.endUserID.equals(reportingItem.getEndUserID());
    }
}
