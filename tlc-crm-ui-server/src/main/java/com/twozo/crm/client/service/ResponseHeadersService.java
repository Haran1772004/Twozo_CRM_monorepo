package com.twozo.crm.client.service;

import com.twozo.crm.client.models.SecuredContent;
import com.twozo.crm.client.models.header.WebHeadersProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class ResponseHeadersService {

    @Autowired
    private WebHeadersProperties webHeadersProperties;

    public void load(final HttpHeaders httpHeaders) {
//        final WebHeadersProperties.HeaderConfig cacheControlHeader = webHeadersProperties.getWebHeaders();

//        httpHeaders.add(cacheControlHeader.getName(), cacheControlHeader.getValue());
    }

    public void load(final HttpHeaders httpHeaders, final SecuredContent securedContent) {
//        httpHeaders.add(webHeadersProperties.getCsp().getName(), securedContent.header());
        load(httpHeaders);
    }

}
