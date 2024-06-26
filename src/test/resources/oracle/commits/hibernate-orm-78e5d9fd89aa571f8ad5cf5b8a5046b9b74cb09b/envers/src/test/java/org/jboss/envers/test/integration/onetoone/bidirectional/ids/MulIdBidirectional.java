package org.jboss.envers.test.integration.onetoone.bidirectional.ids;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.ids.MulId;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MulIdBidirectional extends AbstractEntityTest {
    private MulId ed1_id;
    private MulId ed2_id;

    private MulId ing1_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BiMulIdRefEdEntity.class);
        cfg.addAnnotatedClass(BiMulIdRefIngEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        ed1_id = new MulId(1, 2);
        ed2_id = new MulId(3, 4);

        ing1_id = new MulId(5, 6);

        BiMulIdRefEdEntity ed1 = new BiMulIdRefEdEntity(ed1_id.getId1(), ed1_id.getId2(), "data_ed_1");
        BiMulIdRefEdEntity ed2 = new BiMulIdRefEdEntity(ed2_id.getId1(), ed2_id.getId2(), "data_ed_2");

        BiMulIdRefIngEntity ing1 = new BiMulIdRefIngEntity(ing1_id.getId1(), ing1_id.getId2(), "data_ing_1");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        ing1.setReference(ed1);

        em.persist(ed1);
        em.persist(ed2);

        em.persist(ing1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ing1 = em.find(BiMulIdRefIngEntity.class, ing1_id);
        ed2 = em.find(BiMulIdRefEdEntity.class, ed2_id);

        ing1.setReference(ed2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(BiMulIdRefEdEntity.class, ed1_id));
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(BiMulIdRefEdEntity.class, ed2_id));

        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(BiMulIdRefIngEntity.class, ing1_id));
    }

    @Test
    public void testHistoryOfEdId1() {
        BiMulIdRefIngEntity ing1 = getEntityManager().find(BiMulIdRefIngEntity.class, ing1_id);

        BiMulIdRefEdEntity rev1 = getVersionsReader().find(BiMulIdRefEdEntity.class, ed1_id, 1);
        BiMulIdRefEdEntity rev2 = getVersionsReader().find(BiMulIdRefEdEntity.class, ed1_id, 2);

        assert rev1.getReferencing().equals(ing1);
        assert rev2.getReferencing() == null;
    }

    @Test
    public void testHistoryOfEdId2() {
        BiMulIdRefIngEntity ing1 = getEntityManager().find(BiMulIdRefIngEntity.class, ing1_id);

        BiMulIdRefEdEntity rev1 = getVersionsReader().find(BiMulIdRefEdEntity.class, ed2_id, 1);
        BiMulIdRefEdEntity rev2 = getVersionsReader().find(BiMulIdRefEdEntity.class, ed2_id, 2);

        assert rev1.getReferencing() == null;
        assert rev2.getReferencing().equals(ing1);
    }

    @Test
    public void testHistoryOfIngId1() {
        BiMulIdRefEdEntity ed1 = getEntityManager().find(BiMulIdRefEdEntity.class, ed1_id);
        BiMulIdRefEdEntity ed2 = getEntityManager().find(BiMulIdRefEdEntity.class, ed2_id);

        BiMulIdRefIngEntity rev1 = getVersionsReader().find(BiMulIdRefIngEntity.class, ing1_id, 1);
        BiMulIdRefIngEntity rev2 = getVersionsReader().find(BiMulIdRefIngEntity.class, ing1_id, 2);

        assert rev1.getReference().equals(ed1);
        assert rev2.getReference().equals(ed2);
    }
}