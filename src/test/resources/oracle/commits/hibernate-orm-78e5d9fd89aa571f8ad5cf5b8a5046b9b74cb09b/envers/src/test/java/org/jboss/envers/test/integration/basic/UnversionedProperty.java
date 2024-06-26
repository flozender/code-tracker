package org.jboss.envers.test.integration.basic;

import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.jboss.envers.test.entities.UnversionedEntity;
import org.jboss.envers.test.AbstractEntityTest;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnversionedProperty extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(UnversionedEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        // Rev 1
        em.getTransaction().begin();
        UnversionedEntity ue1 = new UnversionedEntity("a1", "b1");
        em.persist(ue1);
        id1 = ue1.getId();
        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();
        ue1 = em.find(UnversionedEntity.class, id1);
        ue1.setData1("a2");
        ue1.setData2("b2");
        em.getTransaction().commit();
    }

     @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(UnversionedEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        UnversionedEntity rev1 = new UnversionedEntity(id1, "a1", null);
        UnversionedEntity rev2 = new UnversionedEntity(id1, "a2", null);

        assert getVersionsReader().find(UnversionedEntity.class, id1, 1).equals(rev1);
        assert getVersionsReader().find(UnversionedEntity.class, id1, 2).equals(rev2);
    }
}
