package com.tlc.social.server.whatsapp.internal.model;

import com.tlc.social.common.whatsapp.model.template.WATemplate;

import java.util.Collection;

/**
 * A record which represents the whatsapp template response
 *
 * @param nextUrl   Represents the next url in the paging response to retrieve the next set of templates
 * @param templates Represents the retrieved templates
 * @author Maharaja S
 * @version 1.0
 */
public record WATemplateResponse(String nextUrl, Collection<WATemplate> templates) {
}