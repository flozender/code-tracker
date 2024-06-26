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
package org.hibernate.envers.test.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.enhanced.DefaultRevisionEntity;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TestTools {
    public static <T> Set<T> makeSet(T... objects) {
        Set<T> ret = new HashSet<T>();
        //noinspection ManualArrayToCollectionCopy
        for (T o : objects) {
            ret.add(o);
        }

        return ret;
    }

    public static <T> List<T> makeList(T... objects) {
        return Arrays.asList(objects);
    }

    public static Map<Object, Object> makeMap(Object... objects) {
        Map<Object, Object> ret = new HashMap<Object, Object>();
        // The number of objects must be divisable by 2.
        //noinspection ManualArrayToCollectionCopy
        for (int i=0; i<objects.length; i+=2) {
            ret.put(objects[i], objects[i+1]);
        }

        return ret;
    }

    public static <T> boolean checkList(List<T> list, T... objects) {
        if (list.size() != objects.length) {
            return false;
        }

        for (T obj : objects) {
            if (!list.contains(obj)) {
                return false;
            }
        }

        return true;
    }

	public static List<Integer> extractRevisionNumbers(List queryResults) {
		List<Integer> result = new ArrayList<Integer>();
		for (Object queryResult : queryResults) {
			result.add(((DefaultRevisionEntity) ((Object[]) queryResult)[1])
					.getId());
		}
		return result;
	}

	public static Set<String> extractModProperties(
			PersistentClass persistentClass) {
		return extractModProperties(persistentClass,
				GlobalConfiguration.DEFAULT_MODIFIED_FLAG_SUFFIX);
	}

	public static Set<String> extractModProperties(
			PersistentClass persistentClass, String suffix) {
		Set<String> result = new HashSet<String>();
		Iterator iterator = persistentClass.getPropertyIterator();

		while (iterator.hasNext()) {
			Property property = (Property) iterator.next();
			String propertyName = property.getName();
			if (propertyName.endsWith(suffix)) {
				result.add(propertyName);
			}
		}
		return result;
	}
}
