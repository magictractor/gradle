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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

public class Repository {

    public static final Repository GRADLE_PLUGIN_PORTAL = new Repository("https://plugins.gradle.org/m2", true);
    public static final Repository REPSY = new Repository("https://repo.repsy.io/magictractor/maven", false,
        () -> gradleProperty("repsyUsername"), () -> gradleProperty("repsyPassword"));

    private static final Supplier<String> noAuth() {
        //throw new UnsupportedOperationException("Missing authorisation configuration");
        return () -> {
            throw new UnsupportedOperationException("Missing authorisation configuration");
        };
    };

    private static final String gradleProperty(String propertyName) {
        String userHome = System.getProperty("user.home");
        Path propertiesPath = Path.of(userHome, ".gradle", "gradle.properties");
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            properties.load(in);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String value = properties.getProperty(propertyName);
        if (value == null) {
            throw new IllegalArgumentException();
        }

        return value;
    };

    private final String baseUrl;
    private final boolean isPublic;
    private final Supplier<String> usernameSupplier;
    private final Supplier<String> passwordSupplier;

    private String username;
    private String password;

    private Repository(String baseUrl, boolean isPublic) {
        this(baseUrl, isPublic, noAuth(), noAuth());
    }

    private Repository(String baseUrl, boolean isPublic, Supplier<String> usernameSupplier, Supplier<String> passwordSupplier) {
        this.baseUrl = baseUrl;
        this.isPublic = isPublic;
        this.usernameSupplier = usernameSupplier;
        this.passwordSupplier = passwordSupplier;
    }

    private String getUsername() {
        if (username == null) {
            username = usernameSupplier.get();
        }
        return username;
    }

    private String getPassword() {
        if (password == null) {
            password = passwordSupplier.get();
        }
        return password;
    }

    /* default */ HttpURLConnection httpGet(String artifactPath, String fileName) {
        try {
            return httpGet0(artifactPath, fileName);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private HttpURLConnection httpGet0(String artifactPath, String fileName) throws IOException {
        String url = baseUrl + '/' + artifactPath + "/" + fileName;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        if (!isPublic) {
            auth(connection);
        }

        sendRequest(connection);

        return connection;
    }

    public HttpURLConnection httpPut(String artifactPath, String fileName, byte[] body) {
        try {
            return httpPut0(artifactPath, fileName, body);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private HttpURLConnection httpPut0(String artifactPath, String fileName, byte[] body) throws IOException {
        String url = baseUrl + '/' + artifactPath + "/" + fileName;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("PUT");
        auth(connection);
        if (body != null) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(body);
        }

        sendRequest(connection);

        return connection;
    }

    private void sendRequest(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();

        switch (status) {
            case 200:
                // Expected. Happy path. Do nothing.
                break;
            case 401:
                // TODO! message could be more helpful, will vary for public/private repo and GET/not-GET.
                throw new IllegalStateException("Authorisation required");
            default:
                throw new IllegalStateException("Unexpected response: " + status + " " + connection.getResponseMessage());
        }
    }

    private void auth(HttpURLConnection connection) {
        String basic = getUsername() + ":" + getPassword();
        byte[] base64Bytes = Base64.getEncoder().encode(basic.getBytes(StandardCharsets.UTF_8));
        String base64 = new String(base64Bytes, StandardCharsets.UTF_8);
        connection.setRequestProperty("Authorization", "Basic " + base64);
    }

    public List<RepositoryFile> list(String artifact) {
        try {
            return list0(artifact);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // TODO! better to read .module file?
    private List<RepositoryFile> list0(String artifact) throws IOException {
        List<RepositoryFile> list = new ArrayList<>();

        String artifactPath = artifactPath(artifact);
        HttpURLConnection httpConnection = httpGet0(artifactPath, "");
        byte[] bytes = httpConnection.getInputStream().readAllBytes();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));

        String line = reader.readLine();
        do {
            int index = line.indexOf("<a href=\"");
            if (index >= 0) {
                int endIndex = line.indexOf("\"", index + 9);
                String href = line.substring(index + 9, endIndex);
                if (!"../".equals(href)) {
                    RepositoryFile file = new RepositoryFile(this, artifactPath, href);
                    list.add(file);
                }
            }
            line = reader.readLine();
        } while (line != null);

        httpConnection.disconnect();

        return list;
    }

    private String artifactPath(String artifact) {
        StringBuilder sb = new StringBuilder();

        int colon1Index = artifact.indexOf(":");
        int colon2Index = artifact.indexOf(":", colon1Index + 1);

        sb.append(artifact.substring(0, colon1Index).replace('.', '/').replace(':', '/'));
        sb.append('/');
        sb.append(artifact.substring(colon1Index + 1, colon2Index));
        sb.append('/');
        sb.append(artifact.substring(colon2Index + 1));

        return sb.toString();
    }

    public void write(RepositoryFile file) {
        httpPut(file.getArtifactPath(), file.getName(), file.getBytes());
    }

    public static void main(String[] args) throws IOException {
        // Repository.GRADLE_PLUGIN_PORTAL.list("com.netflix.nebula:nebula-release-plugin:21.0.0").forEach(file -> {
        Repository.REPSY.list("uk.co.magictractor:magictractor-gradle:0.0.5").forEach(file -> {
            //file.getBytes();
            System.out.println(file);
        });
    }

}
