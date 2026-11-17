package com.tlc.components.starter.scanner;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Selvakumar
 * @version 1.0
 */
@Component
public class ResourceScanner {

    /**
     * Scans resources matching the specified location pattern and file pattern.
     *
     * @param locationPattern The pattern to match locations.
     * @param filePattern     The pattern to match file names within the locations.
     * @return A list of resources found matching the patterns.
     * @throws IOException If an I/O error occurs while scanning resources.
     */
    public List<Resource> scanResources(final String locationPattern, final String filePattern) throws IOException {
        final List<Resource> resources = new ArrayList<>();

        Collections.addAll(resources, getResources(locationPattern, filePattern));
        return resources;
    }

    /**
     * Scans resources matching the specified location pattern and file pattern.
     *
     * @param locationPattern The pattern to match locations.
     * @param filePattern     The pattern to match file names within the locations.
     * @return A list of resources found matching the patterns.
     * @throws IOException If an I/O error occurs while scanning resources.
     */
    private Resource[] getResources(final String locationPattern, final String filePattern) throws IOException {
        final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());

        return resolver.getResources(String.format("%s%s%s", "classpath*:", locationPattern, filePattern));
    }

    /**
     * Scans resource matching the specified file pattern.
     *
     * @param filePattern The pattern to match file names within the locations.
     * @return A list of resources found matching the patterns.
     * @throws IOException If an I/O error occurs while scanning resources.
     */
    public Resource getResource(final String filePattern) throws IOException {
        final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());

        return resolver.getResource("classpath:" + filePattern);
    }
}