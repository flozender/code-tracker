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
package org.jboss.envers.test.entities.onetomany.ids;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.jboss.envers.Versioned;
import org.jboss.envers.test.entities.ids.EmbId;

/**
 * ReferencIng entity
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SetRefIngEmbIdEntity {
    @EmbeddedId
    private EmbId id;

    @Versioned
    private String data;

    @Versioned
    @ManyToOne
    private SetRefEdEmbIdEntity reference;

    public SetRefIngEmbIdEntity() { }

    public SetRefIngEmbIdEntity(EmbId id, String data, SetRefEdEmbIdEntity reference) {
        this.id = id;
        this.data = data;
        this.reference = reference;
    }

    public SetRefIngEmbIdEntity(String data, SetRefEdEmbIdEntity reference) {
        this.data = data;
        this.reference = reference;
    }

    public EmbId getId() {
        return id;
    }

    public void setId(EmbId id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public SetRefEdEmbIdEntity getReference() {
        return reference;
    }

    public void setReference(SetRefEdEmbIdEntity reference) {
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetRefIngEmbIdEntity)) return false;

        SetRefIngEmbIdEntity that = (SetRefIngEmbIdEntity) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "SetRefIngEmbIdEntity(id = " + id + ", data = " + data + ")";
    }
}