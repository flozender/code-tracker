/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;

/**
 * Common contract for composite identifiers.  Specific sub-types include aggregated
 * (think {@link javax.persistence.EmbeddedId}) and non-aggregated (think
 * {@link javax.persistence.IdClass}).
 *
 * @author Steve Ebersole
 */
public interface CompositeIdentifierSource extends IdentifierSource, EmbeddableSourceContributor {
	/**
	 * Handle silly SpecJ reading of the JPA spec.  They believe composite identifiers should have "partial generation"
	 * capabilities.
	 *
	 * @param identifierAttributeName The name of the individual attribute within the composite identifier.
	 *
	 * @return The generator for the named attribute (within the composite).
	 */
	IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName);
}
