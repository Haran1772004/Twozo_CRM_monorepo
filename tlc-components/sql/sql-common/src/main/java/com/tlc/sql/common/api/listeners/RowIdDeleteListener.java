package com.tlc.sql.common.api.listeners;

import java.util.Set;


/**
 * @author Abishek
 * @version 1.0
 */
public interface RowIdDeleteListener extends IgnoreDelete, OutOfRangeNotification, ListenerOrder
{
    void deleteRows(Set<Long> ids);
}
