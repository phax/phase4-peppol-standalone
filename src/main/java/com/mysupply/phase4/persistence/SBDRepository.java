package com.mysupply.phase4.persistence;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Singleton;
import org.springframework.stereotype.Repository;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import java.util.UUID;

@Repository
@Singleton
@IsSPIImplementation
public class SBDRepository implements ISBDRepository {
    public SBDRepository() {

    }

    public void save(StandardBusinessDocument standardBusinessDocument) {

    }

    public StandardBusinessDocument get(UUID documentId) {
        return null;
    }
}
