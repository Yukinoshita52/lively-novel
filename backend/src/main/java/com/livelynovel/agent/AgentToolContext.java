package com.livelynovel.agent;

import com.livelynovel.model.enums.ScreenplayTypeEnum;

public record AgentToolContext(
        String runId,
        String novelId,
        ScreenplayTypeEnum screenplayType,
        String conversionId
) {
}
