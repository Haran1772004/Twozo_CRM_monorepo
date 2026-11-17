package com.tlc.sql.oltp.internal.meta.engine;

public class DefaultOltpEngine implements OltpTableEngine {

    @Override
    public String getName() {
        return "Default";
    }
}