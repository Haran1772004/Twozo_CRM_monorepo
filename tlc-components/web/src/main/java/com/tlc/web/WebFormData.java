package com.tlc.web;

import com.tlc.attachment.io.File;

import java.util.Collection;
import java.util.List;

/**
 * Provides Web form-data services
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface WebFormData {

    /**
     * Gets the value of given query parameter
     *
     * @param key query parameter
     * @return the value of given query parameter
     */
    String getString(String key);

    /**
     * Gets the collection of query parameter keys
     *
     * @return the {@link Collection} of query parameter keys
     */
    Collection<String> getParams();

    /**
     * Checks whether the given parameter is file or not
     *
     * @param key query parameter
     * @return true if the query parameter contains file
     */
    boolean isFileItem(String key);

    /**
     * Gets the list of files contained in the query
     *
     * @param key query parameter
     * @return the {@link List} of files
     */
    List<File> getFiles(String key);

}