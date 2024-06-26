package org.jboss.envers.test.integration.onetomany.detached;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.onetomany.detached.ListRefCollEntity;
import org.jboss.envers.test.entities.StrTestEntity;
import org.jboss.envers.test.tools.TestTools;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicDetachedList extends AbstractEntityTest {
    private Integer str1_id;
    private Integer str2_id;

    private Integer coll1_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(ListRefCollEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        StrTestEntity str1 = new StrTestEntity("str1");
        StrTestEntity str2 = new StrTestEntity("str2");

        ListRefCollEntity coll1 = new ListRefCollEntity(3, "coll1");

        // Revision 1
        em.getTransaction().begin();

        em.persist(str1);
        em.persist(str2);

        coll1.setCollection(new ArrayList<StrTestEntity>());
        coll1.getCollection().add(str1);
        em.persist(coll1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        str2 = em.find(StrTestEntity.class, str2.getId());
        coll1 = em.find(ListRefCollEntity.class, coll1.getId());

        coll1.getCollection().add(str2);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        str1 = em.find(StrTestEntity.class, str1.getId());
        coll1 = em.find(ListRefCollEntity.class, coll1.getId());

        coll1.getCollection().remove(str1);

        em.getTransaction().commit();

        // Revision 4
        em.getTransaction().begin();

        coll1 = em.find(ListRefCollEntity.class, coll1.getId());

        coll1.getCollection().clear();

        em.getTransaction().commit();

        //

        str1_id = str1.getId();
        str2_id = str2.getId();

        coll1_id = coll1.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3, 4).equals(getVersionsReader().getRevisions(ListRefCollEntity.class, coll1_id));

        assert Arrays.asList(1).equals(getVersionsReader().getRevisions(StrTestEntity.class, str1_id));
        assert Arrays.asList(1).equals(getVersionsReader().getRevisions(StrTestEntity.class, str2_id));
    }

    @Test
    public void testHistoryOfColl1() {
        StrTestEntity str1 = getEntityManager().find(StrTestEntity.class, str1_id);
        StrTestEntity str2 = getEntityManager().find(StrTestEntity.class, str2_id);

        ListRefCollEntity rev1 = getVersionsReader().find(ListRefCollEntity.class, coll1_id, 1);
        ListRefCollEntity rev2 = getVersionsReader().find(ListRefCollEntity.class, coll1_id, 2);
        ListRefCollEntity rev3 = getVersionsReader().find(ListRefCollEntity.class, coll1_id, 3);
        ListRefCollEntity rev4 = getVersionsReader().find(ListRefCollEntity.class, coll1_id, 4);

        assert TestTools.checkList(rev1.getCollection(), str1);
        assert TestTools.checkList(rev2.getCollection(), str1, str2);
        assert TestTools.checkList(rev3.getCollection(), str2);
        assert TestTools.checkList(rev4.getCollection());
        
        assert "coll1".equals(rev1.getData());
        assert "coll1".equals(rev2.getData());
        assert "coll1".equals(rev3.getData());
        assert "coll1".equals(rev4.getData());
    }
}