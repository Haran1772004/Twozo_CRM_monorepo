package com.tlc.social.common.model;

/**
 * Timestamps for a message lifecycle (sent, delivered, read).
 *
 * @author Maharaja S
 * @version 1.0
 */
public record MessageTimestamps(Long sentTime, Long deliveredTime, Long readTime) {
}