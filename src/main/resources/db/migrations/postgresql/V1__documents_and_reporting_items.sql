CREATE TABLE document
(
    id       UUID NOT NULL,
    document BYTEA,
    CONSTRAINT pk_document PRIMARY KEY (id)
);

CREATE TABLE peppol_reporting_items
(
    id                    UUID NOT NULL,
    exchange_date_timeutc TIMESTAMP WITHOUT TIME ZONE,
    direction             VARCHAR(32),
    c2id                  VARCHAR(64),
    c3id                  VARCHAR(64),
    doc_typeidscheme      VARCHAR(255),
    doc_typeidvalue       VARCHAR(255),
    processidscheme       VARCHAR(255),
    processidvalue        VARCHAR(255),
    transport_protocol    VARCHAR(64),
    c1country_code        VARCHAR(16),
    c4country_code        VARCHAR(16),
    end_userid            VARCHAR(64),
    CONSTRAINT pk_peppolreportingitems PRIMARY KEY (id)
);