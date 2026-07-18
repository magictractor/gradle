/**
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.magictractor.gradle.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;

public class RepositoryFile {

    private final Repository sourceRepository;
    private final String artifactPath;
    private final String name;

    private transient byte[] bytes;

    /* default */ RepositoryFile(Repository sourceRepository, String artifactPath, String name) {
        this.sourceRepository = sourceRepository;
        this.artifactPath = artifactPath;
        this.name = name;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        if (bytes == null) {
            HttpURLConnection httpConnection = sourceRepository.httpGet(artifactPath, name);
            try {
                bytes = httpConnection.getInputStream().readAllBytes();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return bytes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{name=");
        sb.append(name);

        if (bytes != null) {
            sb.append(", bytes.length=");
            sb.append(bytes.length);
        }

        sb.append("}");

        return sb.toString();
    }

}
