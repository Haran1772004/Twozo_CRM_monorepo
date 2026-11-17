package com.twozo.crm.client.utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static List<Path> findFiles(final String folderPath, String pattern) throws IOException {
        final Path rootDir = Path.of(folderPath);
        final List<Path> matchingFiles = new ArrayList<>();
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (matcher.matches(rootDir.relativize(file))) {
                    matchingFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return matchingFiles;
    }
}
