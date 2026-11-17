package com.tlc.mcp.tool;

import java.util.List;

public interface OptionsProvider {

    List<String> options();

    final class None implements OptionsProvider {
        public static final None INSTANCE = new None();

        private None() {}

        @Override
        public List<String> options() {
            return List.of();
        }
    }
}
