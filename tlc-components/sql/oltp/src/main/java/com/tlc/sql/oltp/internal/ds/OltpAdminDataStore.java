package com.tlc.sql.oltp.internal.ds;

import com.tlc.sql.common.api.ds.AdminDataStore;
import org.javatuples.Pair;

public interface OltpAdminDataStore extends AdminDataStore {

    long getSequenceNextValue(String sequenceName);

    Pair<Long, Long> getSequenceNextAndLimitValue(String sequenceName);
}