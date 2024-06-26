package com.github.javaparser.ast;

import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.observer.AstObserver;
import com.github.javaparser.ast.observer.Observable;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * A list of nodes.
 * It usually has a parent node.
 * Different from normal Nodes, this does not mean that it is a child of that parent.
 * Instead, this list will make every node it contains a child of its parent.
 * This way, a NodeList does not create an extra level inside the AST.
 *
 * @param <N> the type of nodes contained.
 */
public class NodeList<N extends Node> implements List<N>, Iterable<N>, HasParentNode<NodeList<N>>, Visitable, Observable {
    private List<N> innerList = new ArrayList<>(0);

    private Node parentNode;

    private List<AstObserver> observers = new ArrayList<>();

    public NodeList() {
        this(null);
    }

    public NodeList(Node parent) {
        setParentNode(parent);
    }

    @Override
    public boolean add(N node) {
        notifyElementAdded(innerList.size(), node);
        own(node);
        return innerList.add(node);
    }

    private void own(N node) {
        if (node == null) {
            return;
        }
        setAsParentNodeOf(node);
    }

    public boolean remove(Node node) {
        int index = innerList.indexOf(node);
        if (index != -1) {
            notifyElementRemoved(index, node);
            node.setParentNode(null);
        }
        return innerList.remove(node);
    }

    @SafeVarargs
    public static <X extends Node> NodeList<X> nodeList(X... nodes) {
        final NodeList<X> nodeList = new NodeList<>();
        Collections.addAll(nodeList, nodes);
        return nodeList;
    }

    public static <X extends Node> NodeList<X> nodeList(Collection<X> nodes) {
        final NodeList<X> nodeList = new NodeList<>();
        for (X node : nodes) {
            nodeList.add(node);
        }
        return nodeList;
    }

    public static <X extends Node> NodeList<X> nodeList(NodeList<X> nodes) {
        final NodeList<X> nodeList = new NodeList<>();
        nodeList.addAll(nodes);
        return nodeList;
    }

    public boolean contains(N node) {
        return innerList.contains(node);
    }

    @Override
    public Stream<N> stream() {
        return innerList.stream();
    }

    @Override
    public int size() {
        return innerList.size();
    }

    @Override
    public N get(int i) {
        return innerList.get(i);
    }

    @Override
    public Iterator<N> iterator() {
        // TODO take care of "Iterator.remove"
        return innerList.iterator();
    }

    @Override
    public N set(int index, N element) {
        notifyElementRemoved(index, innerList.get(index));
        notifyElementAdded(index, element);
        setAsParentNodeOf(element);
        return innerList.set(index, element);
    }

    @Override
    public N remove(int index) {
        notifyElementRemoved(index, innerList.get(index));
        N remove = innerList.remove(index);
        if (remove != null)
            remove.setParentNode(null);
        return remove;
    }

    @Override
    public boolean isEmpty() {
        return innerList.isEmpty();
    }

    @Override
    public void sort(Comparator<? super N> comparator) {
        Collections.sort(innerList, comparator);
    }

    public void addAll(NodeList<N> otherList) {
        for (N node : otherList) {
            add(node);
        }
    }

    @Override
    public void add(int index, N node) {
        notifyElementAdded(index, node);
        own(node);
        innerList.add(index, node);
    }

    @Override
    public Optional<Node> getParentNode() {
        return Optional.ofNullable(parentNode);
    }

    /**
     * Sets the parentNode
     *
     * @param parentNode the parentNode
     * @return this, the NodeList
     */
    @Override
    public NodeList<N> setParentNode(Node parentNode) {
        this.parentNode = parentNode;
        setAsParentNodeOf(innerList);
        return this;
    }

    @Override
    public Node getParentNodeForChildren() {
        return parentNode;
    }

    @Override
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    /**
     * @see java.lang.Iterable#forEach(java.util.function.Consumer)
     */
    @Override
    public void forEach(Consumer<? super N> action) {
        innerList.forEach(action);
    }

    /**
     * @see java.util.List#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object o) {
        return innerList.contains(o);
    }

    /**
     * @see java.util.List#toArray()
     */
    @Override
    public Object[] toArray() {
        return innerList.toArray();
    }

    /**
     * @see java.util.List#toArray(java.lang.Object[])
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return innerList.toArray(a);
    }

    /**
     * @see java.util.List#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {
        if (o instanceof Node) {
            return remove((Node) o);
        } else {
            return false;
        }
    }

    /**
     * @see java.util.List#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return innerList.containsAll(c);
    }

    /**
     * @see java.util.List#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends N> c) {
        c.forEach(this::add);
        return !c.isEmpty();
    }

    /**
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll(int index, Collection<? extends N> c) {
        for (N e : c) {
            add(index++, e);
        }
        return !c.isEmpty();
    }

    /**
     * @see java.util.List#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object e : c) {
            changed = remove(e) || changed;
        }
        return changed;
    }

    /**
     * @see java.util.List#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        for (Object e : this.stream().filter(it -> !c.contains(it)).toArray()) {
            if (!c.contains(e)) {
                changed = remove(e) || changed;
            }
        }
        return changed;
    }

    /**
     * @see java.util.List#replaceAll(java.util.function.UnaryOperator)
     */
    @Override
    public void replaceAll(UnaryOperator<N> operator) {
        for (int i = 0; i < this.size(); i++) {
            set(i, operator.apply(this.get(i)));
        }
    }

    /**
     * @see java.util.Collection#removeIf(java.util.function.Predicate)
     */
    @Override
    public boolean removeIf(Predicate<? super N> filter) {
        boolean changed = false;
        for (Object e : this.stream().filter(filter).toArray()) {
            changed = remove(e) || changed;
        }
        return changed;
    }

    /**
     * @see java.util.List#clear()
     */
    @Override
    public void clear() {
        while (!isEmpty()) {
            remove(0);
        }
    }

    /**
     * @see java.util.List#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        return innerList.equals(o);
    }

    /**
     * @see java.util.List#hashCode()
     */
    @Override
    public int hashCode() {
        return innerList.hashCode();
    }

    /**
     * @see java.util.List#indexOf(java.lang.Object)
     */
    @Override
    public int indexOf(Object o) {
        return innerList.indexOf(o);
    }

    /**
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    @Override
    public int lastIndexOf(Object o) {
        return innerList.lastIndexOf(o);
    }

    /**
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator<N> listIterator() {
        return innerList.listIterator();
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    @Override
    public ListIterator<N> listIterator(int index) {
        return innerList.listIterator(index);
    }

    /**
     * @see java.util.Collection#parallelStream()
     */
    @Override
    public Stream<N> parallelStream() {
        return innerList.parallelStream();
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List<N> subList(int fromIndex, int toIndex) {
        return innerList.subList(fromIndex, toIndex);
    }

    /**
     * @see java.util.List#spliterator()
     */
    @Override
    public Spliterator<N> spliterator() {
        return innerList.spliterator();
    }

    private void notifyElementAdded(int index, Node nodeAddedOrRemoved) {
        this.observers.forEach(o -> o.listChange(this, AstObserver.ListChangeType.ADDITION, index, nodeAddedOrRemoved));
    }

    private void notifyElementRemoved(int index, Node nodeAddedOrRemoved) {
        this.observers.forEach(o -> o.listChange(this, AstObserver.ListChangeType.REMOVAL, index, nodeAddedOrRemoved));
    }

    @Override
    public void unregister(AstObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void register(AstObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public boolean isRegistered(AstObserver observer) {
        return this.observers.contains(observer);
    }

}
