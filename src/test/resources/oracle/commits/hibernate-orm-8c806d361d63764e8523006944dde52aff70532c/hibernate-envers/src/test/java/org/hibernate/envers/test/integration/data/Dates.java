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
package org.hibernate.envers.test.integration.data;
import java.util.Arrays;
import java.util.Date;
import javax.persistence.EntityManager;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Dates extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(DateTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        DateTestEntity dte = new DateTestEntity(new Date(12345000));
        em.persist(dte);
        id1 = dte.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        dte = em.find(DateTestEntity.class, id1);
        dte.setDate(new Date(45678000));
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(DateTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        DateTestEntity ver1 = new DateTestEntity(id1, new Date(12345000));
        DateTestEntity ver2 = new DateTestEntity(id1, new Date(45678000));

        assert getAuditReader().find(DateTestEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(DateTestEntity.class, id1, 2).equals(ver2);
    }
}