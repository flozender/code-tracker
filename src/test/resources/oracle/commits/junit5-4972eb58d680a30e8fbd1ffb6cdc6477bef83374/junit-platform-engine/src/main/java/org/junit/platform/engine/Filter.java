/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.platform.engine;

import static java.util.Arrays.asList;
import static org.junit.platform.commons.meta.API.Usage.Internal;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.CompositeFilter.alwaysIncluded;

import java.util.Collection;
import java.util.function.Predicate;

import org.junit.platform.commons.meta.API;
import org.junit.platform.commons.util.Preconditions;

/**
 * A {@link Filter} can be applied to determine if an object should be
 * <em>included</em> or <em>excluded</em> in a result set.
 *
 * <p>For example, tests may be filtered during or after test discovery
 * based on certain criteria.
 *
 * <p>Clients should not implement this interface directly but rather one of
 * its subinterfaces.
 *
 * @since 5.0
 * @see DiscoveryFilter
 */
@FunctionalInterface
@API(Internal)
public interface Filter<T> {

	/**
	 * Combine the supplied array of {@link Filter filters} into a new composite
	 * filter that will include elements if and only if all of the filters in the
	 * specified array include it.
	 *
	 * <p>If the array is empty, the returned filter will include all elements
	 * it is asked to filter.
	 *
	 * <p>If the length of the array is 1, this method will return the single
	 * filter contained in the array.
	 *
	 * @param filters the array of filters to compose; never {@code null}
	 * @see #composeFilters(Collection)
	 */
	@SafeVarargs
	static <T> Filter<T> composeFilters(Filter<T>... filters) {
		Preconditions.notNull(filters, "Filters must not be null");

		if (filters.length == 0) {
			return alwaysIncluded();
		}
		if (filters.length == 1) {
			return filters[0];
		}
		return new CompositeFilter<>(asList(filters));
	}

	/**
	 * Combine the supplied collection of {@link Filter filters} into a new
	 * composite filter that will include elements if and only if all of the
	 * filters in the specified collection include it.
	 *
	 * <p>If the collection is empty, the returned filter will include all
	 * elements it is asked to filter.
	 *
	 * <p>If the size of the collection is 1, this method will return the single
	 * filter contained in the collection.
	 *
	 * @param filters the collection of filters to compose; never {@code null}
	 * @see #composeFilters(Filter...)
	 */
	static <T> Filter<T> composeFilters(Collection<? extends Filter<T>> filters) {
		Preconditions.notNull(filters, "Filters must not be null");

		if (filters.isEmpty()) {
			return alwaysIncluded();
		}
		if (filters.size() == 1) {
			return getOnlyElement(filters);
		}
		return new CompositeFilter<>(filters);
	}

	/**
	 * Apply this filter to the supplied object.
	 */
	FilterResult apply(T object);

	/**
	 * Return a {@link Predicate} that returns {@code true} if this filter
	 * <em>includes</em> the object supplied to the predicate's
	 * {@link Predicate#test test} method.
	 */
	default Predicate<T> toPredicate() {
		return object -> apply(object).included();
	}

}
