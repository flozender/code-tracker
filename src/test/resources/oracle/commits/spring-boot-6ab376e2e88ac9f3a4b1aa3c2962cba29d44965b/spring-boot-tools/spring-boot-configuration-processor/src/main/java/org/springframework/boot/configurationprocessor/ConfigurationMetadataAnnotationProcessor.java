/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;

import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;
import org.springframework.boot.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;

/**
 * Annotation {@link Processor} that writes meta-data file for
 * {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Kris De Volder
 * @since 1.2.0
 */
@SupportedAnnotationTypes({ "*" })
public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {

	static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.boot."
			+ "context.properties.ConfigurationProperties";

	static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.boot."
			+ "context.properties.NestedConfigurationProperty";

	static final String LOMBOK_DATA_ANNOTATION = "lombok.Data";

	static final String LOMBOK_GETTER_ANNOTATION = "lombok.Getter";

	static final String LOMBOK_SETTER_ANNOTATION = "lombok.Setter";

	private MetadataStore metadataStore;

	private MetadataCollector metadataCollector;

	private TypeUtils typeUtils;

	private FieldValuesParser fieldValuesParser;

	private TypeExcludeFilter typeExcludeFilter = new TypeExcludeFilter();

	protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		this.typeUtils = new TypeUtils(env);
		this.metadataStore = new MetadataStore(env);
		this.metadataCollector = new MetadataCollector(env,
				this.metadataStore.readMetadata());
		try {
			this.fieldValuesParser = new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			this.fieldValuesParser = FieldValuesParser.NONE;
			logWarning("Field value processing of @ConfigurationProperty meta-data is "
					+ "not supported");
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		this.metadataCollector.processing(roundEnv);
		Elements elementUtils = this.processingEnv.getElementUtils();
		TypeElement annotationType = elementUtils
				.getTypeElement(configurationPropertiesAnnotation());
		if (annotationType != null) { // Is @ConfigurationProperties available
			for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
				processElement(element);
			}
		}
		if (roundEnv.processingOver()) {
			writeMetaData();
		}
		return false;
	}

	private void processElement(Element element) {
		try {
			AnnotationMirror annotation = getAnnotation(element,
					configurationPropertiesAnnotation());
			if (annotation != null) {
				String prefix = getPrefix(annotation);
				if (element instanceof TypeElement) {
					processAnnotatedTypeElement(prefix, (TypeElement) element);
				}
				else if (element instanceof ExecutableElement) {
					processExecutableElement(prefix, (ExecutableElement) element);
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processAnnotatedTypeElement(String prefix, TypeElement element) {
		String type = this.typeUtils.getType(element);
		this.metadataCollector.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element);
	}

	private void processExecutableElement(String prefix, ExecutableElement element) {
		if (element.getModifiers().contains(Modifier.PUBLIC)
				&& (TypeKind.VOID != element.getReturnType().getKind())) {
			Element returns = this.processingEnv.getTypeUtils()
					.asElement(element.getReturnType());
			if (returns instanceof TypeElement) {
				this.metadataCollector.add(
						ItemMetadata.newGroup(prefix, this.typeUtils.getType(returns),
								this.typeUtils.getType(element.getEnclosingElement()),
								element.toString()));
				processTypeElement(prefix, (TypeElement) returns);
			}
		}
	}

	private void processTypeElement(String prefix, TypeElement element) {
		TypeElementMembers members = new TypeElementMembers(this.processingEnv, element);
		Map<String, Object> fieldValues = getFieldValues(element);
		processSimpleTypes(prefix, element, members, fieldValues);
		processLombokTypes(prefix, element, members, fieldValues);
		processNestedTypes(prefix, element, members);
	}

	private Map<String, Object> getFieldValues(TypeElement element) {
		try {
			return this.fieldValuesParser.getFieldValues(element);
		}
		catch (Exception ex) {
			return Collections.emptyMap();
		}
	}

	private void processSimpleTypes(String prefix, TypeElement element,
			TypeElementMembers members, Map<String, Object> fieldValues) {
		for (Map.Entry<String, ExecutableElement> entry : members.getPublicGetters()
				.entrySet()) {
			String name = entry.getKey();
			ExecutableElement getter = entry.getValue();
			ExecutableElement setter = members.getPublicSetters().get(name);
			VariableElement field = members.getFields().get(name);
			TypeMirror returnType = getter.getReturnType();
			Element returnTypeElement = this.processingEnv.getTypeUtils()
					.asElement(returnType);
			boolean isExcluded = this.typeExcludeFilter.isExcluded(returnType);
			boolean isNested = isNested(returnTypeElement, field, element);
			boolean isCollection = this.typeUtils.isCollectionOrMap(returnType);
			if (!isExcluded && !isNested && (setter != null || isCollection)) {
				String dataType = this.typeUtils.getType(returnType);
				String sourceType = this.typeUtils.getType(element);
				String description = this.typeUtils.getJavaDoc(field);
				Object defaultValue = fieldValues.get(name);
				boolean deprecated = hasDeprecateAnnotation(getter)
						|| hasDeprecateAnnotation(setter)
						|| hasDeprecateAnnotation(element);
				this.metadataCollector
						.add(ItemMetadata.newProperty(prefix, name, dataType, sourceType,
								null, description, defaultValue, deprecated));
			}
		}
	}

	private void processLombokTypes(String prefix, TypeElement element,
			TypeElementMembers members, Map<String, Object> fieldValues) {
		for (Map.Entry<String, VariableElement> entry : members.getFields().entrySet()) {
			String name = entry.getKey();
			VariableElement field = entry.getValue();
			if (!isLombokField(field, element)) {
				continue;
			}
			TypeMirror returnType = field.asType();
			Element returnTypeElement = this.processingEnv.getTypeUtils()
					.asElement(returnType);
			boolean isExcluded = this.typeExcludeFilter.isExcluded(returnType);
			boolean isNested = isNested(returnTypeElement, field, element);
			boolean isCollection = this.typeUtils.isCollectionOrMap(returnType);
			boolean hasSetter = hasLombokSetter(field, element);
			if (!isExcluded && !isNested && (hasSetter || isCollection)) {
				String dataType = this.typeUtils.getType(returnType);
				String sourceType = this.typeUtils.getType(element);
				String description = this.typeUtils.getJavaDoc(field);
				Object defaultValue = fieldValues.get(name);
				boolean deprecated = hasDeprecateAnnotation(field)
						|| hasDeprecateAnnotation(element);
				this.metadataCollector
						.add(ItemMetadata.newProperty(prefix, name, dataType, sourceType,
								null, description, defaultValue, deprecated));
			}
		}
	}

	private boolean isLombokField(VariableElement field, TypeElement element) {
		return hasAnnotation(field, LOMBOK_GETTER_ANNOTATION)
				|| hasAnnotation(element, LOMBOK_GETTER_ANNOTATION)
				|| hasAnnotation(element, LOMBOK_DATA_ANNOTATION);
	}

	private boolean hasLombokSetter(VariableElement field, TypeElement element) {
		return !field.getModifiers().contains(Modifier.FINAL)
				&& (hasAnnotation(field, LOMBOK_SETTER_ANNOTATION)
						|| hasAnnotation(element, LOMBOK_SETTER_ANNOTATION)
						|| hasAnnotation(element, LOMBOK_DATA_ANNOTATION));
	}

	private void processNestedTypes(String prefix, TypeElement element,
			TypeElementMembers members) {
		for (Map.Entry<String, ExecutableElement> entry : members.getPublicGetters()
				.entrySet()) {
			String name = entry.getKey();
			ExecutableElement getter = entry.getValue();
			VariableElement field = members.getFields().get(name);
			Element returnType = this.processingEnv.getTypeUtils()
					.asElement(getter.getReturnType());
			AnnotationMirror annotation = getAnnotation(getter,
					configurationPropertiesAnnotation());
			boolean isNested = isNested(returnType, field, element);
			if (returnType != null && returnType instanceof TypeElement
					&& annotation == null && isNested) {
				String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, name);
				this.metadataCollector.add(ItemMetadata.newGroup(nestedPrefix,
						this.typeUtils.getType(returnType),
						this.typeUtils.getType(element), getter.toString()));
				processTypeElement(nestedPrefix, (TypeElement) returnType);
			}
		}
	}

	private boolean isNested(Element returnType, VariableElement field,
			TypeElement element) {
		if (hasAnnotation(field, nestedConfigurationPropertyAnnotation())) {
			return true;
		}
		return this.typeUtils.isEnclosedIn(returnType, element)
				&& returnType.getKind() != ElementKind.ENUM;
	}

	private boolean hasDeprecateAnnotation(Element element) {
		return hasAnnotation(element, "java.lang.Deprecated");
	}

	private boolean hasAnnotation(Element element, String type) {
		return getAnnotation(element, type) != null;
	}

	private AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

	private String getPrefix(AnnotationMirror annotation) {
		Map<String, Object> elementValues = getAnnotationElementValues(annotation);
		Object prefix = elementValues.get("prefix");
		if (prefix != null && !"".equals(prefix)) {
			return (String) prefix;
		}
		Object value = elementValues.get("value");
		if (value != null && !"".equals(value)) {
			return (String) value;
		}
		return null;
	}

	private Map<String, Object> getAnnotationElementValues(AnnotationMirror annotation) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation
				.getElementValues().entrySet()) {
			values.put(entry.getKey().getSimpleName().toString(),
					entry.getValue().getValue());
		}
		return values;
	}

	protected ConfigurationMetadata writeMetaData() {
		ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
		metadata = mergeAdditionalMetadata(metadata);
		if (!metadata.getItems().isEmpty()) {
			try {
				this.metadataStore.writeMetadata(metadata);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
			return metadata;
		}
		return null;
	}

	private ConfigurationMetadata mergeAdditionalMetadata(
			ConfigurationMetadata metadata) {
		try {
			ConfigurationMetadata merged = new ConfigurationMetadata(metadata);
			merged.addAll(this.metadataStore.readAdditionalMetadata());
			return merged;
		}
		catch (FileNotFoundException ex) {
			// No additional metadata
			return metadata;
		}
		catch (Exception ex) {
			logWarning("Unable to merge additional metadata");
			logWarning(getStackTrace(ex));
			return metadata;
		}
	}

	private String getStackTrace(Exception ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer, true));
		return writer.toString();
	}

	private void logWarning(String msg) {
		this.processingEnv.getMessager().printMessage(Kind.WARNING, msg);
	}

}
