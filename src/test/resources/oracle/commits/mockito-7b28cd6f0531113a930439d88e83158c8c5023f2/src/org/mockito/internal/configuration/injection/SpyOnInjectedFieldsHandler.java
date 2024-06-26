package org.mockito.internal.configuration.injection;

import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.util.reflection.FieldReader;
import org.mockito.internal.util.reflection.FieldSetter;

import java.lang.reflect.Field;
import java.util.Set;

import static org.mockito.Mockito.withSettings;

/**
 * Handler for field annotated with &#64;InjectMocks and &#64;Spy.
 *
 * <p>
 * The handler assumes that field initialization AND injection already happened.
 * So if the field is still null, then nothing will happen there.
 * </p>
 */
public class SpyOnInjectedFieldsHandler extends MockInjectionStrategy {

    @Override
    protected boolean processInjection(Field field, Object fieldOwner, Set<Object> mockCandidates) {
        FieldReader fieldReader = new FieldReader(fieldOwner, field);

        // TODO refoctor : code duplicated in SpyAnnotationEngine
        if(!fieldReader.isNull() && field.isAnnotationPresent(Spy.class)) {
            try {
                Object instance = fieldReader.read();
                if (new MockUtil().isMock(instance)) {
                    // A. instance has been spied earlier
                    // B. protect against multiple use of MockitoAnnotations.initMocks()
                    Mockito.reset(instance);
                } else {
                    new FieldSetter(fieldOwner, field).set(
                        Mockito.mock(instance.getClass(), withSettings()
                            .spiedInstance(instance)
                            .defaultAnswer(Mockito.CALLS_REAL_METHODS)
                            .name(field.getName()))
                    );
                }
            } catch (Exception e) {
                throw new MockitoException("Problems initiating spied field " + field.getName(), e);
            }
        }

        return false;
    }
}
