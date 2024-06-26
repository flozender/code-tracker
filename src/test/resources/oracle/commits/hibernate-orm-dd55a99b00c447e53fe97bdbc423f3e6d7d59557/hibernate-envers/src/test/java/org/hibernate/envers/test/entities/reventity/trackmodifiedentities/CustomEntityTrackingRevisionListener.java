package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionType;

import java.io.Serializable;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CustomEntityTrackingRevisionListener implements EntityTrackingRevisionListener {
    @Override
    public void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
                              Object revisionEntity) {
        ((CustomTrackingRevisionEntity)revisionEntity).addModifiedEntityName(entityClass.getName());
    }

    @Override
    public void newRevision(Object revisionEntity) {
    }
}
