/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.support;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchInterruptedException;
import org.elasticsearch.ElasticSearchTimeoutException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.util.concurrent.AbstractFuture;
import org.elasticsearch.common.util.concurrent.UncategorizedExecutionException;
import org.elasticsearch.util.TimeValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author kimchy (shay.banon)
 */
public abstract class AdapterActionFuture<T, L> extends AbstractFuture<T> implements ActionFuture<T>, ActionListener<L> {

    @Override public T actionGet() throws ElasticSearchException {
        try {
            return get();
        } catch (InterruptedException e) {
            throw new ElasticSearchInterruptedException(e.getMessage());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ElasticSearchException) {
                throw (ElasticSearchException) e.getCause();
            } else {
                throw new UncategorizedExecutionException("Failed execution", e);
            }
        }
    }

    @Override public T actionGet(String timeout) throws ElasticSearchException {
        return actionGet(TimeValue.parseTimeValue(timeout, null));
    }

    @Override public T actionGet(long timeoutMillis) throws ElasticSearchException {
        return actionGet(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override public T actionGet(TimeValue timeout) throws ElasticSearchException {
        return actionGet(timeout.millis(), TimeUnit.MILLISECONDS);
    }

    @Override public T actionGet(long timeout, TimeUnit unit) throws ElasticSearchException {
        try {
            return get(timeout, unit);
        } catch (TimeoutException e) {
            throw new ElasticSearchTimeoutException(e.getMessage());
        } catch (InterruptedException e) {
            throw new ElasticSearchInterruptedException(e.getMessage());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ElasticSearchException) {
                throw (ElasticSearchException) e.getCause();
            } else {
                throw new UncategorizedExecutionException("Failed execution", e);
            }
        }
    }

    @Override public void onResponse(L result) {
        set(convert(result));
    }

    @Override public void onFailure(Throwable e) {
        setException(e);
    }

    protected abstract T convert(L listenerResponse);
}
