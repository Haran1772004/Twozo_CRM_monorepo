package com.tlc.sql.common.internal.update;

import com.tlc.sql.common.api.DataContainer;

/**
 * @author Abishek
 * @version 1.0
 */
public interface PreCommitCallback
{
    void call(DataContainer dataContainer);
}
