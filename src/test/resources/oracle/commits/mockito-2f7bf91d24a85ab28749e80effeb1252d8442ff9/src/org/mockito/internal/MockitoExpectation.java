/*
 * Copyright (c) 2007 Mockito contributors 
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal;


public interface MockitoExpectation<T> {

    void andReturn(T value);

    void andThrows(Throwable throwable);
}