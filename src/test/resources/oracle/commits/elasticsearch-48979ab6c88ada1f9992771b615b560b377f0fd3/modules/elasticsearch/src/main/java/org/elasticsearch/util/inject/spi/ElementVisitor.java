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
 * Visit elements.
 *
 * @param <V> any type to be returned by the visit method. Use {@link Void} with
 *     {@code return null} if no return type is needed.
 * 
 * @since 2.0
 */
public interface ElementVisitor<V> {

  /**
   * Visit a mapping from a key (type and optional annotation) to the strategy for getting
   * instances of the type.
   */
  <T> V visit(Binding<T> binding);

  /**
   * Visit a registration of a scope annotation with the scope that implements it.
   */
  V visit(ScopeBinding binding);

  /**
   * Visit a registration of type converters for matching target types.
   */
  V visit(TypeConverterBinding binding);

  /**
   * Visit a request to inject the instance fields and methods of an instance.
   */
  V visit(InjectionRequest request);

  /**
   * Visit a request to inject the static fields and methods of type.
   */
  V visit(StaticInjectionRequest request);

  /**
   * Visit a lookup of the provider for a type.
   */
  <T> V visit(ProviderLookup<T> lookup);

  /**
   * Visit a lookup of the members injector.
   */
  <T> V visit(MembersInjectorLookup<T> lookup);

  /**
   * Visit an error message and the context in which it occured.
   */
  V visit(Message message);

  /**
   * Visit a collection of configuration elements for a {@linkplain org.elasticsearch.util.inject.PrivateBinder
   * private binder}.
   */
  V visit(PrivateElements elements);

  /**
   * Visit an injectable type listener binding.
   */
  V visit(TypeListenerBinding binding);
}
