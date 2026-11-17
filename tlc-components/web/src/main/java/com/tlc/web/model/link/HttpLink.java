package com.tlc.web.model.link;

/**
 * <p>
 * Represents a {@link HttpLink}, providing common properties and behaviors for subclasses
 * that handle specific types of links, such as permanent or temporary links.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class HttpLink {

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
