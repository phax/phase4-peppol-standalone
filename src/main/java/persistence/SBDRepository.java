package persistence;

import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import java.util.UUID;

@RequestScope
@Repository
public class SBDRepository implements ISBDRepository {

    public SBDRepository() {

    }

    public void save(StandardBusinessDocument standardBusinessDocument) {

    }

    public StandardBusinessDocument get(UUID documentId) {
        return null;
    }
}
