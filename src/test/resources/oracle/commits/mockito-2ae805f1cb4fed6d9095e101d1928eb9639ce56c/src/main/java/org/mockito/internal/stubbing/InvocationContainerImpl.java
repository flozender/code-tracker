/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.stubbing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.invocation.StubInfoImpl;
import org.mockito.internal.verification.DefaultRegisteredInvocations;
import org.mockito.internal.verification.RegisteredInvocations;
import org.mockito.internal.verification.SingleRegisteredInvocation;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.MatchableInvocation;
import org.mockito.mock.MockCreationSettings;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubbing;
import org.mockito.stubbing.ValidableAnswer;

import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;

@SuppressWarnings("unchecked")
public class InvocationContainerImpl implements InvocationContainer, Serializable {

    private static final long serialVersionUID = -5334301962749537177L;
    private final LinkedList<StubbedInvocationMatcher> stubbed = new LinkedList<StubbedInvocationMatcher>();
    private final List<Answer<?>> answersForStubbing = new ArrayList<Answer<?>>();
    private final RegisteredInvocations registeredInvocations;

    private MatchableInvocation invocationForStubbing;

    public InvocationContainerImpl(MockCreationSettings mockSettings) {
        this.registeredInvocations = createRegisteredInvocations(mockSettings);
    }

    @Override
    public void setInvocationForPotentialStubbing(MatchableInvocation invocation) {
        registeredInvocations.add(invocation.getInvocation());
        this.invocationForStubbing = invocation;
    }

    @Override
    public void resetInvocationForPotentialStubbing(MatchableInvocation invocationMatcher) {
        this.invocationForStubbing = invocationMatcher;
    }

    public void addAnswer(Answer answer) {
        registeredInvocations.removeLast();
        addAnswer(answer, false);
    }

    public void addConsecutiveAnswer(Answer answer) {
        addAnswer(answer, true);
    }

    /**
     * Adds new stubbed answer and returns the invocation matcher the answer was added to.
     */
    public StubbedInvocationMatcher addAnswer(Answer answer, boolean isConsecutive) {
        Invocation invocation = invocationForStubbing.getInvocation();
        mockingProgress().stubbingCompleted();
        if (answer instanceof ValidableAnswer) {
            ((ValidableAnswer) answer).validateFor(invocation);
        }

        synchronized (stubbed) {
            if (isConsecutive) {
                stubbed.getFirst().addAnswer(answer);
            } else {
                stubbed.addFirst(new StubbedInvocationMatcher(invocationForStubbing, answer));
            }
            return stubbed.getFirst();
        }
    }

    Object answerTo(Invocation invocation) throws Throwable {
        return findAnswerFor(invocation).answer(invocation);
    }

    @Override
    public StubbedInvocationMatcher findAnswerFor(Invocation invocation) {
        synchronized (stubbed) {
            for (StubbedInvocationMatcher s : stubbed) {
                if (s.matches(invocation)) {
                    s.markStubUsed(invocation);
                    invocation.markStubbed(new StubInfoImpl(s));
                    return s;
                }
            }
        }

        return null;
    }

    public void addAnswerForVoidMethod(Answer answer) {
        answersForStubbing.add(answer);
    }

    public void setAnswersForStubbing(List<Answer<?>> answers) {
        answersForStubbing.addAll(answers);
    }

    @Override
    public boolean hasAnswersForStubbing() {
        return !answersForStubbing.isEmpty();
    }

    @Override
    public boolean hasInvocationForPotentialStubbing() {
        return !registeredInvocations.isEmpty();
    }

    @Override
    public void setMethodForStubbing(MatchableInvocation invocation) {
        invocationForStubbing = invocation;
        assert hasAnswersForStubbing();
        for (int i = 0; i < answersForStubbing.size(); i++) {
            addAnswer(answersForStubbing.get(i), i != 0);
        }
        answersForStubbing.clear();
    }

    @Override
    public String toString() {
        return "invocationForStubbing: " + invocationForStubbing;
    }

    public List<Invocation> getInvocations() {
        return registeredInvocations.getAll();
    }

    public void clearInvocations() {
        registeredInvocations.clear();
    }

    public List<Stubbing> getStubbedInvocations() {
        return (List) stubbed;
    }

    public Object invokedMock() {
        return invocationForStubbing.getInvocation().getMock();
    }

    public MatchableInvocation getInvocationForStubbing() {
        return invocationForStubbing;
    }

    private RegisteredInvocations createRegisteredInvocations(MockCreationSettings mockSettings) {
        return mockSettings.isStubOnly()
          ? new SingleRegisteredInvocation()
          : new DefaultRegisteredInvocations();
    }
}
