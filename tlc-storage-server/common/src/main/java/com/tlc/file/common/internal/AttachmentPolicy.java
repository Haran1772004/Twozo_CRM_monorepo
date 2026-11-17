package com.tlc.file.common.internal;

import java.util.Map;

/**
 * Represents a policy configuration for validating file attachments.
 * <p>
 * This record encapsulates metadata such as:
 * <ul>
 *     <li>The attachment category name and its corresponding ID</li>
 *     <li>The maximum allowed file size (in bytes)</li>
 *     <li>A list of allowed file extensions for that category</li>
 * </ul>
 *
 * @param category                  The name of the attachment category.
 * @param categoryId                The numeric ID representing the category.
 * @param defaultMaxSize            The default maximum allowed file size in bytes.
 * @param allowedExtensionsWithSize A collection of permitted file extensions with size for attachments in this category.
 */
public record AttachmentPolicy(String category, short categoryId, int defaultMaxSize, Map<String, Integer> allowedExtensionsWithSize) {
}
