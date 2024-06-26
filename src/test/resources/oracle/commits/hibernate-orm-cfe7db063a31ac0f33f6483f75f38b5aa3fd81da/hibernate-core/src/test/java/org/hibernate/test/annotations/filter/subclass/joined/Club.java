package org.hibernate.test.annotations.filter.subclass.joined;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;

@Entity
@FilterDefs({
	@FilterDef(name="iqMin", parameters={@ParamDef(name="min", type="integer")}),
	@FilterDef(name="pregnantMembers")})
public class Club {
	@Id
	@GeneratedValue
	@Column(name="CLUB_ID")
	private int id;
	
	private String name;
	
	@OneToMany(mappedBy="club")
	@Filters({
		@Filter(name="iqMin", table="ZOOLOGY_HUMAN", condition="HUMAN_IQ >= :min"),
		@Filter(name="pregnantMembers", table="ZOOLOGY_MAMMAL", condition="IS_PREGNANT=1")
	})
	private Set<Human> members = new HashSet<Human>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Human> getMembers() {
		return members;
	}

	public void setMembers(Set<Human> members) {
		this.members = members;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
