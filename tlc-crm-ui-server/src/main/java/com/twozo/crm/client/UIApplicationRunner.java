package com.twozo.crm.client;

import com.twozo.crm.client.models.header.WebHeadersProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class UIApplicationRunner implements ApplicationRunner {
    private final WebHeadersProperties webHeadersProperties;

    public UIApplicationRunner(WebHeadersProperties webHeadersProperties) {
        this.webHeadersProperties = webHeadersProperties;
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        System.out.println(webHeadersProperties.getHeaders());
//        final WebHeadersProperties.HeaderConfig csp = webHeadersProperties.getCsp();
//        final WebHeadersProperties.HeaderConfig cacheControl = webHeadersProperties.getCacheControl();
//
//        System.out.println(csp.getName() + " : " + csp.getValue());
//        System.out.println(cacheControl.getName() + " : " + cacheControl.getValue());
    }
}