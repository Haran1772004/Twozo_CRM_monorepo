package com.tlc.social.common.whatsapp.events;

/**
 * Represents the WhatsApp events which will be used in the event callback
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAEvents {

    public static final String FETCH_WA_BUSINESS_ACCOUNT = "social.wa.business.account.retrieval.event";
    public static final String FETCH_WA_BUSINESS_PHONE_NUMBERS = "social.wa.business.phone.numbers.retrieval.event";
    public static final String FETCH_WA_BUSINESS_PHONE_NUMBERS_WITH_CHAT = "social.wa.business.phone.numbers.with.chat.retrieval.event";
    public static final String FETCH_WA_TEMPLATES = "social.wa.templates.retrieval.event";
    public static final String FETCH_WA_TEMPLATE = "social.wa.template.retrieval.event";
    public static final String FETCH_WA_TEMPLATE_NAMES = "social.wa.template.names.retrieval.event";
    public static final String SYNC_WA_TEMPLATES = "social.wa.templates.sync.event";
    public static final String CHECK_WA_TEMPLATE_SYNC_STATUS = "social.wa.template.sync.status.check.event";
    public static final String FETCH_WA_TEMPLATE_LANGUAGES = "social.wa.template.languages.retrieval.event";
    public static final String FETCH_WA_TEMPLATES_WITH_COMPONENTS = "social.wa.templates.with.components.retrieval.event";
    public static final String FETCH_WA_TEMPLATE_COUNT = "social.wa.template.count.retrieval.event";
}