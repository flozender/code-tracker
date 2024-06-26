/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage;

import static org.mockito.Matchers.*;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockitoutil.TestBase;

@SuppressWarnings("unchecked")
@Ignore
public class PlaygroundTest extends TestBase {

    @Mock IMethods mock;

    @Test
    public void testSomething() {
        anyString();
    }
    
    @Test
    public void testGetLastUpdates() {
        mock = Mockito.mock(IMethods.class);
        mock.simpleMethod();
    }
}