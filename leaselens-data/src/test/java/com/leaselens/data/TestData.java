package com.leaselens.data;

import java.nio.file.Files;
import java.nio.file.Path;

/** Locates the repository's data folder (set by Maven Surefire; falls back to ../data). */
public final class TestData {

    private TestData() {
    }

    public static Path dir(String child) {
        String configured = System.getProperty("leaselens.dataDir");
        Path base = configured != null ? Path.of(configured) : Path.of("..", "data");
        Path dir = base.resolve(child).normalize();
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("Test data folder not found: " + dir.toAbsolutePath());
        }
        return dir;
    }
}
