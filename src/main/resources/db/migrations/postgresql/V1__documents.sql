CREATE TABLE document
(
    id                          UUID NOT NULL,
    data                        BYTEA NOT NULL,
    data_size                   BIGINT NOT NULL DEFAULT 0,  -- The size of the data in bytes
    created                     TIMESTAMPTZ NOT NULL,   -- The time the document was received
    domain                      VARCHAR NOT NULL,       -- The domain name that received the document (target vax name)
    sender_identifier           VARCHAR NOT NULL,       -- The sender identification from SBDH
    receiver_identifier         VARCHAR NOT NULL,       -- The receiver identification from SBDH
    doc_type                    VARCHAR NOT NULL,       -- The document type from SBDH
    process                     VARCHAR NOT NULL,       -- The process identifier from SBDH
    sender_certificate          TEXT NULL,              -- The sender's certificate (PEM encoded)
    receiver_certificate        TEXT NULL,              -- The receiver's certificate (PEM encoded)
    protocol                    VARCHAR NULL,           -- The protocol used (e.g. AS4)
    conversation_id             VARCHAR NULL,           -- The conversation id from the message
    message_id                  VARCHAR NULL,           -- The message id from the message
    retrieved                   TIMESTAMPTZ NULL,       -- The retrieved timestamp
    vax_id                      UUID NULL,              -- The id the document has in VAX
    retrieved_by_connector_name VARCHAR NULL,           -- The name of the connector that confirmed the document.
    retrieved_by_connector_id   UUID NULL,              -- The id of the connector that confirmed the document.
    retrieved_by_instance_name  VARCHAR NULL,           -- The name of the vax instance.
    CONSTRAINT pk_document PRIMARY KEY (id)
);

-- Index for finding not retrieved documents by search criteria (used in findNotRetrievedIdsBySearchCriteria)
CREATE INDEX idx_document_retrieved ON document (retrieved);
CREATE INDEX idx_document_sender_identifier ON document (sender_identifier);
CREATE INDEX idx_document_receiver_identifier ON document (receiver_identifier);
CREATE INDEX idx_document_domain ON document (domain);

-- Composite index for the most common query pattern
CREATE INDEX idx_document_not_retrieved_search ON document (retrieved, sender_identifier, receiver_identifier, domain, created)
    WHERE retrieved IS NULL;

