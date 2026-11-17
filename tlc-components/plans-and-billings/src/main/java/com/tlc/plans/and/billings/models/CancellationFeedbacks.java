package com.tlc.plans.and.billings.models;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a collection of cancellation feedback objects.
 * Provides methods to manipulate and serialize feedback data.
 *
 * @author Sathish
 * @version 1.0
 */
public class CancellationFeedbacks {

    private final Collection<CancellationFeedback> feedbacks;

    private String feedbackAsJsonString;

    public CancellationFeedbacks() {
        feedbacks = new ArrayList<>();
    }

    public void add(final CancellationFeedback cancellationFeedback) {
        feedbacks.add(cancellationFeedback);
    }

    public String toJsonString() {
        if (Objects.isNull(feedbackAsJsonString)) {
            final JsonObject feedbackJson = Json.object();

            for (final CancellationFeedback feedback : feedbacks) {
                feedbackJson.put(feedback.getQuestion(), feedback.getResponse());
            }
            return feedbackAsJsonString = 0 == feedbackJson.size() ? null : feedbackJson.toString();
        }
        return feedbackAsJsonString;
    }
}
