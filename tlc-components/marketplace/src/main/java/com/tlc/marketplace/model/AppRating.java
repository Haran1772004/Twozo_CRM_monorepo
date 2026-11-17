package com.tlc.marketplace.model;

import java.io.Serializable;

/**
 * Refers the model of {@code AppRating}.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public final class AppRating implements Serializable {

    private double rating;
    private Long totalRatings;

    public Long getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(final Long totalRatings) {
        this.totalRatings = totalRatings;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(final double rating) {
        this.rating = rating;
    }
}