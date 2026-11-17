package com.tlc.sql.common.internal.update;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import java.util.NavigableMap;
import java.util.NavigableSet;

/**
 * @author Abishek
 * @version 1.0
 */
public interface DataModifier
{
    void modifyForInstall(DataContainer dataContainer);

    void modifyForUpgrade(DataContainer dataContainer);

    void modifyForUnInstall(NavigableMap<Table, NavigableSet<Long>> pkValueMap);
}
