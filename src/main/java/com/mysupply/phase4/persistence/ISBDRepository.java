package com.mysupply.phase4.persistence;

import com.mysupply.phase4.domain.Document;
import com.mysupply.phase4.peppolstandalone.dto.DocumentOverview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ISBDRepository extends JpaRepository<Document, UUID> {
    // Find only document IDs by sender, receiver and domain that have not been retrieved
    @Query("SELECT d.id FROM Document d WHERE d.retrieved IS NULL " +
           "AND (:senderWildcard = true OR d.senderIdentifier IN :senderIdentifiers) " +
           "AND (:receiverWildcard = true OR d.receiverIdentifier IN :receiverIdentifiers) " +
           "AND (:domainWildcard = true OR d.domain IN :domains) " +
           "ORDER BY d.created ASC")
    List<UUID> findNotRetrievedIdsBySearchCriteria(
            @Param("senderWildcard") boolean senderWildcard,
            @Param("senderIdentifiers") List<String> senderIdentifiers,
            @Param("receiverWildcard") boolean receiverWildcard,
            @Param("receiverIdentifiers") List<String> receiverIdentifiers,
            @Param("domainWildcard") boolean domainWildcard,
            @Param("domains") List<String> domains
    );

    // Find all documents for overview without fetching the data blob
    @Query("SELECT new com.mysupply.phase4.peppolstandalone.dto.DocumentOverview(" +
           "d.id, d.created, d.domain, d.senderIdentifier, d.receiverIdentifier, " +
           "d.docType, d.process, d.protocol, d.conversationId, d.messageId, " +
           "d.retrieved, d.vaxId, d.retrievedByInstanceName, d.retrievedByConnectorId, " +
           "d.retrievedByConnectorName, d.dataSize) " +
           "FROM Document d ORDER BY d.created DESC")
    List<DocumentOverview> findAllDocumentOverviews();
}
