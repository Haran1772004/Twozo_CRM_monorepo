package com.tlc.email.common.model.exchange;

import com.tlc.email.common.model.address.SyncAddressInfo;

/**
 * <p>
 * Provides the functionality for sync email exchange operations, including sending
 * and receiving emails.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class SyncEmailData extends EmailData {

    protected SyncAddressInfo syncAddressInfo;

    public SyncAddressInfo getSyncEmailAddress() {
        return syncAddressInfo;
    }

    public void setSyncEmailAddress(final SyncAddressInfo syncAddressInfo) {
        this.syncAddressInfo = syncAddressInfo;
    }
}
