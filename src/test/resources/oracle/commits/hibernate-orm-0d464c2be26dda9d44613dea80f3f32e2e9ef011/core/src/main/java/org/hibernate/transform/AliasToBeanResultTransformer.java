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
 *
 */
package org.hibernate.transform;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;

/**
 * Result transformer that allows to transform a result to 
 * a user specified class which will be populated via setter  
 * methods or fields matching the alias names. 
 * 
 * <pre>
 * List resultWithAliasedBean = s.createCriteria(Enrolment.class)
 *			.createAlias("student", "st")
 *			.createAlias("course", "co")
 *			.setProjection( Projections.projectionList()
 *					.add( Projections.property("co.description"), "courseDescription" )
 *			)
 *			.setResultTransformer( new AliasToBeanResultTransformer(StudentDTO.class) )
 *			.list();
 *
 *  StudentDTO dto = (StudentDTO)resultWithAliasedBean.get(0);
 *	</pre>
 *
 * @author max
 *
 */
public class AliasToBeanResultTransformer implements ResultTransformer {
	
	private final Class resultClass;
	private Setter[] setters;
	private PropertyAccessor propertyAccessor;
	
	public AliasToBeanResultTransformer(Class resultClass) {
		if(resultClass==null) throw new IllegalArgumentException("resultClass cannot be null");
		this.resultClass = resultClass;
		propertyAccessor = new ChainedPropertyAccessor(new PropertyAccessor[] { PropertyAccessorFactory.getPropertyAccessor(resultClass,null), PropertyAccessorFactory.getPropertyAccessor("field")}); 		
	}

	public Object transformTuple(Object[] tuple, String[] aliases) {
		Object result;
		
		try {
			if(setters==null) {
				setters = new Setter[aliases.length];
				for (int i = 0; i < aliases.length; i++) {
					String alias = aliases[i];
					if(alias != null) {
						setters[i] = propertyAccessor.getSetter(resultClass, alias);
					}
				}
			}
			result = resultClass.newInstance();
			
			for (int i = 0; i < aliases.length; i++) {
				if(setters[i]!=null) {
					setters[i].set(result, tuple[i], null);
				}
			}
		} catch (InstantiationException e) {
			throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
		} catch (IllegalAccessException e) {
			throw new HibernateException("Could not instantiate resultclass: " + resultClass.getName());
		}
		
		return result;
	}

	public List transformList(List collection) {
		return collection;
	}

}
