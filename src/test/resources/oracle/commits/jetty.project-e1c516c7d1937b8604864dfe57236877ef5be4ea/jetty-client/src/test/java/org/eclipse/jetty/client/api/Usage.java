//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.client.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.FutureResponseListener;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpVersion;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class Usage
{
    @Test
    public void testGETBlocking_ShortAPI() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        // Block to get the response
        ContentResponse response = client.GET("http://localhost:8080/foo");

        // Verify response status code
        Assert.assertEquals(200, response.getStatus());

        // Access headers
        response.getHeaders().get("Content-Length");
    }

    @Test
    public void testGETBlocking() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        // Address must be provided, it's the only thing non defaultable
        Request request = client.newRequest("localhost", 8080)
                .scheme("https")
                .method(HttpMethod.GET)
                .path("/uri")
                .version(HttpVersion.HTTP_1_1)
                .param("a", "b")
                .header("X-Header", "Y-value")
                .agent("Jetty HTTP Client")
                .idleTimeout(5000, TimeUnit.MILLISECONDS)
                .timeout(20, TimeUnit.SECONDS);

        ContentResponse response = request.send();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testGETAsync() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        final AtomicReference<Response> responseRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        client.newRequest("localhost", 8080)
                // Send asynchronously
                .send(new Response.CompleteListener()
        {
            @Override
            public void onComplete(Result result)
            {
                if (result.isSucceeded())
                {
                    responseRef.set(result.getResponse());
                    latch.countDown();
                }
            }
        });

        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Response response = responseRef.get();
        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testPOSTWithParams_ShortAPI() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        // One liner to POST
        client.POST("http://localhost:8080").param("a", "\u20AC").send();
    }

    @Test
    public void testRequestListener() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        Response response = client.newRequest("localhost", 8080)
                // Add a request listener
                .listener(new Request.Listener.Empty()
                {
                    @Override
                    public void onSuccess(Request request)
                    {
                    }
                }).send();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testRequestWithExplicitConnectionControl() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        // Create an explicit connection, and use try-with-resources to manage it
        try (Connection connection = client.getDestination("http", "localhost", 8080).newConnection().get(5, TimeUnit.SECONDS))
        {
            Request request = client.newRequest("localhost", 8080);

            // Asynchronous send but using FutureResponseListener
            FutureResponseListener listener = new FutureResponseListener(request);
            connection.send(request, listener);
            // Wait for the response on the listener
            Response response = listener.get(5, TimeUnit.SECONDS);

            Assert.assertNotNull(response);
            Assert.assertEquals(200, response.getStatus());
        }
    }

    @Test
    public void testFileUpload() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        // One liner to upload files
        Response response = client.newRequest("localhost", 8080).file(Paths.get("file_to_upload.txt")).send();

        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testCookie() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        // Set a cookie to be sent in requests that match the cookie's domain
        client.getCookieStore().add(URI.create("http://host:8080/path"), new HttpCookie("name", "value"));

        // Send a request for the cookie's domain
        Response response = client.newRequest("host", 8080).send();

        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testBasicAuthentication() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        URI uri = URI.create("http://localhost:8080/secure");

        // Setup Basic authentication credentials for TestRealm
        client.getAuthenticationStore().addAuthentication(new BasicAuthentication(uri, "TestRealm", "username", "password"));

        // One liner to send the request
        ContentResponse response = client.newRequest(uri).timeout(5, TimeUnit.SECONDS).send();

        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testFollowRedirects() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        // Do not follow redirects by default
        client.setFollowRedirects(false);

        ContentResponse response = client.newRequest("localhost", 8080)
                // Follow redirects for this request only
                .followRedirects(true)
                .timeout(5, TimeUnit.SECONDS)
                .send();

        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testResponseInputStream() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        InputStreamResponseListener listener = new InputStreamResponseListener();
        // Send asynchronously with the InputStreamResponseListener
        client.newRequest("localhost", 8080).send(listener);

        // Call to the listener's get() blocks until the headers arrived
        Response response = listener.get(5, TimeUnit.SECONDS);

        // Now check the response information that arrived to decide whether to read the content
        if (response.getStatus() == 200)
        {
            byte[] buffer = new byte[256];
            try (InputStream input = listener.getInputStream())
            {
                while (true)
                {
                    int read = input.read(buffer);
                    if (read < 0)
                        break;
                    // Do something with the bytes just read
                }
            }
        }
        else
        {
            response.abort(new Exception());
        }
    }

    @Test
    public void testRequestInputStream() throws Exception
    {
        HttpClient client = new HttpClient();
        client.start();

        InputStream input = new ByteArrayInputStream("content".getBytes("UTF-8"));

        ContentResponse response = client.newRequest("localhost", 8080)
                // Provide the content as InputStream
                .content(new InputStreamContentProvider(input))
                .send();

        Assert.assertEquals(200, response.getStatus());
    }
}
