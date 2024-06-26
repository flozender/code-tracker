package me.tomassetti.symbolsolver.logic;

import me.tomassetti.symbolsolver.model.declarations.ClassDeclaration;
import me.tomassetti.symbolsolver.model.declarations.InterfaceDeclaration;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.ReferenceType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractClassDeclaration extends AbstractTypeDeclaration implements ClassDeclaration {

    protected abstract ReferenceType object();

    @Override
    public boolean hasName() {
        return getQualifiedName() != null;
    }

    @Override
    public final List<ReferenceType> getAllSuperClasses() {
        // TODO it could specify type parameters: they should appear
        List<ReferenceType> superclasses = new ArrayList<>();
        ReferenceType superClass = getSuperClass();
        if (superClass != null) {
            superclasses.add(superClass);
            superclasses.addAll(superClass.getAllAncestors());
            superclasses.add(object());
        }
        boolean foundObject = false;
        for (int i = 0; i < superclasses.size(); i++) {
            if (superclasses.get(i).getQualifiedName().equals(Object.class.getCanonicalName())) {
                if (foundObject) {
                    superclasses.remove(i);
                    i--;
                } else {
                    foundObject = true;
                }
            }
        }
        return superclasses.stream().filter((s) -> s.getTypeDeclaration().isClass()).collect(Collectors.toList());
    }

    @Override
    public final List<InterfaceDeclaration> getAllInterfaces() {
        // TODO it could specify type parameters: they should appear
        List<InterfaceDeclaration> interfaces = new ArrayList<>();
        for (InterfaceDeclaration interfaceDeclaration : getInterfaces()) {
            interfaces.add(interfaceDeclaration);
            interfaces.addAll(interfaceDeclaration.getAllInterfacesExtended());
        }
        return interfaces;
    }

    protected abstract TypeSolver typeSolver();

}
