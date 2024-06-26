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

package org.elasticsearch.rest;

import com.google.inject.Inject;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.util.component.AbstractComponent;
import org.elasticsearch.util.component.Lifecycle;
import org.elasticsearch.util.component.LifecycleComponent;
import org.elasticsearch.util.path.PathTrie;
import org.elasticsearch.util.settings.Settings;

import java.io.IOException;

/**
 * @author kimchy (Shay Banon)
 */
public class RestController extends AbstractComponent implements LifecycleComponent<RestController> {

    private final Lifecycle lifecycle = new Lifecycle();

    private final PathTrie<RestHandler> getHandlers = new PathTrie<RestHandler>();
    private final PathTrie<RestHandler> postHandlers = new PathTrie<RestHandler>();
    private final PathTrie<RestHandler> putHandlers = new PathTrie<RestHandler>();
    private final PathTrie<RestHandler> deleteHandlers = new PathTrie<RestHandler>();

    @Inject public RestController(Settings settings) {
        super(settings);
    }

    @Override public Lifecycle.State lifecycleState() {
        return this.lifecycle.state();
    }

    @Override public RestController start() throws ElasticSearchException {
        if (!lifecycle.moveToStarted()) {
            return this;
        }
        return this;
    }

    @Override public RestController stop() throws ElasticSearchException {
        if (!lifecycle.moveToStopped()) {
            return this;
        }
        return this;
    }

    @Override public void close() throws ElasticSearchException {
        if (lifecycle.started()) {
            stop();
        }
        if (!lifecycle.moveToClosed()) {
            return;
        }
    }

    public void registerHandler(RestRequest.Method method, String path, RestHandler handler) {
        switch (method) {
            case GET:
                getHandlers.insert(path, handler);
                break;
            case DELETE:
                deleteHandlers.insert(path, handler);
                break;
            case POST:
                postHandlers.insert(path, handler);
                break;
            case PUT:
                putHandlers.insert(path, handler);
                break;
            default:
                throw new ElasticSearchIllegalArgumentException("Can't handle [" + method + "] for path [" + path + "]");
        }
    }

    public void dispatchRequest(final RestRequest request, final RestChannel channel) {
        final RestHandler handler = getHandler(request);
        try {
            handler.handleRequest(request, channel);
        } catch (Exception e) {
            try {
                channel.sendResponse(new JsonThrowableRestResponse(request, e));
            } catch (IOException e1) {
                logger.error("Failed to send failure response for uri [" + request.uri() + "]", e1);
            }
        }
    }

    private RestHandler getHandler(RestRequest request) {
        String path = getPath(request);
        RestRequest.Method method = request.method();
        if (method == RestRequest.Method.GET) {
            return getHandlers.retrieve(path, request.params());
        } else if (method == RestRequest.Method.POST) {
            return postHandlers.retrieve(path, request.params());
        } else if (method == RestRequest.Method.PUT) {
            return putHandlers.retrieve(path, request.params());
        } else if (method == RestRequest.Method.DELETE) {
            return deleteHandlers.retrieve(path, request.params());
        } else {
            return null;
        }
    }

    private String getPath(RestRequest request) {
        String uri = request.uri();
        int questionMarkIndex = uri.indexOf('?');
        if (questionMarkIndex == -1) {
            return uri;
        }
        return uri.substring(0, questionMarkIndex);
    }
}
