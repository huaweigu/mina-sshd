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
package org.apache.sshd.common.session;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.SessionListener;
import org.apache.sshd.common.util.AbstractLoggingBean;

/**
 * Task that iterates over all currently open {@link AbstractSession}s and checks each of them for timeouts. If
 * the {@link AbstractSession} has timed out (either auth or idle timeout), the session will be disconnected.
 *
 * @see org.apache.sshd.common.session.AbstractSession#checkForTimeouts()
 */
public class SessionTimeoutListener extends AbstractLoggingBean implements SessionListener, Runnable {
    private final Set<AbstractSession> sessions = new CopyOnWriteArraySet<AbstractSession>();

    @Override
    public void sessionCreated(Session session) {
        if (session instanceof AbstractSession && (session.getAuthTimeout() > 0 || session.getIdleTimeout() > 0)) {
            sessions.add((AbstractSession) session);
        }
    }

    @Override
    public void sessionEvent(Session session, Event event) {
        // ignored
    }

    @Override
    public void sessionClosed(Session s) {
        sessions.remove(s);
    }

    @Override
    public void run() {
        for (AbstractSession session : sessions) {
            try {
                session.checkForTimeouts();
            } catch(Exception e) {
                log.warn("An error occurred while checking session=" + session + " timeouts", e);
            }
        }
    }
}
