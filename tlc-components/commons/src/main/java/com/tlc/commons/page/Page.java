package com.tlc.commons.page;

import java.util.Collection;

/**
 * <p>
 * A model to represent the page
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class Page<T> {

    private Collection<T> data;
    private int number;

    public Page() {
    }

    public Page(final int number) {
        this.number = number;
    }

    public Collection<T> getData() {
        return data;
    }

    public void setData(final Collection<T> data) {
        this.data = data;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(final int number) {
        this.number = number;
    }
}