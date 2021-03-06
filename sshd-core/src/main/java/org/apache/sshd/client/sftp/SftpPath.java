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
package org.apache.sshd.client.sftp;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;

import org.apache.sshd.common.file.util.BasePath;
import org.apache.sshd.common.file.util.ImmutableList;

public class SftpPath extends BasePath<SftpPath, SftpFileSystem> {

    public SftpPath(SftpFileSystem fileSystem, String root, ImmutableList<String> names) {
        super(fileSystem, root, names);
    }

    public URI toUri() {
        // TODO
        return null;
    }

    public SftpPath toRealPath(LinkOption... options) throws IOException {
//        try (SftpClient client = fileSystem.getClient()) {
//            client.realP
//        }
        // TODO: handle links
        SftpPath absolute = toAbsolutePath();
        fileSystem.provider().checkAccess(absolute);
        return absolute;
    }

}
