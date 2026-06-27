package com.livelynovel.agent;

import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.service.ScreenplayService;
import org.springframework.stereotype.Component;

@Component
public class ScreenplayAgentTools {

    public static final String RUN_EXISTING_SCREENPLAY_CONVERSION = "runExistingScreenplayConversion";

    public ScreenplayAgentTools(ToolRegistry toolRegistry, ScreenplayService screenplayService) {
        toolRegistry.register(new AgentTool(
                RUN_EXISTING_SCREENPLAY_CONVERSION,
                "复用现有整本剧本转换流程",
                AgentToolSideEffectLevelEnum.WRITE_COST,
                context -> {
                    screenplayService.convertNovel(context.novelId(), context.screenplayType());
                    String conversionId = screenplayService.getLatestConversionId(
                            context.novelId(),
                            context.screenplayType()
                    );
                    return AgentToolResult.completed(
                            conversionId,
                            "{\"conversionId\":\"" + (conversionId == null ? "" : conversionId) + "\"}"
                    );
                }
        ));
    }
}
