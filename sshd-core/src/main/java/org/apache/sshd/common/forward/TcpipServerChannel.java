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
package org.apache.sshd.common.forward;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;

import org.apache.sshd.client.future.DefaultOpenFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.channel.ChannelOutputStream;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoConnectFuture;
import org.apache.sshd.common.io.IoConnector;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.util.Readable;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.server.channel.AbstractServerChannel;
import org.apache.sshd.server.channel.OpenChannelException;

/**
 * TODO Add javadoc
 *
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public class TcpipServerChannel extends AbstractServerChannel {
    public abstract static class TcpipFactory implements NamedFactory<Channel> {
        private final Type type;

        protected TcpipFactory(Type type) {
            this.type = type;
        }
        
        public final Type getType() {
            return type;
        }
        
        @Override
        public Channel create() {
            return new TcpipServerChannel(getType());
        }
    }

    public static class DirectTcpipFactory extends TcpipFactory {
        public static final DirectTcpipFactory  INSTANCE = new DirectTcpipFactory();

        public DirectTcpipFactory() {
            super(Type.Direct);
        }

        @Override
        public String getName() {
            return "direct-tcpip";
        }
    }

    public static class ForwardedTcpipFactory extends TcpipFactory {
        public static final ForwardedTcpipFactory INSTANCE = new ForwardedTcpipFactory();
        
        public ForwardedTcpipFactory() {
            super(Type.Forwarded);
        }

        @Override
        public String getName() {
            return "forwarded-tcpip";
        }
    }

    private enum Type {
        Direct,
        Forwarded
    }

    private final Type type;
    private IoConnector connector;
    private IoSession ioSession;
    private OutputStream out;

    public TcpipServerChannel(Type type) {
        this.type = type;
    }

    @Override
    protected OpenFuture doInit(Buffer buffer) {
        final OpenFuture f = new DefaultOpenFuture(this);

        String hostToConnect = buffer.getString();
        int portToConnect = buffer.getInt();
        String originatorIpAddress = buffer.getString();
        int originatorPort = buffer.getInt();
        log.info("Receiving request for direct tcpip: hostToConnect={}, portToConnect={}, originatorIpAddress={}, originatorPort={}",
                new Object[] { hostToConnect, Integer.valueOf(portToConnect), originatorIpAddress, Integer.valueOf(originatorPort) });


        SshdSocketAddress address = null;
        switch (type) {
            case Direct:    address = new SshdSocketAddress(hostToConnect, portToConnect); break;
            case Forwarded: address = service.getTcpipForwarder().getForwardedPort(portToConnect); break;
        }
        final ForwardingFilter filter = getSession().getFactoryManager().getTcpipForwardingFilter();
        if (address == null || filter == null || !filter.canConnect(address, getSession())) {
            super.close(true);
            f.setException(new OpenChannelException(SshConstants.SSH_OPEN_ADMINISTRATIVELY_PROHIBITED, "Connection denied"));
            return f;
        }

        // TODO: revisit for better threading. Use async io ?
        out = new ChannelOutputStream(this, remoteWindow, log, SshConstants.SSH_MSG_CHANNEL_DATA);
        IoHandler handler = new IoHandler() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void messageReceived(IoSession session, Readable message) throws Exception {
                if (isClosing()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring write to channel {} in CLOSING state", Integer.valueOf(id));
                    }
                } else {
                    Buffer buffer = new ByteArrayBuffer();
                    buffer.putBuffer(message);
                    out.write(buffer.array(), buffer.rpos(), buffer.available());
                    out.flush();
                }
            }
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                // ignored
            }
            @Override
            public void sessionClosed(IoSession session) throws Exception {
                close(false);
            }
            @Override
            public void exceptionCaught(IoSession ioSession, Throwable cause) throws Exception {
                close(true);
            }
        };
        connector = getSession().getFactoryManager().getIoServiceFactory()
                .createConnector(handler);
        IoConnectFuture future = connector.connect(address.toInetSocketAddress());
        future.addListener(new SshFutureListener<IoConnectFuture>() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void operationComplete(IoConnectFuture future) {
                if (future.isConnected()) {
                    ioSession = future.getSession();
                    f.setOpened();
                } else if (future.getException() != null) {
                    closeImmediately0();
                    if (future.getException() instanceof ConnectException) {
                        f.setException(new OpenChannelException(
                            SshConstants.SSH_OPEN_CONNECT_FAILED,
                            future.getException().getMessage(),
                            future.getException()));
                    } else {
                        f.setException(future.getException());
                    }
                }
            }
        });
        return f;
    }

    private void closeImmediately0() {
        // We need to close the channel immediately to remove it from the
        // server session's channel table and *not* send a packet to the
        // client.  A notification was already sent by our caller, or will
        // be sent after we return.
        //
        super.close(true);

        // We also need to dispose of the connector, but unfortunately we
        // are being invoked by the connector thread or the connector's
        // own processor thread.  Disposing of the connector within either
        // causes deadlock.  Instead create a new thread to dispose of the
        // connector in the background.
        //
        new Thread("TcpIpServerChannel-ConnectorCleanup") {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                connector.close(true);
            }
        }.start();
    }

    @Override
    public CloseFuture close(boolean immediately) {
        return super.close(immediately).addListener(new SshFutureListener<CloseFuture>() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void operationComplete(CloseFuture sshFuture) {
                closeImmediately0();
            }
        });
    }

    @Override
    protected void doWriteData(byte[] data, int off, final int len) throws IOException {
        // Make sure we copy the data as the incoming buffer may be reused
        Buffer buf = new ByteArrayBuffer(data, off, len);
        buf = new ByteArrayBuffer(buf.getCompactData());
        ioSession.write(buf).addListener(new SshFutureListener<IoWriteFuture>() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void operationComplete(IoWriteFuture future) {
                try {
                    localWindow.consumeAndCheck(len);
                } catch (IOException e) {
                    session.exceptionCaught(e);
                }
            }
        });
    }

    @Override
    protected void doWriteExtendedData(byte[] data, int off, int len) throws IOException {
        throw new UnsupportedOperationException(type + "Tcpip channel does not support extended data");
    }

}
