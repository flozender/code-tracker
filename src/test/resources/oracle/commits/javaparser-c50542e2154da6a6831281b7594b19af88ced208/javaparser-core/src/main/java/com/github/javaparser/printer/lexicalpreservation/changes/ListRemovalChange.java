package com.github.javaparser.printer.lexicalpreservation.changes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.observer.ObservableProperty;

/**
 * The removal of an element in a list.
 */
public class ListRemovalChange implements Change {
    private ObservableProperty observableProperty;
    private int index;

    public ListRemovalChange(ObservableProperty observableProperty, int index) {
        this.observableProperty = observableProperty;
        this.index = index;
    }

    @Override
    public Object getValue(ObservableProperty property, Node node) {
        if (property == observableProperty) {
            NodeList nodeList = new NodeList();
            Object currentRawValue = new NoChange().getValue(property, node);
            if (!(currentRawValue instanceof NodeList)){
                throw new IllegalStateException("Expected NodeList, found " + currentRawValue.getClass().getCanonicalName());
            }
            NodeList currentNodeList = (NodeList)currentRawValue;
            nodeList.addAll(currentNodeList);
            nodeList.remove(index);
            return nodeList;
        } else {
            return new NoChange().getValue(property, node);
        }
    }
}
