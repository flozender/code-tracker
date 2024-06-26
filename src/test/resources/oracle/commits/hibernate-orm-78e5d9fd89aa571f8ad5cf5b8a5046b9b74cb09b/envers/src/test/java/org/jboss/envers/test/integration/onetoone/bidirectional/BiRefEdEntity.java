package org.jboss.envers.test.integration.onetoone.bidirectional;

import org.jboss.envers.Versioned;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Id;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class BiRefEdEntity {
    @Id
    private Integer id;

    @Versioned
    private String data;

    @Versioned
    @OneToOne(mappedBy="reference")
    private BiRefIngEntity referencing;

    public BiRefEdEntity() {
    }

    public BiRefEdEntity(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public BiRefEdEntity(Integer id, String data, BiRefIngEntity referencing) {
        this.id = id;
        this.data = data;
        this.referencing = referencing;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public BiRefIngEntity getReferencing() {
        return referencing;
    }

    public void setReferencing(BiRefIngEntity referencing) {
        this.referencing = referencing;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BiRefEdEntity)) return false;

        BiRefEdEntity that = (BiRefEdEntity) o;

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
}
