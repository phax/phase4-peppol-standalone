CREATE TABLE document
(
    id       UUID NOT NULL,
    document BYTEA,
    CONSTRAINT pk_document PRIMARY KEY (id)
);