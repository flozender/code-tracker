//$Id$
package org.hibernate.search.test.filter;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Soap {
	@Id @DocumentId
	@GeneratedValue
	private Integer id;

	@Field
	private String perfume;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getPerfume() {
		return perfume;
	}

	public void setPerfume(String perfume) {
		this.perfume = perfume;
	}
}
