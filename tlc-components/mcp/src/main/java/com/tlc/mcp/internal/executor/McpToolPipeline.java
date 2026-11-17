package com.tlc.mcp.internal.executor;

import com.tlc.mcp.executor.LayerPosition;
import com.tlc.mcp.executor.McpPipelineChain;
import com.tlc.mcp.executor.McpPipelineLayer;
import com.tlc.mcp.executor.McpToolInvocation;
import com.tlc.mcp.tool.McpToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class McpToolPipeline {

    private final McpExecutionLayer executionLayer;
    private final McpAuthLayer authLayer;
    private final McpValidationLayer validationLayer;

    private final List<McpPipelineLayer> beforeAuthLayers = new CopyOnWriteArrayList<>();
    private final List<McpPipelineLayer> afterAuthLayers = new CopyOnWriteArrayList<>();
    private final List<McpPipelineLayer> afterValidationLayers = new CopyOnWriteArrayList<>();

    public McpToolPipeline(McpAuthLayer authLayer,
                            McpValidationLayer validationLayer,
                            McpExecutionLayer executionLayer) {
        this.authLayer = authLayer;
        this.validationLayer = validationLayer;
        this.executionLayer = executionLayer;
    }

    public void addLayer(McpPipelineLayer layer, LayerPosition position) {
        switch (position) {
            case BEFORE_AUTH      -> beforeAuthLayers.add(layer);
            case AFTER_AUTH       -> afterAuthLayers.add(layer);
            case AFTER_VALIDATION -> afterValidationLayers.add(layer);
        }
    }

    public McpToolResult execute(McpToolInvocation invocation) {
        final List<McpPipelineLayer> chain = new ArrayList<>();
        chain.addAll(beforeAuthLayers);
        chain.add(authLayer);
        chain.addAll(afterAuthLayers);
        chain.add(validationLayer);
        chain.addAll(afterValidationLayers);

        return new ChainImpl(chain, executionLayer).next(invocation);
    }

    private static final class ChainImpl implements McpPipelineChain {

        private final List<McpPipelineLayer> layers;
        private final McpExecutionLayer terminal;
        private int index = 0;

        ChainImpl(List<McpPipelineLayer> layers, McpExecutionLayer terminal) {
            this.layers = layers;
            this.terminal = terminal;
        }

        @Override
        public McpToolResult next(McpToolInvocation invocation) {
            if (index < layers.size()) {
                return layers.get(index++).process(invocation, this);
            }
            return terminal.process(invocation, inv -> McpToolResult.error("Pipeline exhausted"));
        }
    }
}
