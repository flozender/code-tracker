/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.comparator;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A decorator for a comparator, with an "ascending" flag denoting
 * whether comparison results should be treated in forward (standard
 * ascending) order or flipped for reverse (descending) order.
 * 
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public class InvertibleComparator implements Comparator, Serializable {

	private final Comparator comparator;

	private boolean ascending = true;


	/**
	 * Create an InvertibleComparator that sorts ascending by default.
	 * For the actual comparison, the specified Comparator will be used.
	 * @param comparator the comparator to decorate
	 */
	public InvertibleComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	/**
	 * Create an InvertibleComparator that sorts based on the provided order.
	 * For the actual comparison, the specified Comparator will be used.
	 * @param comparator the comparator to decorate
	 * @param ascending the sort order: ascending (true) or descending (false)
	 */
	public InvertibleComparator(Comparator comparator, boolean ascending) {
		this.comparator = comparator;
		setAscending(ascending);
	}


	/**
	 * Specify the sort order: ascending (true) or descending (false).
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * Return the sort order: ascending (true) or descending (false).
	 */
	public boolean isAscending() {
		return ascending;
	}

	/**
	 * Invert the sort order: ascending -> descending or
	 * descending -> ascending.
	 */
	public void invertOrder() {
		this.ascending = !this.ascending;
	}


	public int compare(Object o1, Object o2) {
		int result = this.comparator.compare(o1, o2);
		if (result != 0) {
			// Invert the order if it is a reverse sort.
			if (!this.ascending) {
				if (Integer.MIN_VALUE == result) {
					result = Integer.MAX_VALUE;
				}
				else {
					result *= -1;
				}
			}
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof InvertibleComparator)) {
			return false;
		}
		InvertibleComparator other = (InvertibleComparator) obj;
		return (this.comparator.equals(other.comparator) && this.ascending == other.ascending);
	}

	@Override
	public int hashCode() {
		return this.comparator.hashCode();
	}

	@Override
	public String toString() {
		return "InvertibleComparator: [" + this.comparator + "]; ascending=" + this.ascending;
	}

}
