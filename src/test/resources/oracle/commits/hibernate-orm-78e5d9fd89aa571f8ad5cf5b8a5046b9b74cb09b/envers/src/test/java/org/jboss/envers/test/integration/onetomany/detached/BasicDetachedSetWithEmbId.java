package org.jboss.envers.test.integration.onetomany.detached;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.onetomany.detached.ids.SetRefCollEntityEmbId;
import org.jboss.envers.test.entities.ids.EmbIdTestEntity;
import org.jboss.envers.test.entities.ids.EmbId;
import org.jboss.envers.test.tools.TestTools;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicDetachedSetWithEmbId extends AbstractEntityTest {
    private EmbId str1_id;
    private EmbId str2_id;

    private EmbId coll1_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EmbIdTestEntity.class);
        cfg.addAnnotatedClass(SetRefCollEntityEmbId.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        str1_id = new EmbId(1, 2);
        str2_id = new EmbId(3, 4);

        coll1_id = new EmbId(5, 6);

        EmbIdTestEntity str1 = new EmbIdTestEntity(str1_id, "str1");
        EmbIdTestEntity str2 = new EmbIdTestEntity(str2_id, "str2");

        SetRefCollEntityEmbId coll1 = new SetRefCollEntityEmbId(coll1_id, "coll1");

        // Revision 1
        em.getTransaction().begin();

        em.persist(str1);
        em.persist(str2);

        coll1.setCollection(new HashSet<EmbIdTestEntity>());
        coll1.getCollection().add(str1);
        em.persist(coll1);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        str2 = em.find(EmbIdTestEntity.class, str2.getId());
        coll1 = em.find(SetRefCollEntityEmbId.class, coll1.getId());

        coll1.getCollection().add(str2);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        str1 = em.find(EmbIdTestEntity.class, str1.getId());
        coll1 = em.find(SetRefCollEntityEmbId.class, coll1.getId());

        coll1.getCollection().remove(str1);

        em.getTransaction().commit();

        // Revision 4
        em.getTransaction().begin();

        coll1 = em.find(SetRefCollEntityEmbId.class, coll1.getId());

        coll1.getCollection().clear();

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3, 4).equals(getVersionsReader().getRevisions(SetRefCollEntityEmbId.class, coll1_id));

        assert Arrays.asList(1).equals(getVersionsReader().getRevisions(EmbIdTestEntity.class, str1_id));
        assert Arrays.asList(1).equals(getVersionsReader().getRevisions(EmbIdTestEntity.class, str2_id));
    }

    @Test
    public void testHistoryOfColl1() {
        EmbIdTestEntity str1 = getEntityManager().find(EmbIdTestEntity.class, str1_id);
        EmbIdTestEntity str2 = getEntityManager().find(EmbIdTestEntity.class, str2_id);

        SetRefCollEntityEmbId rev1 = getVersionsReader().find(SetRefCollEntityEmbId.class, coll1_id, 1);
        SetRefCollEntityEmbId rev2 = getVersionsReader().find(SetRefCollEntityEmbId.class, coll1_id, 2);
        SetRefCollEntityEmbId rev3 = getVersionsReader().find(SetRefCollEntityEmbId.class, coll1_id, 3);
        SetRefCollEntityEmbId rev4 = getVersionsReader().find(SetRefCollEntityEmbId.class, coll1_id, 4);

        assert rev1.getCollection().equals(TestTools.makeSet(str1));
        assert rev2.getCollection().equals(TestTools.makeSet(str1, str2));
        assert rev3.getCollection().equals(TestTools.makeSet(str2));
        assert rev4.getCollection().equals(TestTools.makeSet());

        assert "coll1".equals(rev1.getData());
        assert "coll1".equals(rev2.getData());
        assert "coll1".equals(rev3.getData());
        assert "coll1".equals(rev4.getData());
    }
}