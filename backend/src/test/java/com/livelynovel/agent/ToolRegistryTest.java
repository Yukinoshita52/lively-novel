package com.livelynovel.agent;

import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryTest {

    @Test
    void registersAndExecutesTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new AgentTool(
                "echo",
                "回显输入",
                AgentToolSideEffectLevelEnum.NONE,
                context -> AgentToolResult.completed("cv-test", "{\"ok\":true}")
        ));

        AgentToolResult result = registry.execute("echo", new AgentToolContext(
                "ar-1234abcd",
                "nv-1234abcd",
                ScreenplayTypeEnum.ANIME,
                null
        ));

        assertThat(result.conversionId()).isEqualTo("cv-test");
        assertThat(result.outputJson()).contains("ok");
    }

    @Test
    void rejectsDuplicateToolName() {
        ToolRegistry registry = new ToolRegistry();
        AgentTool tool = new AgentTool(
                "echo",
                "回显输入",
                AgentToolSideEffectLevelEnum.NONE,
                context -> AgentToolResult.completed(null, "{}")
        );

        registry.register(tool);

        assertThatThrownBy(() -> registry.register(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("工具已注册");
    }

    @Test
    void rejectsUnknownTool() {
        ToolRegistry registry = new ToolRegistry();

        assertThatThrownBy(() -> registry.execute("missing", new AgentToolContext(
                "ar-1234abcd",
                "nv-1234abcd",
                ScreenplayTypeEnum.ANIME,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知工具");
    }
}
