package org.mockito.internal.invocation.finder;

import org.mockito.internal.invocation.Invocation;
import org.mockito.internal.util.ListUtil;

import java.util.List;

/**
 * Author: Szczepan Faber, created at: 4/3/11
 */
public class VerifiableInvocationsFinder {

    public List<Invocation> find(List<?> mocks) {
        List<Invocation> invocations = new AllInvocationsFinder().find(mocks);
        return ListUtil.filter(invocations, new RemoveIgnoredForVerification());
    }

    static class RemoveIgnoredForVerification implements ListUtil.Filter<Invocation>{
        public boolean isOut(Invocation i) {
            return i.isIgnoredForVerification();
        }
    }
}
