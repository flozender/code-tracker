/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.stubbing;



public interface VoidMethodStubable<T> {

    StubbedMethodSelector<T> toThrow(Throwable throwable);

}
