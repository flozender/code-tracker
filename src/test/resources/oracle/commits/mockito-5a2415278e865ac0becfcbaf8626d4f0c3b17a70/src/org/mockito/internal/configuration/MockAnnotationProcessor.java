package org.mockito.internal.configuration;

import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import java.lang.reflect.Field;

/**
 * Instantiates a mock on a field annotated by {@link Mock}
 */
public class MockAnnotationProcessor implements FieldAnnotationProcessor<Mock> {
    public Object process(Mock annotation, Field field) {
        MockSettings mockSettings = Mockito.withSettings();
        if (annotation.extraInterfaces().length > 0) { // never null
            mockSettings.extraInterfaces(annotation.extraInterfaces());
        }
        if ("".equals(annotation.name())) {
            mockSettings.name(field.getName());
        } else {
            mockSettings.name(annotation.name());
        }

        // see @Mock answer default value
        mockSettings.defaultAnswer(annotation.answer().get());
        return Mockito.mock(field.getType(), mockSettings);
    }
}
