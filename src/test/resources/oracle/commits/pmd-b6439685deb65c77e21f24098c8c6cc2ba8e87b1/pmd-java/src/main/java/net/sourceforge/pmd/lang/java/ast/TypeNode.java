/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.typeresolution.JavaTypeDefinition;

/**
 * This interface allows a Java Class to be associated with a node.
 */
public interface TypeNode extends Node {

    /**
     * Get the Java Class associated with this node.
     *
     * @return The Java Class, may return <code>null</code>.
     */
    Class<?> getType();

    /**
     * Get the TypeDefinition associated with this node. The Class object
     * contained in the TypeDefinition will always be equal to that which
     * is returned by <code>getType()</code>.
     *
     * @return The TypeDefinition, may return <code>null</code>
     */
    JavaTypeDefinition getTypeDefinition();

    /**
     * Set the TypeDefinition associated with this node.
     *
     * @param type A TypeDefinition object
     */
    void setTypeDefinition(JavaTypeDefinition type);

    /**
     * Set the Java Class associated with this node.
     *
     * @param type A Java Class
     */
    void setType(Class<?> type);
}
