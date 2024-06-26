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
package org.hibernate.sql.ordering.antlr;

import org.hibernate.Logger;
import org.hibernate.hql.ast.util.ASTPrinter;
import org.hibernate.util.StringHelper;
import antlr.collections.AST;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class OrderByFragmentRenderer extends GeneratedOrderByFragmentRenderer {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                OrderByFragmentRenderer.class.getPackage().getName());
	private static final ASTPrinter printer = new ASTPrinter( GeneratedOrderByFragmentRendererTokenTypes.class );

	@Override
    protected void out(AST ast) {
		out( ( ( Node ) ast ).getRenderableText() );
	}


	// handle trace logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private int traceDepth = 0;

	@Override
    public void traceIn(String ruleName, AST tree) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = StringHelper.repeat( '-', (traceDepth++ * 2) ) + "-> ";
		String traceText = ruleName + " (" + buildTraceNodeName(tree) + ")";
        LOG.trace(prefix + traceText);
	}

	private String buildTraceNodeName(AST tree) {
		return tree == null
				? "???"
				: tree.getText() + " [" + printer.getTokenTypeName( tree.getType() ) + "]";
	}

	@Override
    public void traceOut(String ruleName, AST tree) {
		if ( inputState.guessing > 0 ) {
			return;
		}
		String prefix = "<-" + StringHelper.repeat( '-', (--traceDepth * 2) ) + " ";
        LOG.trace(prefix + ruleName);
	}
}
