//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/* ------------------------------------------------------------ */
/** 
 * <p>A Ternary Trie String lookup data structure.</p>
 * This Trie is of a fixed size and cannot grow (which can be a good thing with regards to DOS when used as a cache).
 * <p>
 * The Trie is stored in 3 arrays:<dl>
 * <dt>char[] _tree</dt><dd>This is semantically 2 dimensional array flattened into a 1 dimensional char array. The second dimension
 * is that every 4 sequential elements represents a row of: character; hi index; eq index; low index, used to build a
 * ternary trie of key strings.</dd>
 * <dt>String[] _key<dt><dd>An array of key values where each element matches a row in the _tree array. A non zero key element 
 * indicates that the _tree row is a complete key rather than an intermediate character of a longer key.</dd>
 * <dt>V[] _value</dt><dd>An array of values corresponding to the _key array</dd>
 * </dl>
 * </p>
 * <p>The lookup of a value will iterate through the _tree array matching characters. If the equal tree branch is followed,
 * then the _key array is looked up to see if this is a complete match.  If a match is found then the _value array is looked up
 * to return the matching value.
 * </p>
 * <p>
 * This Trie may be instantiated either as case sensitive or insensitive.
 * </p>
 * <p>This Trie is not Threadsafe and contains no mutual exclusion 
 * or deliberate memory barriers.  It is intended for an ArrayTrie to be
 * built by a single thread and then used concurrently by multiple threads
 * and not mutated during that access.  If concurrent mutations of the
 * Trie is required external locks need to be applied.
 * </p>
 * 
 * @param <V>
 */
public class ArrayTernaryTrie<V> extends AbstractTrie<V>
{
    private static int LO=1;
    private static int EQ=2;
    private static int HI=3;
    
    /**
     * The Size of a Trie row is the char, and the low, equal and high
     * child pointers
     */
    private static final int ROW_SIZE = 4;
    
    /**
     * The Trie rows in a single array which allows a lookup of row,character
     * to the next row in the Trie.  This is actually a 2 dimensional
     * array that has been flattened to achieve locality of reference.
     */
    private final char[] _tree;
    
    /**
     * The key (if any) for a Trie row. 
     * A row may be a leaf, a node or both in the Trie tree.
     */
    private final String[] _key;
    
    /**
     * The value (if any) for a Trie row. 
     * A row may be a leaf, a node or both in the Trie tree.
     */
    private final V[] _value;
    
    /**
     * The number of rows allocated
     */
    private char _rows;

    /* ------------------------------------------------------------ */
    /** Create a case insensitive Trie of default capacity.
     */
    public ArrayTernaryTrie()
    {
        this(128);
    }
    
    /* ------------------------------------------------------------ */
    /** Create a Trie of default capacity
     * @param insensitive true if the Trie is insensitive to the case of the key.
     */
    public ArrayTernaryTrie(boolean insensitive)
    {
        this(insensitive,128);
    }

    /* ------------------------------------------------------------ */
    /** Create a case insensitive Trie
     * @param capacity  The capacity of the Trie, which is in the worst case
     * is the total number of characters of all keys stored in the Trie.
     * The capacity needed is dependent of the shared prefixes of the keys.
     * For example, a capacity of 6 nodes is required to store keys "foo" 
     * and "bar", but a capacity of only 4 is required to
     * store "bar" and "bat".
     */
    public ArrayTernaryTrie(int capacity)
    {
        this(true,capacity);
    }
    
    /* ------------------------------------------------------------ */
    /** Create a Trie
     * @param insensitive true if the Trie is insensitive to the case of the key.
     * @param capacity The capacity of the Trie, which is in the worst case
     * is the total number of characters of all keys stored in the Trie.
     * The capacity needed is dependent of the shared prefixes of the keys.
     * For example, a capacity of 6 nodes is required to store keys "foo" 
     * and "bar", but a capacity of only 4 is required to
     * store "bar" and "bat".
     */
    public ArrayTernaryTrie(boolean insensitive, int capacity)
    {
        super(insensitive);
        _value=(V[])new Object[capacity];
        _tree=new char[capacity*ROW_SIZE];
        _key=new String[capacity];
    }

    /* ------------------------------------------------------------ */
    /** Copy Trie and change capacity by a factor
     * @param trie
     * @param factor
     */
    public ArrayTernaryTrie(ArrayTernaryTrie<V> trie, double factor)
    {
        super(trie.isCaseInsensitive());
        int capacity=(int)(trie._value.length*factor);
        _rows=trie._rows;
        _value=Arrays.copyOf(trie._value, capacity);
        _tree=Arrays.copyOf(trie._tree, capacity*ROW_SIZE);
        _key=Arrays.copyOf(trie._key, capacity);
    }
    
    /* ------------------------------------------------------------ */
    @Override
    public boolean put(String s, V v)
    {
        int t=0;
        int limit = s.length();
        int last=0;
        for(int k=0; k < limit; k++)
        {
            char c=s.charAt(k);
            if(isCaseInsensitive() && c<128)
                c=StringUtil.lowercases[c];
            
            while (true)
            {
                int row=ROW_SIZE*t;
                
                // Do we need to create the new row?
                if (t==_rows)
                {
                    _rows++;
                    if (_rows>=_key.length)
                    {
                        _rows--;
                        return false;
                    }
                    _tree[row]=c;
                }

                char n=_tree[row];
                int diff=n-c;
                if (diff==0)
                    t=_tree[last=(row+EQ)];
                else if (diff<0)
                    t=_tree[last=(row+LO)];
                else
                    t=_tree[last=(row+HI)];
                
                // do we need a new row?
                if (t==0)
                {
                    t=_rows;
                    _tree[last]=(char)t;
                }
                
                if (diff==0)
                    break;
            }
        }

        // Do we need to create the new row?
        if (t==_rows)
        {
            _rows++;
            if (_rows>=_key.length)
            {
                _rows--;
                return false;
            }
        }

        // Put the key and value
        _key[t]=v==null?null:s;
        _value[t] = v;
                
        return true;
    }
    

    /* ------------------------------------------------------------ */
    @Override
    public V get(String s,int offset, int len)
    {
        int t = 0;
        for(int i=0; i < len;)
        {
            char c=s.charAt(offset+i++);
            if(isCaseInsensitive() && c<128)
                c=StringUtil.lowercases[c];
            
            while (true)
            {
                int row = ROW_SIZE*t;
                char n=_tree[row];
                int diff=n-c;
                
                if (diff==0)
                {
                    t=_tree[row+EQ];
                    if (t==0)
                        return null;
                    break;
                }

                t=_tree[row+hilo(diff)];
                if (t==0)
                    return null;
            }
        }
        
        return _value[t];
    }

    
    @Override
    public V get(ByteBuffer b, int offset, int len)
    {
        int t = 0;
        offset+=b.position();
        
        for(int i=0; i < len;)
        {
            byte c=(byte)(b.get(offset+i++)&0x7f);
            if(isCaseInsensitive())
                c=(byte)StringUtil.lowercases[c];
            
            while (true)
            {
                int row = ROW_SIZE*t;
                char n=_tree[row];
                int diff=n-c;
                
                if (diff==0)
                {
                    t=_tree[row+EQ];
                    if (t==0)
                        return null;
                    break;
                }

                t=_tree[row+hilo(diff)];
                if (t==0)
                    return null;
            }
        }

        return (V)_value[t];
    }

    /* ------------------------------------------------------------ */
    @Override
    public V getBest(String s)
    {
        return getBest(0,s,0,s.length());
    }
    
    /* ------------------------------------------------------------ */
    @Override
    public V getBest(String s, int offset, int length)
    {
        return getBest(0,s,offset,length);
    }

    /* ------------------------------------------------------------ */
    private V getBest(int t,String s,int offset,int len)
    {
        int node=t;
        loop: for(int i=0; i<len; i++)
        {
            char c=s.charAt(offset+i);
            if(isCaseInsensitive() && c<128)
                c=StringUtil.lowercases[c];

            while (true)
            {
                int row = ROW_SIZE*t;
                char n=_tree[row];
                int diff=n-c;
                
                if (diff==0)
                {
                    t=_tree[row+EQ];
                    if (t==0)
                        break loop;
                    
                    // if this node is a match, recurse to remember 
                    if (_key[t]!=null)
                    {
                        node=t;
                        V best=getBest(t,s,offset+i+1,len-i-1);
                        if (best!=null)
                            return best;
                    }
                    break;
                }

                t=_tree[row+hilo(diff)];
                if (t==0)
                    break loop;
            }
        }
        return (V)_value[node];
    }


    /* ------------------------------------------------------------ */
    @Override
    public V getBest(ByteBuffer b, int offset, int len)
    {
        if (b.hasArray())
            return getBest(0,b.array(),b.arrayOffset()+b.position()+offset,len);
        return getBest(0,b,offset,len);
    }

    /* ------------------------------------------------------------ */
    private V getBest(int t,byte[] b, int offset, int len)
    {
        int node=t;
        loop: for(int i=0; i<len; i++)
        {
            byte c=(byte)(b[offset+i]&0x7f);
            if(isCaseInsensitive())
                c=(byte)StringUtil.lowercases[c];

            while (true)
            {
                int row = ROW_SIZE*t;
                char n=_tree[row];
                int diff=n-c;
                
                if (diff==0)
                {
                    t=_tree[row+EQ];
                    if (t==0)
                        break loop;
                    
                    // if this node is a match, recurse to remember 
                    if (_key[t]!=null)
                    {
                        node=t;
                        V best=getBest(t,b,offset+i+1,len-i-1);
                        if (best!=null)
                            return best;
                    }
                    break;
                }

                t=_tree[row+hilo(diff)];
                if (t==0)
                    break loop;
            }
        }
        return (V)_value[node];
    }

    /* ------------------------------------------------------------ */
    private V getBest(int t,ByteBuffer b, int offset, int len)
    {
        int node=t;
        int o= offset+b.position();
        
        loop: for(int i=0; i<len; i++)
        {
            byte c=(byte)(b.get(o+i)&0x7f);
            if(isCaseInsensitive())
                c=(byte)StringUtil.lowercases[c];

            while (true)
            {
                int row = ROW_SIZE*t;
                char n=_tree[row];
                int diff=n-c;
                
                if (diff==0)
                {
                    t=_tree[row+EQ];
                    if (t==0)
                        break loop;
                    
                    // if this node is a match, recurse to remember 
                    if (_key[t]!=null)
                    {
                        node=t;
                        V best=getBest(t,b,offset+i+1,len-i-1);
                        if (best!=null)
                            return best;
                    }
                    break;
                }

                t=_tree[row+hilo(diff)];
                if (t==0)
                    break loop;
            }
        }
        return (V)_value[node];
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for (int r=0;r<=_rows;r++)
        {
            if (_key[r]!=null && _value[r]!=null)
            {
                buf.append(',');
                buf.append(_key[r]);
                buf.append('=');
                buf.append(_value[r].toString());
            }
        }
        if (buf.length()==0)
            return "{}";
        
        buf.setCharAt(0,'{');
        buf.append('}');
        return buf.toString();
    }



    @Override
    public Set<String> keySet()
    {
        Set<String> keys = new HashSet<>();

        for (int r=0;r<=_rows;r++)
        {
            if (_key[r]!=null && _value[r]!=null)
                keys.add(_key[r]);
        }
        return keys;
    }

    @Override
    public boolean isFull()
    {
        return _rows+1==_key.length;
    }
    
    public static int hilo(int diff)
    {
        // branchless equivalent to return ((diff<0)?LO:HI);
        // return 3+2*((diff&Integer.MIN_VALUE)>>Integer.SIZE-1);
        return 1+(diff|Integer.MAX_VALUE)/(Integer.MAX_VALUE/2);
    }
    
    public void dump()
    {
        for (int r=0;r<_rows;r++)
        {
            char c=_tree[r*ROW_SIZE+0];
            System.err.printf("%4d [%s,%d,%d,%d] '%s':%s%n",
                r,
                (c<' '||c>127)?(""+(int)c):"'"+c+"'",
                (int)_tree[r*ROW_SIZE+LO],
                (int)_tree[r*ROW_SIZE+EQ],
                (int)_tree[r*ROW_SIZE+HI],
                _key[r],
                _value[r]);
        }
        
    }
}
