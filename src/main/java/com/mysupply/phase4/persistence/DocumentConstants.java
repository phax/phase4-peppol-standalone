package com.mysupply.phase4.persistence;

public class DocumentConstants {
    public static final String DOCUMENT_TABLE_NAME = "document";
    public static final String DOCUMENT_SCHEMA_NAME = "phase4_documents";

    /** Table name for the Peppol stored-reports table, as created by the peppol-reporting-support Flyway migration. */
    public static final String PEPPOL_REPORT_TABLE_NAME = "peppol_report";
    /** Table name for the Peppol report sending-records table, as created by the peppol-reporting-support Flyway migration. */
    public static final String PEPPOL_SENDING_REPORT_TABLE_NAME = "peppol_sending_report";
    /** Table name for the Peppol raw reporting-items table, as created by the peppol-reporting-backend-sql Flyway migration. */
    public static final String PEPPOL_REPORTING_ITEM_TABLE_NAME = "peppol_reporting_item";
}
