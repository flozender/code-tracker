/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.util;

import org.mockito.exceptions.Reporter;
import org.mockito.internal.creation.MockSettingsImpl;
import org.mockito.internal.creation.jmock.ClassImposterizer;

import java.util.Collection;

@SuppressWarnings("unchecked")
public class MockCreationValidator {

    public boolean isTypeMockable(Class<?> clz) {
        return ClassImposterizer.INSTANCE.canImposterise(clz);
    }

    public void validateType(Class classToMock) {
        if (!isTypeMockable(classToMock)) {
            new Reporter().cannotMockFinalClass(classToMock);
        }
    }

    public void validateExtraInterfaces(Class classToMock, Collection<Class> extraInterfaces) {
        if (extraInterfaces == null) {
            return;
        }

        for (Class i : extraInterfaces) {
            if (classToMock == i) {
                new Reporter().extraInterfacesCannotContainMockedType(classToMock);
            }
        }
    }

    public void validateMockedType(Class classToMock, Object spiedInstance) {
        if (classToMock == null || spiedInstance == null) {
            return;
        }
        if (!classToMock.equals(spiedInstance.getClass())) {
            new Reporter().mockedTypeIsInconsistentWithSpiedInstanceType(classToMock, spiedInstance);
        }
    }

    public void validateDelegatedInstance(Class classToMock, Object delegatedInstance) {
    	if (classToMock == null || delegatedInstance == null) {
            return;
        }
    	if (delegatedInstance.getClass().isAssignableFrom(classToMock)) {
            new Reporter().mockedTypeIsInconsistentWithDelegatedInstanceType(classToMock, delegatedInstance);
        }
    }
}