package org.hibernate.envers.revisioninfo;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Returns modified entity names from a persisted revision info entity.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ModifiedEntityNamesReader {
    private final Getter modifiedEntityNamesGetter;

    public ModifiedEntityNamesReader(Class<?> revisionInfoClass, PropertyData modifiedEntityNamesData) {
        modifiedEntityNamesGetter = ReflectionTools.getGetter(revisionInfoClass, modifiedEntityNamesData);
    }

    @SuppressWarnings({"unchecked"})
    public Set<String> getModifiedEntityNames(Object revisionEntity) {
        Set<String> modifiedEntityNames = (Set<String>) modifiedEntityNamesGetter.get(revisionEntity);
        return modifiedEntityNames != null ? modifiedEntityNames : Collections.EMPTY_SET;
    }
}
