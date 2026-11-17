package com.tlc.mcp.internal.status;

import com.tlc.commons.code.ErrorCodeGroup;
import com.tlc.commons.code.ErrorCodeProvider;

public enum McpErrorCodes implements ErrorCodeProvider {

    INVALID_REQUEST (-32600),
    METHOD_NOT_FOUND(-32601),
    INVALID_PARAMS  (-32602),
    INTERNAL_ERROR  (-32603);

    private final int code;

    McpErrorCodes(final int localCode) {
        this.code = McpErrorCodeGroup.GROUP.getConvertedCode(localCode);
    }

    @Override
    public int getCode() {
        return code;
    }

    private static class McpErrorCodeGroup implements ErrorCodeGroup {
        private static final ErrorCodeGroup GROUP = new McpErrorCodeGroup();

        @Override
        public int getPrefix() {
            return 0;
        }
    }
}
