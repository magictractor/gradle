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
package uk.co.magictractor.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ProjectBuilder {

    private static final File BUILD_DIR = new File("build");

    private Map<String, FileConfig> fileConfigs = new HashMap<>();
    private File testProjectDir;

    private boolean buildDone;

    public ProjectBuilder() {
        initFileConfigs();
    }

    private void initFileConfigs() {
        try {
            initFileConfigs0();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void initFileConfigs0() throws URISyntaxException, IOException {
        URL baseUrl = getClass().getResource("/");
        File baseDir = Paths.get(baseUrl.toURI()).toFile();

        // Eclipse: ~\git\gradle\bin\main
        // gradlew: ~\git\gradle\build\classes\java\test
        File rootDir = baseDir.getParentFile();
        while (!new File(rootDir, "src").exists()) {
            rootDir = rootDir.getParentFile();
            if (rootDir == null) {
                throw new IllegalStateException("Error determining src/example directory");
            }
        }
        File srcDir = new File(rootDir, "src");

        File srcExampleDir = new File(srcDir, "example");
        if (!srcExampleDir.exists()) {
            throw new IllegalStateException("Error determining src/example directory");
        }

        addFileConfigs(new File(srcExampleDir, "resources/example"), null);
        addFileConfigs(new File(srcExampleDir, "java"), "src/main/java");
        addFileConfigs(new File(srcExampleDir, "test"), "src/test/java");
    }

    @Deprecated // use Path
    private void addFileConfigs(File sourceDir, String relativeTargetPath) throws IOException {
        addFileConfigs(sourceDir.toPath(), relativeTargetPath);
    }

    private void addFileConfigs(Path sourceDir, String relativeTargetPath) throws IOException {
        // if (!sourceDir.exists()) {
        if (!Files.exists(sourceDir)) {
            throw new IllegalArgumentException();
        }

        addFileConfigs0(sourceDir, relativeTargetPath);
    }

    private void addFileConfigs0(Path sourceDir, String relativeTargetPath) throws IOException {
        // TODO! handle stream more efficiently
        for (Path sourceFile : Files.list(sourceDir).toList()) {
            //  if (sourceFile.isDirectory()) {
            if (Files.isDirectory(sourceFile)) {
                // TODO! handle name more efficiently (done elsewhere too)
                addFileConfigs0(sourceFile, relativeTargetPath + "/" + sourceFile.getFileName().toString());
            }
            else {
                addFileConfig0(sourceFile, relativeTargetPath);
            }
        }
    }

    private void addFileConfig0(Path path, String relativePath) {
        String name = path.getFileName().toString();
        if (fileConfigs.containsKey(name)) {
            throw new IllegalStateException("Duplicate name " + name);
        }
        if (name.endsWith(".class")) {
            throw new IllegalStateException("Expected .java files, but found a .class file " + path);
        }

        FileConfig fileConfig = new FileConfig(path, relativePath);
        fileConfigs.put(name, fileConfig);
    }

    public ProjectBuilder withTestProjectDir(File testProjectDir) {
        checkBuildNotDone();
        if (testProjectDir != null) {
            throw new IllegalStateException();
        }

        this.testProjectDir = testProjectDir;

        return this;
    }

    private File getTestProjectDir() {
        if (testProjectDir == null) {
            testProjectDir = createDefaultTestProjectDir();
        }
        return testProjectDir;
    }

    private File createDefaultTestProjectDir() {
        File defaultTestProjectDir = new File(BUILD_DIR, "example");
        if (!defaultTestProjectDir.exists()) {
            defaultTestProjectDir.mkdirs();
        }
        else {
            // TODO! clear the directory
        }

        return defaultTestProjectDir;
    }

    private void init() {
        try {
            init0();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void init0() throws URISyntaxException, IOException {
        for (FileConfig fileConfig : fileConfigs.values()) {
            createFile0(fileConfig);
        }
    }

    private void createFile0(FileConfig fileConfig) throws IOException {
        Path target = getTestProjectDir().toPath();
        if (fileConfig.relativePath != null) {
            // target = new File(target, fileConfig.relativePath);
            target = target.resolve(fileConfig.relativePath);
        }
        target = target.resolve(fileConfig.source.getFileName());

        copyFile0(fileConfig.source, target);
    }

    private void copyFile0(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void checkBuildNotDone() {
        if (buildDone) {
            throw new IllegalStateException("build() has already been called");
        }
    }

    public File build() {
        if (!buildDone) {
            init();
            buildDone = true;
        }
        return testProjectDir;
    }

    private static final class FileConfig {
        private Path source;
        // null for top-level, "src/main/java" or "src/test/java"
        private String relativePath;

        /* default */ FileConfig(Path source, String relativePath) {
            this.source = source;
            this.relativePath = relativePath;
        }

        @Override
        public String toString() {
            return source + " -> " + relativePath;
        }
    }

}
