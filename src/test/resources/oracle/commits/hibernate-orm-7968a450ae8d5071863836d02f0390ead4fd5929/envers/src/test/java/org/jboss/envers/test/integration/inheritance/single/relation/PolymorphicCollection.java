/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.jboss.envers.test.integration.inheritance.single.relation;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.tools.TestTools;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PolymorphicCollection extends AbstractEntityTest {
    private Integer ed_id1;
    private Integer c_id;
    private Integer p_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ChildIngEntity.class);
        cfg.addAnnotatedClass(ParentIngEntity.class);
        cfg.addAnnotatedClass(ReferencedEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        // Rev 1
        em.getTransaction().begin();

        ReferencedEntity re = new ReferencedEntity();
        em.persist(re);
        ed_id1 = re.getId();

        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();

        re = em.find(ReferencedEntity.class, ed_id1);

        ParentIngEntity pie = new ParentIngEntity("x");
        pie.setReferenced(re);
        em.persist(pie);
        p_id = pie.getId();

        em.getTransaction().commit();

        // Rev 3
        em.getTransaction().begin();
        
        re = em.find(ReferencedEntity.class, ed_id1);

        ChildIngEntity cie = new ChildIngEntity("y", 1l);
        cie.setReferenced(re);
        em.persist(cie);
        c_id = cie.getId();

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 3).equals(getVersionsReader().getRevisions(ReferencedEntity.class, ed_id1));
        assert Arrays.asList(2).equals(getVersionsReader().getRevisions(ParentIngEntity.class, p_id));
        assert Arrays.asList(3).equals(getVersionsReader().getRevisions(ChildIngEntity.class, c_id));
    }

    @Test
    public void testHistoryOfReferencedCollection() {      
        assert getVersionsReader().find(ReferencedEntity.class, ed_id1, 1).getReferencing().size() == 0;
        assert getVersionsReader().find(ReferencedEntity.class, ed_id1, 2).getReferencing().equals(
                TestTools.makeSet(new ParentIngEntity(p_id, "x")));
        assert getVersionsReader().find(ReferencedEntity.class, ed_id1, 3).getReferencing().equals(
                TestTools.makeSet(new ParentIngEntity(p_id, "x"), new ChildIngEntity(c_id, "y", 1l)));
    }
}