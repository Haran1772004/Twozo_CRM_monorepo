package com.tlc.plans.and.billings.models;

/**
 * Represents the plan cancellation feedback entry.
 *
 * @author Sathish
 * @version 1.0
 */
public class CancellationFeedback {

    private String question;
    private String response;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(final String question) {
        this.question = question;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(final String response) {
        this.response = response;
    }
}
