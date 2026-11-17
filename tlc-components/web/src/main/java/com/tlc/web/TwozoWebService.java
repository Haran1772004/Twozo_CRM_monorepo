package com.tlc.web;

/**
 * @author Abishek
 * @version 1.0
 */
public interface TwozoWebService extends WebService
{
    void register(final WebAction annotation, final Action action);

    void unregister(String path);
}
