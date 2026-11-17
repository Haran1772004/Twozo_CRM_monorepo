package com.tlc.cluster.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

/**
 * Maintains cluster module related error codes
 *
 * @author Selvakumar
 */
public enum ClusterErrorCodes implements ErrorCodeProvider {

    FAILED_TO_ESTABLISH_CONNECTION_WITH_ZOOKEEPER(0x01),
    NODE_LISTENER_REGISTRATION_FAILED(0x02),
    NODE_DELETE_ACTION_FAILED(0x03),
    CHILD_NODES_DATA_FETCH_FAILED(0x04),
    CHILD_NODES_FETCH_ACTION_FAILED(0x05),
    CHILD_NODES_WACTHER_REGISTRATION_FAILED(0x06),
    NODE_WATCHER_REGISTRATION_FAILED(0x07),
    NODE_EXISTENCE_CHECK_FAILED(0x08),
    DATA_UPDATE_ACTION_FAILED(0x09),
    DATA_NODE_NOT_FOUND(0x10),
    NODE_CREATION_FAILED(0x11),
    NODE_ALREADY_EXISTS(0x12),
    LOCK_ACQUISITION_IS_IN_PROGRESS(0x13),
    LOCK_ACQUISITION_FAILED(0x14),
    LOCK_RELEASE_ACTION_FAILED(0x15),
    DATA_FETCH_ACTION_FAILED(0x16),
    FAILED_TO_STOP_ZOOKEEPER_CONNECTION(0x17);

    private final int code;

    ClusterErrorCodes(int localCode) {
        this.code = ClusterErrorCodes.CommonErrorCodesGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class CommonErrorCodesGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new ClusterErrorCodes.CommonErrorCodesGroup();

        @Override
        public int getPrefix() {
            return 0x00_0_0001;
        }
    }
}
