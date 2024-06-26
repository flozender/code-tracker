package org.hibernate.envers.test.integration.manytomany.unidirectional;
import static org.hibernate.envers.test.tools.TestTools.checkList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.manytomany.unidirectional.M2MIndexedListTargetNotAuditedEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A test for auditing a many-to-many indexed list where the target entity is not audited.
 *
 * @author Vladimir Klyushnikov
 * @author Adam Warski
 */
public class M2MIndexedListNotAuditedTarget extends AbstractEntityTest {
    private Integer itnae1_id;
    private Integer itnae2_id;

    private UnversionedStrTestEntity uste1;
    private UnversionedStrTestEntity uste2;


    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(UnversionedStrTestEntity.class);
        cfg.addAnnotatedClass(M2MIndexedListTargetNotAuditedEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        uste1 = new UnversionedStrTestEntity("str1");
        uste2 = new UnversionedStrTestEntity("str2");

        // No revision
        em.getTransaction().begin();

        em.persist(uste1);
        em.persist(uste2);

        em.getTransaction().commit();

        // Revision 1
        em.getTransaction().begin();

        uste1 = em.find(UnversionedStrTestEntity.class, uste1.getId());
        uste2 = em.find(UnversionedStrTestEntity.class, uste2.getId());

        M2MIndexedListTargetNotAuditedEntity itnae1 = new M2MIndexedListTargetNotAuditedEntity(1, "tnae1");

        itnae1.getReferences().add(uste1);
        itnae1.getReferences().add(uste2);

        em.persist(itnae1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        M2MIndexedListTargetNotAuditedEntity itnae2 = new M2MIndexedListTargetNotAuditedEntity(2, "tnae2");

        itnae2.getReferences().add(uste2);

        em.persist(itnae2);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        itnae1.getReferences().set(0, uste2);
        itnae1.getReferences().set(1, uste1);
        em.getTransaction().commit();

        itnae1_id = itnae1.getId();
        itnae2_id = itnae2.getId();
    }

	@Test
	public void testRevisionsCounts() {
		List<Number> revisions = getAuditReader().getRevisions(M2MIndexedListTargetNotAuditedEntity.class, itnae1_id);
		assertEquals(revisions, Arrays.asList(1, 3));

		revisions = getAuditReader().getRevisions(M2MIndexedListTargetNotAuditedEntity.class, itnae2_id);
        assertEquals(revisions, Arrays.asList(2));
	}

    @Test
    public void testHistory1() throws Exception {
        M2MIndexedListTargetNotAuditedEntity rev1 = getAuditReader().find(M2MIndexedListTargetNotAuditedEntity.class, itnae1_id, 1);
        M2MIndexedListTargetNotAuditedEntity rev2 = getAuditReader().find(M2MIndexedListTargetNotAuditedEntity.class, itnae1_id, 2);
        M2MIndexedListTargetNotAuditedEntity rev3 = getAuditReader().find(M2MIndexedListTargetNotAuditedEntity.class, itnae1_id, 3);

        assertTrue(checkList(rev1.getReferences(), uste1, uste2));
        assertTrue(checkList(rev2.getReferences(), uste1, uste2));
        assertTrue(checkList(rev3.getReferences(), uste2, uste1));
    }

    @Test
    public void testHistory2() throws Exception {
        M2MIndexedListTargetNotAuditedEntity rev1 = getAuditReader().find(M2MIndexedListTargetNotAuditedEntity.class, itnae2_id, 1);
        M2MIndexedListTargetNotAuditedEntity rev2 = getAuditReader().find(M2MIndexedListTargetNotAuditedEntity.class, itnae2_id, 2);
        M2MIndexedListTargetNotAuditedEntity rev3 = getAuditReader().find(M2MIndexedListTargetNotAuditedEntity.class, itnae2_id, 3);

        assertNull(rev1);
        assertTrue(checkList(rev2.getReferences(), uste2));
        assertTrue(checkList(rev3.getReferences(), uste2));
    }
}
