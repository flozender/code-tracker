/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.classreading;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;

/**
 * ASM class visitor which looks for the class name and implemented types as
 * well as for the annotations defined on the class, exposing them through
 * the {@link org.springframework.core.type.AnnotationMetadata} interface.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 */
final class AnnotationMetadataReadingVisitor extends ClassMetadataReadingVisitor implements AnnotationMetadata {

	private final Map<String, Map<String, Object>> annotationMap = new LinkedHashMap<String, Map<String, Object>>();

	private final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<String, Set<String>>();

	private final Set<MethodMetadata> methodMetadataSet = new LinkedHashSet<MethodMetadata>();

	private final ClassLoader classLoader;


	public AnnotationMetadataReadingVisitor(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodMetadataReadingVisitor mm = new MethodMetadataReadingVisitor(name, access, this.classLoader);
		this.methodMetadataSet.add(mm);
		return mm;
	}

	@Override
	public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
		String className = Type.getType(desc).getClassName();
		return new AnnotationAttributesReadingVisitor(className, this.annotationMap, this.metaAnnotationMap, this.classLoader);
	}


	public Set<String> getAnnotationTypes() {
		return this.annotationMap.keySet();
	}

	public boolean hasAnnotation(String annotationType) {
		return this.annotationMap.containsKey(annotationType);
	}

	public Map<String, Object> getAnnotationAttributes(String annotationType) {
		return this.annotationMap.get(annotationType);
	}

	public Set<String> getMetaAnnotationTypes(String annotationType) {
		return this.metaAnnotationMap.get(annotationType);
	}
	
	public boolean hasMetaAnnotation(String metaAnnotationType) {
		Collection<Set<String>> allMetaTypes = this.metaAnnotationMap.values();
		for (Set<String> metaTypes : allMetaTypes) {
			if (metaTypes.contains(metaAnnotationType)) {
				return true;
			}
		}
		return false;
	}

	public Set<MethodMetadata> getAnnotatedMethods() {
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<MethodMetadata>();
		for (MethodMetadata method : this.methodMetadataSet) {
			if (!method.getAnnotationTypes().isEmpty()) {
				annotatedMethods.add(method);
			}
		}
		return annotatedMethods;
	}

	public Set<MethodMetadata> getAnnotatedMethods(String annotationType) {
		Set<MethodMetadata> annotatedMethods = new LinkedHashSet<MethodMetadata>();
		for (MethodMetadata method : this.methodMetadataSet) {
			if (method.hasAnnotation(annotationType)) {
				annotatedMethods.add(method);
			}
		}
		return annotatedMethods;
	}

}
