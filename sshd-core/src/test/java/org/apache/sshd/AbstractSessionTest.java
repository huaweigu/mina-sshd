/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sshd;

import java.io.IOException;

import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.util.BaseTestSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * Test basic stuff on AbstractSession.
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class AbstractSessionTest extends BaseTestSupport {

    private MySession session;

    @Before
    public void setUp() throws Exception {
        session = new MySession();
    }

    @Test
    public void testReadIdentSimple() {
        Buffer buf = new ByteArrayBuffer("SSH-2.0-software\r\n".getBytes());
        String ident = session.doReadIdentification(buf);
        assertEquals("SSH-2.0-software", ident);
    }

    @Test
    public void testReadIdentWithoutCR() {
        Buffer buf = new ByteArrayBuffer("SSH-2.0-software\n".getBytes());
        String ident = session.doReadIdentification(buf);
        assertEquals("SSH-2.0-software", ident);
    }

    @Test
    public void testReadIdentWithHeaders() {
        Buffer buf = new ByteArrayBuffer(("a header line\r\nSSH-2.0-software\r\n").getBytes());
        String ident = session.doReadIdentification(buf);
        assertEquals("SSH-2.0-software", ident);
    }

    @Test
    public void testReadIdentWithSplitPackets() {
        Buffer buf = new ByteArrayBuffer("header line\r\nSSH".getBytes());
        String ident = session.doReadIdentification(buf);
        assertNull(ident);
        buf.putRawBytes("-2.0-software\r\n".getBytes());
        ident = session.doReadIdentification(buf);
        assertEquals("SSH-2.0-software", ident);
    }

    @Test(expected = IllegalStateException.class)
    public void testReadIdentBadLineEnding() {
        Buffer buf = new ByteArrayBuffer(("SSH-2.0-software\ra").getBytes());
        String ident = session.doReadIdentification(buf);
        fail("Unexpected success: " + ident);
    }

    @Test(expected = IllegalStateException.class)
    public void testReadIdentLongLine() {
        Buffer buf = new ByteArrayBuffer(("SSH-2.0-software" +
                "01234567890123456789012345678901234567890123456789" +
                "01234567890123456789012345678901234567890123456789" +
                "01234567890123456789012345678901234567890123456789" +
                "01234567890123456789012345678901234567890123456789" +
                "01234567890123456789012345678901234567890123456789" +
                "01234567890123456789012345678901234567890123456789").getBytes());
        String ident = session.doReadIdentification(buf);
        fail("Unexpected success: " + ident);
    }

    @Test(expected = IllegalStateException.class)
    public void testReadIdentLongHeader() {
        StringBuilder sb = new StringBuilder(32768);
        for (int i = 0; i < 500; i++) {
            sb.append("01234567890123456789012345678901234567890123456789\r\n");
        }
        sb.append("SSH-2.0-software\r\n");
        Buffer buf = new ByteArrayBuffer(sb.toString().getBytes());
        String ident = session.doReadIdentification(buf);
        fail("Unexpected success: " + ident);
    }

    public static class MySession extends AbstractSession {
        public MySession() {
            super(true, SshServer.setUpDefaultServer(), null);
        }
        @Override
        protected void handleMessage(Buffer buffer) throws Exception {
            // ignored
        }
        @Override
        protected boolean readIdentification(Buffer buffer) {
            return false;
        }
        public String doReadIdentification(Buffer buffer) {
            return super.doReadIdentification(buffer, false);
        }
        @Override
        protected void sendKexInit() throws IOException {
            // ignored
        }
        @Override
        protected void checkKeys() {
            // ignored
        }
        @Override
        protected void receiveKexInit(Buffer buffer) throws IOException {
            // ignored
        }
        @Override
        public void startService(String name) throws Exception {
            // ignored
        }
        @Override
        public void resetIdleTimeout() {
            // ignored
        }
    }
}
