package persistence;

import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import java.util.UUID;

public interface ISBDRepository {
    void save(StandardBusinessDocument standardBusinessDocument);
    StandardBusinessDocument get(UUID documentId);
}
