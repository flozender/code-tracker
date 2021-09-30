package org.codetracker.element;

import gr.uom.java.xmi.UMLClass;
import org.codetracker.api.Version;

import static org.codetracker.util.Util.annotationsToString;
import static org.codetracker.util.Util.getPath;

public class Class extends BaseCodeElement {
    private final UMLClass umlClass;

    private Class(UMLClass umlClass, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlClass = umlClass;
    }

    public static Class of(UMLClass umlClass, Version version) {
        String sourceFolder = getPath(umlClass.getLocationInfo().getFilePath(), umlClass.getName());
        String packageName = umlClass.getPackageName();
        String name = umlClass.getName().replace(umlClass.getPackageName(), "").replace(".", "");
        String modifiersString = new ModifiersBuilder()
                .isFinal(umlClass.isFinal())
                .isStatic(umlClass.isStatic())
                .isAbstract(umlClass.isAbstract())
                .build();
        String visibility = umlClass.getVisibility();
        String identifierExcludeVersion = String.format("%s%s.(%s)%s%s%s", sourceFolder, packageName, visibility, modifiersString, name, annotationsToString(umlClass.getAnnotations()));

        return new Class(umlClass, identifierExcludeVersion, String.format("%s%s.(%s)%s%s(%d)", sourceFolder, packageName, visibility, modifiersString, name, umlClass.getLocationInfo().getStartLine()), umlClass.getLocationInfo().getFilePath(), version);
    }

    public UMLClass getUmlClass() {
        return umlClass;
    }
}
