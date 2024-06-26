package org.hibernate.envers.synchronization;

import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.synchronization.work.AuditWorkUnit;
import org.hibernate.envers.synchronization.work.PersistentCollectionChangeWorkUnit;
import org.hibernate.persister.entity.EntityPersister;

import java.io.Serializable;

/**
 * Notifies {@link RevisionInfoGenerator} about changes made in the current revision.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EntityChangeNotifier {
    private final RevisionInfoGenerator revisionInfoGenerator;
    private final SessionImplementor sessionImplementor;

    public EntityChangeNotifier(RevisionInfoGenerator revisionInfoGenerator, SessionImplementor sessionImplementor) {
        this.revisionInfoGenerator = revisionInfoGenerator;
        this.sessionImplementor = sessionImplementor;
    }

    /**
     * Notifies {@link RevisionInfoGenerator} about changes made in the current revision. Provides information
     * about modified entity class, entity name and its id, as well as {@link RevisionType} and revision log entity.
     * @param session Active session.
     * @param currentRevisionData Revision log entity.
     * @param vwu Performed work unit.
     */
    public void entityChanged(Session session, Object currentRevisionData, AuditWorkUnit vwu) {
        Serializable entityId = vwu.getEntityId();
        if (entityId instanceof PersistentCollectionChangeWorkUnit.PersistentCollectionChangeWorkUnitId) {
            // Notify about a change in collection owner entity.
            entityId = ((PersistentCollectionChangeWorkUnit.PersistentCollectionChangeWorkUnitId) entityId).getOwnerId();
        }
        Class entityClass = getEntityClass(session, vwu.getEntityName());
        revisionInfoGenerator.entityChanged(entityClass, vwu.getEntityName(), entityId, vwu.getRevisionType(),
                                            currentRevisionData);
    }

    /**
     * @return Java class mapped to specified entity name.
     */
    private Class getEntityClass(Session session, String entityName) {
        EntityPersister entityPersister = sessionImplementor.getFactory().getEntityPersister(entityName);
        return entityPersister.getClassMetadata().getMappedClass(session.getEntityMode());
    }
}
