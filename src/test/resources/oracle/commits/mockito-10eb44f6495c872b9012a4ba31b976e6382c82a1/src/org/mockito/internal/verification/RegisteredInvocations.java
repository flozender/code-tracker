/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.verification;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.mockito.internal.invocation.Invocation;
import org.mockito.internal.invocation.StubInfo;
import org.mockito.internal.util.ListUtil;
import org.mockito.internal.util.ListUtil.Filter;


public class RegisteredInvocations implements Serializable {

    private static final long serialVersionUID = -2674402327380736290L;
    private final List<Invocation> invocations = Collections.synchronizedList(new LinkedList<Invocation>());
    
    public void add(Invocation invocation) {
        invocations.add(invocation);
    }

    public void removeLast() {
        int last = invocations.size() - 1;
        invocations.remove(last);
    }

    public List<Invocation> getAll() {
        return ListUtil.filter(new LinkedList<Invocation>(invocations), new RemoveToString());
    }

    private static class RemoveToString implements Filter<Invocation> {
        public boolean isOut(Invocation invocation) {
            return Invocation.isToString(invocation);
        }
    }
}