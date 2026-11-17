package com.tlc.sql.common.internal.listeners;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.listeners.CompleteRowListener;
import com.tlc.sql.common.api.listeners.RowAddListener;
import com.tlc.sql.common.api.listeners.RowDeleteListener;
import com.tlc.sql.common.api.listeners.RowIdDeleteListener;
import com.tlc.sql.common.api.listeners.RowListener;
import com.tlc.sql.common.api.listeners.RowUpdateListener;


/**
 * @author Abishek
 * @version 1.0
 */
public class RowListenerHandler
{
    private final static class InstanceHolder
    {
        private static final RowListenerHandler INSTANCE = new RowListenerHandler();
    }

    public static RowListenerHandler getInstance()
    {
        return InstanceHolder.INSTANCE;
    }

    private final RowListenerContainer rowListenerContainer;
    private RowListenerHandler()
    {
        this.rowListenerContainer = RowListenerContainer.getInstance();
    }

    public void listenForChanges(Table table, RowListener listener)
    {
        rowListenerContainer.addAddListener(table, listener);
        rowListenerContainer.addUpdateListener(table, listener);
        rowListenerContainer.addDeleteListener(table, listener);
    }

    public void listenForChanges(Table table, CompleteRowListener listener)
    {
        rowListenerContainer.addAddListener(table, listener);
        rowListenerContainer.addUpdateListener(table, listener);
        rowListenerContainer.addDeleteListener(table, listener);
    }

    public void listenForAddChanges(Table table, RowAddListener listener)
    {
        rowListenerContainer.addAddListener(table, listener);
    }

    public void listenForUpdateChanges(Table table, RowUpdateListener listener)
    {
        rowListenerContainer.addUpdateListener(table, listener);
    }

    public void listenForDeleteChanges(Table table, RowIdDeleteListener listener)
    {
        rowListenerContainer.addDeleteListener(table, listener);
    }

    public void listenForDeleteChanges(Table table, RowDeleteListener listener)
    {
        rowListenerContainer.addDeleteListener(table, listener);
    }

}
