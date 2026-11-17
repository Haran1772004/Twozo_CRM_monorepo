package com.tlc.web;

import java.util.Objects;

/**
 * Represents the information of a cookie
 *
 * @author Selvakumar
 * @version 1.0
 */
public class HttpCookie {

    private final String name;
    private final String value;
    private final boolean isSecured;
    private final boolean httpOnly;
    private final String path;
    private final String domain;
    private final SameSite sameSite;
    private final long maxAgeInSeconds;
    private final int version;
    private final String comment;

    public HttpCookie(final String name, final String value, final boolean isSecured, final boolean httpOnly,
                      final String path, final String domain, final SameSite sameSite,
                      final long maxAgeInSeconds, final int version, final String comment) {
        Objects.requireNonNull(name, "Cookie name cannot be null");
        Objects.requireNonNull(name, "Cookie value cannot be null");

        this.name = name;
        this.value = value;
        this.isSecured = isSecured;
        this.httpOnly = httpOnly;
        this.path = path;
        this.domain = domain;
        this.sameSite = sameSite;
        this.maxAgeInSeconds = maxAgeInSeconds;
        this.version = version;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public String getPath() {
        return path;
    }

    public String getDomain() {
        return domain;
    }

    public SameSite getSameSite() {
        return sameSite;
    }

    public long getMaxAgeInSeconds() {
        return maxAgeInSeconds;
    }

    public String getComment() {
        return comment;
    }

    public int getVersion() {
        return this.version;
    }

    public static class Builder {
        private static final long DEFAULT_EXPIRY_TIME = -1;
        private static final int DEFAULT_VERSION = 1;
        private final String name;
        private final String value;
        private boolean isSecured = false;
        private boolean httpOnly = false;
        private String path;
        private String domain;
        private String comment;
        private SameSite sameSite;
        private long maxAgeInSeconds = DEFAULT_EXPIRY_TIME;
        private int version = DEFAULT_VERSION;

        /**
         * A builder that builds the {@link HttpCookie}
         *
         * @param name  Cookie name - Required parameter
         * @param value Cookie value - Required parameter
         */
        public Builder(final String name, final String value) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
            this.name = name;
            this.value = value;
        }

        public Builder isSecured(final boolean isSecured) {
            this.isSecured = isSecured;
            return this;
        }

        public Builder httpOnly(final boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        public Builder path(final String path) {
            this.path = path;
            return this;
        }

        public Builder domain(final String domain) {
            this.domain = domain;
            return this;
        }

        public Builder sameSite(final SameSite sameSite) {
            this.sameSite = sameSite;
            return this;
        }

        public Builder sameSite(final String sameSite) {
            if (Objects.nonNull(sameSite))
                this.sameSite = SameSite.valueOf(sameSite.toUpperCase());
            return this;
        }

        public Builder maxAgeInSeconds(final Long maxAgeInSeconds) {
            this.maxAgeInSeconds = Objects.isNull(maxAgeInSeconds) ?
                    DEFAULT_EXPIRY_TIME : maxAgeInSeconds;
            return this;
        }

        public Builder maxAgeInSeconds(final Integer maxAgeInSeconds) {
            return maxAgeInSeconds(Objects.isNull(maxAgeInSeconds) ?
                    DEFAULT_EXPIRY_TIME : maxAgeInSeconds.longValue());
        }

        public Builder version(final int version) {
            this.version = version;
            return this;
        }

        public Builder comment(final String comment) {
            this.comment = comment;
            return this;
        }

        public HttpCookie build() {
            if (this.sameSite == SameSite.NONE && !this.isSecured) {
                this.isSecured = true;
            }
            return new HttpCookie(this.name, this.value, this.isSecured, this.httpOnly, this.path,
                    this.domain, this.sameSite, this.maxAgeInSeconds, this.version, this.comment);
        }
    }

    /**
     * Represents the same site mode of a cookie
     *
     * @author Selvakumar
     * @version 1.0
     */
    public enum SameSite {
        LAX, STRICT, NONE
    }
}
