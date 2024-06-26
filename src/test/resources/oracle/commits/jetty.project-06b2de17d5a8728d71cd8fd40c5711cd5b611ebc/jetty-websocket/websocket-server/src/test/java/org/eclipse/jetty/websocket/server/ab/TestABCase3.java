//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
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

package org.eclipse.jetty.websocket.server.ab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.protocol.CloseInfo;
import org.eclipse.jetty.websocket.protocol.WebSocketFrame;
import org.junit.Test;

public class TestABCase3 extends AbstractABCase
{
    /**
     * Send small text frame, with RSV1 == true, with no extensions defined.
     */
    @Test
    public void testCase3_1() throws Exception
    {
        WebSocketFrame send = WebSocketFrame.text("small").setRsv1(true); // intentionally bad

        WebSocketFrame expect = new CloseInfo(StatusCode.PROTOCOL).asFrame();

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Send small text frame, send again with RSV2 == true, then ping, with no extensions defined.
     */
    @Test
    public void testCase3_2() throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text("small"));
        send.add(WebSocketFrame.text("small").setRsv2(true)); // intentionally bad
        send.add(WebSocketFrame.ping().setPayload("ping"));

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text("small")); // echo on good frame
        expect.add(new CloseInfo(StatusCode.PROTOCOL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Send small text frame, send again with (RSV1 & RSV2), then ping, with no extensions defined.
     */
    @Test
    public void testCase3_3() throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text("small"));
        send.add(WebSocketFrame.text("small").setRsv1(true).setRsv2(true)); // intentionally bad
        send.add(WebSocketFrame.ping().setPayload("ping"));

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text("small")); // echo on good frame
        expect.add(new CloseInfo(StatusCode.PROTOCOL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.PER_FRAME);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Send small text frame, send again with (RSV3), then ping, with no extensions defined.
     */
    @Test
    public void testCase3_4() throws Exception
    {
        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text("small"));
        send.add(WebSocketFrame.text("small").setRsv3(true)); // intentionally bad
        send.add(WebSocketFrame.ping().setPayload("ping"));

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text("small")); // echo on good frame
        expect.add(new CloseInfo(StatusCode.PROTOCOL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.SLOW);
            fuzzer.setSlowSendSegmentSize(1);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Send binary frame with (RSV3 & RSV1), with no extensions defined.
     */
    @Test
    public void testCase3_5() throws Exception
    {
        byte payload[] = new byte[8];
        Arrays.fill(payload,(byte)0xFF);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.binary(payload).setRsv3(true).setRsv1(true)); // intentionally bad

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.PROTOCOL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Send ping frame with (RSV3 & RSV2), with no extensions defined.
     */
    @Test
    public void testCase3_6() throws Exception
    {
        byte payload[] = new byte[8];
        Arrays.fill(payload,(byte)0xFF);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.ping().setPayload(payload).setRsv3(true).setRsv2(true)); // intentionally bad

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.PROTOCOL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Send close frame with (RSV3 & RSV2 & RSV1), with no extensions defined.
     */
    @Test
    public void testCase3_7() throws Exception
    {
        byte payload[] = new byte[8];
        Arrays.fill(payload,(byte)0xFF);

        List<WebSocketFrame> send = new ArrayList<>();
        WebSocketFrame frame = new CloseInfo(StatusCode.NORMAL).asFrame();
        frame.setRsv1(true);
        frame.setRsv2(true);
        frame.setRsv3(true);
        send.add(frame); // intentionally bad

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new CloseInfo(StatusCode.PROTOCOL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect);
        }
        finally
        {
            fuzzer.close();
        }
    }
}
