/*
 * Copyright (C) 2007 Google Inc.
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

package org.elasticsearch.util.inject.internal;

import org.elasticsearch.util.inject.Injector;
import org.elasticsearch.util.inject.Key;
import org.elasticsearch.util.inject.Binder;
import org.elasticsearch.util.inject.spi.BindingTargetVisitor;
import org.elasticsearch.util.inject.spi.LinkedKeyBinding;

public final class LinkedBindingImpl<T> extends BindingImpl<T> implements LinkedKeyBinding<T> {

  final Key<? extends T> targetKey;

  public LinkedBindingImpl(Injector injector, Key<T> key, Object source,
      InternalFactory<? extends T> internalFactory, Scoping scoping,
      Key<? extends T> targetKey) {
    super(injector, key, source, internalFactory, scoping);
    this.targetKey = targetKey;
  }

  public LinkedBindingImpl(Object source, Key<T> key, Scoping scoping, Key<? extends T> targetKey) {
    super(source, key, scoping);
    this.targetKey = targetKey;
  }

  public <V> V acceptTargetVisitor(BindingTargetVisitor<? super T, V> visitor) {
    return visitor.visit(this);
  }

  public Key<? extends T> getLinkedKey() {
    return targetKey;
  }

  public BindingImpl<T> withScoping(Scoping scoping) {
    return new LinkedBindingImpl<T>(getSource(), getKey(), scoping, targetKey);
  }

  public BindingImpl<T> withKey(Key<T> key) {
    return new LinkedBindingImpl<T>(getSource(), key, getScoping(), targetKey);
  }

  public void applyTo(Binder binder) {
    getScoping().applyTo(binder.withSource(getSource()).bind(getKey()).to(getLinkedKey()));
  }

  @Override public String toString() {
    return new ToStringBuilder(LinkedKeyBinding.class)
        .add("key", getKey())
        .add("source", getSource())
        .add("scope", getScoping())
        .add("target", targetKey)
        .toString();
  }
}
