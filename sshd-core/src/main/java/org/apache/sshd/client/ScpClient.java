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
package org.apache.sshd.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 */
public interface ScpClient {

    enum Option {
        Recursive,
        PreserveAttributes,
        TargetIsDirectory
    }

    void download(String remote, String local, Option... options) throws IOException;
    void download(String remote, String local, Collection<Option> options) throws IOException;

    void download(String remote, Path local, Option... options) throws IOException;
    void download(String remote, Path local, Collection<Option> options) throws IOException;

    void download(String[] remote, String local, Option... options) throws IOException;
    void download(String[] remote, String local, Collection<Option> options) throws IOException;

    void download(String[] remote, Path local, Option... options) throws IOException;
    void download(String[] remote, Path local, Collection<Option> options) throws IOException;

    void upload(String local, String remote, Option... options) throws IOException;
    void upload(String local, String remote, Collection<Option> options) throws IOException;

    void upload(Path local, String remote, Option... options) throws IOException;
    void upload(Path local, String remote, Collection<Option> options) throws IOException;

    void upload(String[] local, String remote, Option... options) throws IOException;
    void upload(String[] local, String remote, Collection<Option> options) throws IOException;
    
    void upload(Path[] local, String remote, Option... options) throws IOException;
    void upload(Path[] local, String remote, Collection<Option> options) throws IOException;
}
