package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tests proper behavior of tracking modified entity types when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings({"unchecked"})
public class DefaultTrackingEntitiesTest extends AbstractEntityTest {
    private Integer steId = null;
    private Integer siteId = null;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.setProperty("org.hibernate.envers.track_entities_changed_in_revision", "true");
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();
        
        // Revision 1 - Adding two entities
        em.getTransaction().begin();
        StrTestEntity ste = new StrTestEntity("x");
        StrIntTestEntity site = new StrIntTestEntity("y", 1);
        em.persist(ste);
        em.persist(site);
        steId = ste.getId();
        siteId = site.getId();
        em.getTransaction().commit();

        // Revision 2 - Modifying one entity
        em.getTransaction().begin();
        site = em.find(StrIntTestEntity.class, siteId);
        site.setNumber(2);
        em.getTransaction().commit();

        // Revision 3 - Deleting both entities
        em.getTransaction().begin();
        ste = em.find(StrTestEntity.class, steId);
        site = em.find(StrIntTestEntity.class, siteId);
        em.remove(ste);
        em.remove(site);
        em.getTransaction().commit();
    }

    @Test
    public void testRevEntityTableCreation() {
        Iterator<Table> tableIterator = getCfg().getTableMappings();
        while (tableIterator.hasNext()) {
            Table table = tableIterator.next();
            if ("REVCHANGES".equals(table.getName())) {
                assert table.getColumnSpan() == 2;
                assert table.getColumn(new Column("REV")) != null;
                assert table.getColumn(new Column("ENTITYTYPE")) != null;
                return;
            }
        }
        assert false;
    }

    @Test
    public void testTrackAddedEntities() {
        StrTestEntity ste = new StrTestEntity("x", steId);
        StrIntTestEntity site = new StrIntTestEntity("y", 1, siteId);

        assert TestTools.checkList(getAuditReader().findEntitiesChangedInRevision(1), ste, site);
    }

    @Test
    public void testTrackModifiedEntities() {
        StrIntTestEntity site = new StrIntTestEntity("y", 2, siteId);

        assert TestTools.checkList(getAuditReader().findEntitiesChangedInRevision(2), site);
    }

    @Test
    public void testTrackDeletedEntities() {
        StrTestEntity ste = new StrTestEntity(null, steId);
        StrIntTestEntity site = new StrIntTestEntity(null, null, siteId);

        assert TestTools.checkList(getAuditReader().findEntitiesChangedInRevision(3), site, ste);
    }

    @Test
    public void testFindChangesInInvalidRevision() {
        assert getAuditReader().findEntitiesChangedInRevision(4).isEmpty();
    }

    @Test
    public void testTrackAddedEntitiesGroupByRevisionType() {
        StrTestEntity ste = new StrTestEntity("x", steId);
        StrIntTestEntity site = new StrIntTestEntity("y", 1, siteId);

        Map<RevisionType, List<Object>> result = getAuditReader().findEntitiesChangedInRevisionGroupByRevisionType(1);
        assert TestTools.checkList(result.get(RevisionType.ADD), site, ste);
        assert TestTools.checkList(result.get(RevisionType.MOD));
        assert TestTools.checkList(result.get(RevisionType.DEL));
    }

    @Test
    public void testTrackModifiedEntitiesGroupByRevisionType() {
        StrIntTestEntity site = new StrIntTestEntity("y", 2, siteId);

        Map<RevisionType, List<Object>> result = getAuditReader().findEntitiesChangedInRevisionGroupByRevisionType(2);
        assert TestTools.checkList(result.get(RevisionType.ADD));
        assert TestTools.checkList(result.get(RevisionType.MOD), site);
        assert TestTools.checkList(result.get(RevisionType.DEL));
    }

    @Test
    public void testTrackDeletedEntitiesGroupByRevisionType() {
        StrTestEntity ste = new StrTestEntity(null, steId);
        StrIntTestEntity site = new StrIntTestEntity(null, null, siteId);

        Map<RevisionType, List<Object>> result = getAuditReader().findEntitiesChangedInRevisionGroupByRevisionType(3);
        assert TestTools.checkList(result.get(RevisionType.ADD));
        assert TestTools.checkList(result.get(RevisionType.MOD));
        assert TestTools.checkList(result.get(RevisionType.DEL), site, ste);
    }

    @Test
    public void testFindChangedEntitiesByRevisionTypeADD() {
        StrTestEntity ste = new StrTestEntity("x", steId);
        StrIntTestEntity site = new StrIntTestEntity("y", 1, siteId);

        assert TestTools.checkList(getAuditReader().findEntitiesChangedInRevision(1, RevisionType.ADD), ste, site);
    }

    @Test
    public void testFindChangedEntitiesByRevisionTypeMOD() {
        StrIntTestEntity site = new StrIntTestEntity("y", 2, siteId);

        assert TestTools.checkList(getAuditReader().findEntitiesChangedInRevision(2, RevisionType.MOD), site);
    }

    @Test
    public void testFindChangedEntitiesByRevisionTypeDEL() {
        StrTestEntity ste = new StrTestEntity(null, steId);
        StrIntTestEntity site = new StrIntTestEntity(null, null, siteId);

        assert TestTools.checkList(getAuditReader().findEntitiesChangedInRevision(3, RevisionType.DEL), ste, site);
    }

    @Test
    public void testFindEntityTypesChangedInRevision() {
        assert TestTools.makeSet(StrTestEntity.class, StrIntTestEntity.class).equals(getAuditReader().findEntityTypesChangedInRevision(1));
        assert TestTools.makeSet(StrIntTestEntity.class).equals(getAuditReader().findEntityTypesChangedInRevision(2));
        assert TestTools.makeSet(StrTestEntity.class, StrIntTestEntity.class).equals(getAuditReader().findEntityTypesChangedInRevision(3));
    }
}
