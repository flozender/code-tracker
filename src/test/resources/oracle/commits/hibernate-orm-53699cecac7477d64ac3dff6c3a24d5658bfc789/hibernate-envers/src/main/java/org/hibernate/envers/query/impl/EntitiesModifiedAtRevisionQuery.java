package org.hibernate.envers.query.impl;

import org.hibernate.Query;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.configuration.AuditEntitiesConfiguration;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.reader.AuditReaderImplementor;

import java.util.ArrayList;
import java.util.List;

/**
 * In comparison to {@link EntitiesAtRevisionQuery} this query returns an empty collection if an entity
 * of a certain type has not been changed in a given revision.
 * @see EntitiesAtRevisionQuery
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EntitiesModifiedAtRevisionQuery extends AbstractAuditQuery {
    private final Number revision;

    public EntitiesModifiedAtRevisionQuery(AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
                                           Class<?> cls, Number revision) {
		super(verCfg, versionsReader, cls);
        this.revision = revision;
	}

    @SuppressWarnings({"unchecked"})
    public List list() {
        /*
         * The query that we need to create:
         *   SELECT new list(e) FROM versionsReferencedEntity e
         *   WHERE
         * (all specified conditions, transformed, on the "e" entity) AND
         * e.revision = :revision
         */
        AuditEntitiesConfiguration verEntCfg = verCfg.getAuditEntCfg();
        String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
        qb.getRootParameters().addWhereWithParam(revisionPropertyPath, "=", revision);

        // all specified conditions
        for (AuditCriterion criterion : criterions) {
            criterion.addToQuery(verCfg, entityName, qb, qb.getRootParameters());
        }

        Query query = buildQuery();
        List queryResult = query.list();

        if (hasProjection) {
            return queryResult;
        } else {
            List result = new ArrayList();
            entityInstantiator.addInstancesFromVersionsEntities(entityName, result, queryResult, revision);

            return result;
        }
    }
}