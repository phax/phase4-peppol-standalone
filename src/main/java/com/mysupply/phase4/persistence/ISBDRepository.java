package com.mysupply.phase4.persistence;

import com.helger.commons.annotation.IsSPIInterface;
import com.mysupply.phase4.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

@IsSPIInterface
public interface ISBDRepository extends JpaRepository<Document, UUID> {
}
