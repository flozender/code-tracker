/**
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.util.inject.spi;

import org.elasticsearch.util.inject.Binding;

/**
 * No-op visitor for subclassing. All interface methods simply delegate to {@link
 * #visitOther(Binding)}, returning its result.
 *
 * @param <V> any type to be returned by the visit method. Use {@link Void} with
 *     {@code return null} if no return type is needed.
 *
 * @author jessewilson@google.com (Jesse Wilson)
 * @since 2.0
 */
public abstract class DefaultBindingTargetVisitor<T, V> implements BindingTargetVisitor<T, V> {

  /**
   * Default visit implementation. Returns {@code null}.
   */
  protected V visitOther(Binding<? extends T> binding) {
    return null;
  }

  public V visit(InstanceBinding<? extends T> instanceBinding) {
    return visitOther(instanceBinding);
  }

  public V visit(ProviderInstanceBinding<? extends T> providerInstanceBinding) {
    return visitOther(providerInstanceBinding);
  }

  public V visit(ProviderKeyBinding<? extends T> providerKeyBinding) {
    return visitOther(providerKeyBinding);
  }

  public V visit(LinkedKeyBinding<? extends T> linkedKeyBinding) {
    return visitOther(linkedKeyBinding);
  }

  public V visit(ExposedBinding<? extends T> exposedBinding) {
    return visitOther(exposedBinding);
  }

  public V visit(UntargettedBinding<? extends T> untargettedBinding) {
    return visitOther(untargettedBinding);
  }

  public V visit(ConstructorBinding<? extends T> constructorBinding) {
    return visitOther(constructorBinding);
  }

  public V visit(ConvertedConstantBinding<? extends T> convertedConstantBinding) {
    return visitOther(convertedConstantBinding);
  }

   // javac says it's an error to cast ProviderBinding<? extends T> to Binding<? extends T>
  @SuppressWarnings("unchecked")
  public V visit(ProviderBinding<? extends T> providerBinding) {
    return visitOther((Binding) providerBinding);
  }
}
