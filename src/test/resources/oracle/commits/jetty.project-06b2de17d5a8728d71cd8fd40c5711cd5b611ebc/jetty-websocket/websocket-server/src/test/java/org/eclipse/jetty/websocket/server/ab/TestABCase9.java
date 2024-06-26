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
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.toolchain.test.AdvancedRunner;
import org.eclipse.jetty.toolchain.test.annotation.Stress;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.protocol.CloseInfo;
import org.eclipse.jetty.websocket.protocol.OpCode;
import org.eclipse.jetty.websocket.protocol.WebSocketFrame;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Big frame/message tests
 */
@RunWith(AdvancedRunner.class)
public class TestABCase9 extends AbstractABCase
{
    private static final int KBYTE = 1024;
    private static final int MBYTE = KBYTE * KBYTE;

    private void assertMultiFrameEcho(byte opcode, int overallMsgSize, int fragmentSize) throws Exception
    {
        byte msg[] = new byte[overallMsgSize];
        Arrays.fill(msg,(byte)'M');

        List<WebSocketFrame> send = new ArrayList<>();
        byte frag[];
        int remaining = msg.length;
        int offset = 0;
        boolean fin;
        byte op = opcode;
        while (remaining > 0)
        {
            int len = Math.min(remaining,fragmentSize);
            frag = new byte[len];
            System.arraycopy(msg,offset,frag,0,len);
            remaining -= len;
            fin = (remaining <= 0);
            send.add(new WebSocketFrame(op).setPayload(frag).setFin(fin));
            offset += len;
            op = OpCode.CONTINUATION;
        }
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new WebSocketFrame(opcode).setPayload(msg));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,8);
        }
        finally
        {
            fuzzer.close();
        }
    }

    private void assertSlowFrameEcho(byte opcode, int overallMsgSize, int segmentSize) throws Exception
    {
        byte msg[] = new byte[overallMsgSize];
        Arrays.fill(msg,(byte)'M');

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(new WebSocketFrame(opcode).setPayload(msg));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(new WebSocketFrame(opcode).setPayload(msg));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.SLOW);
            fuzzer.setSlowSendSegmentSize(segmentSize);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,8);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 64KB text message (1 frame)
     */
    @Test
    public void testCase9_1_1() throws Exception
    {
        byte utf[] = new byte[64 * KBYTE];
        Arrays.fill(utf,(byte)'y');
        String msg = StringUtil.toUTF8String(utf,0,utf.length);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text(msg));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text(msg));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

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
     * Echo 256KB text message (1 frame)
     */
    @Test
    public void testCase9_1_2() throws Exception
    {
        byte utf[] = new byte[256 * KBYTE];
        Arrays.fill(utf,(byte)'y');

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text().setPayload(utf));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text().setPayload(utf));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,2);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 1MB text message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_1_3() throws Exception
    {
        byte utf[] = new byte[1 * MBYTE];
        Arrays.fill(utf,(byte)'y');

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text().setPayload(utf));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text().setPayload(utf));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,4);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 4MB text message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_1_4() throws Exception
    {
        byte utf[] = new byte[4 * MBYTE];
        Arrays.fill(utf,(byte)'y');

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text().setPayload(utf));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text().setPayload(utf));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,8);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 8MB text message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_1_5() throws Exception
    {
        byte utf[] = new byte[8 * MBYTE];
        Arrays.fill(utf,(byte)'y');

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text().setPayload(utf));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text().setPayload(utf));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,16);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 16MB text message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_1_6() throws Exception
    {
        byte utf[] = new byte[16 * MBYTE];
        Arrays.fill(utf,(byte)'y');

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.text().setPayload(utf));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.text().setPayload(utf));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,32);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 64KB binary message (1 frame)
     */
    @Test
    public void testCase9_2_1() throws Exception
    {
        byte data[] = new byte[64 * KBYTE];
        Arrays.fill(data,(byte)0x21);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.binary(data));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.binary(data));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

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
     * Echo 256KB binary message (1 frame)
     */
    @Test
    public void testCase9_2_2() throws Exception
    {
        byte data[] = new byte[256 * KBYTE];
        Arrays.fill(data,(byte)0x22);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.binary().setPayload(data));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.binary().setPayload(data));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,2);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 1MB binary message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_2_3() throws Exception
    {
        byte data[] = new byte[1 * MBYTE];
        Arrays.fill(data,(byte)0x23);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.binary().setPayload(data));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.binary().setPayload(data));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,4);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 4MB binary message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_2_4() throws Exception
    {
        byte data[] = new byte[4 * MBYTE];
        Arrays.fill(data,(byte)0x24);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.binary().setPayload(data));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.binary().setPayload(data));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,8);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 8MB binary message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_2_5() throws Exception
    {
        byte data[] = new byte[8 * MBYTE];
        Arrays.fill(data,(byte)0x25);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.binary().setPayload(data));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.binary().setPayload(data));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,16);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Echo 16MB binary message (1 frame)
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_2_6() throws Exception
    {
        byte data[] = new byte[16 * MBYTE];
        Arrays.fill(data,(byte)0x26);

        List<WebSocketFrame> send = new ArrayList<>();
        send.add(WebSocketFrame.binary().setPayload(data));
        send.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        List<WebSocketFrame> expect = new ArrayList<>();
        expect.add(WebSocketFrame.binary().setPayload(data));
        expect.add(new CloseInfo(StatusCode.NORMAL).asFrame());

        Fuzzer fuzzer = new Fuzzer(this);
        try
        {
            fuzzer.connect();
            fuzzer.setSendMode(Fuzzer.SendMode.BULK);
            fuzzer.send(send);
            fuzzer.expect(expect,TimeUnit.SECONDS,32);
        }
        finally
        {
            fuzzer.close();
        }
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_1() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,64);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_2() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,256);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_3() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,1 * KBYTE);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_4() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,4 * KBYTE);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_5() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,16 * KBYTE);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_6() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,64 * KBYTE);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_7() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,256 * KBYTE);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_8() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,1 * MBYTE);
    }

    /**
     * Send 4MB text message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_3_9() throws Exception
    {
        assertMultiFrameEcho(OpCode.TEXT,4 * MBYTE,4 * MBYTE);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_1() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,64);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_2() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,256);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_3() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,1 * KBYTE);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_4() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,4 * KBYTE);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_5() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,16 * KBYTE);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_6() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,64 * KBYTE);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_7() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,256 * KBYTE);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_8() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,1 * MBYTE);
    }

    /**
     * Send 4MB binary message in multiple frames.
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_4_9() throws Exception
    {
        assertMultiFrameEcho(OpCode.BINARY,4 * MBYTE,4 * MBYTE);
    }

    /**
     * Send 1MB text message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_5_1() throws Exception
    {
        assertSlowFrameEcho(OpCode.TEXT,1 * MBYTE,64);
    }

    /**
     * Send 1MB text message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_5_2() throws Exception
    {
        assertSlowFrameEcho(OpCode.TEXT,1 * MBYTE,128);
    }

    /**
     * Send 1MB text message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_5_3() throws Exception
    {
        assertSlowFrameEcho(OpCode.TEXT,1 * MBYTE,256);
    }

    /**
     * Send 1MB text message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_5_4() throws Exception
    {
        assertSlowFrameEcho(OpCode.TEXT,1 * MBYTE,512);
    }

    /**
     * Send 1MB text message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_5_5() throws Exception
    {
        assertSlowFrameEcho(OpCode.TEXT,1 * MBYTE,1024);
    }

    /**
     * Send 1MB text message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_5_6() throws Exception
    {
        assertSlowFrameEcho(OpCode.TEXT,1 * MBYTE,2048);
    }

    /**
     * Send 1MB binary message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_6_1() throws Exception
    {
        assertSlowFrameEcho(OpCode.BINARY,1 * MBYTE,64);
    }

    /**
     * Send 1MB binary message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_6_2() throws Exception
    {
        assertSlowFrameEcho(OpCode.BINARY,1 * MBYTE,128);
    }

    /**
     * Send 1MB binary message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_6_3() throws Exception
    {
        assertSlowFrameEcho(OpCode.BINARY,1 * MBYTE,256);
    }

    /**
     * Send 1MB binary message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_6_4() throws Exception
    {
        assertSlowFrameEcho(OpCode.BINARY,1 * MBYTE,512);
    }

    /**
     * Send 1MB binary message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_6_5() throws Exception
    {
        assertSlowFrameEcho(OpCode.BINARY,1 * MBYTE,1024);
    }

    /**
     * Send 1MB binary message in 1 frame, but slowly
     */
    @Test
    @Stress("High I/O use")
    public void testCase9_6_6() throws Exception
    {
        assertSlowFrameEcho(OpCode.BINARY,1 * MBYTE,2048);
    }
}
