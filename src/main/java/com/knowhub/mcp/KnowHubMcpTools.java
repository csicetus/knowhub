package com.knowhub.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class KnowHubMcpTools {

    @Tool(description = "测试 MCP Server 是否正常运行")
    public String hello() {
        return "KnowHub MCP Server 运行正常！";
    }
}
