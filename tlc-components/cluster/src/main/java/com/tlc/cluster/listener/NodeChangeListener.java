package com.tlc.cluster.listener;

/**
 * @author Abishek
 * @version 1.0
 */
public interface NodeChangeListener
{
    void added(String path, byte[] data);

    void modified(String path, byte[] data);

    void removed(String path, byte[] data);
}
