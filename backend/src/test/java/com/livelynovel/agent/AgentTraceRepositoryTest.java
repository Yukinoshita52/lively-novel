package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentToolCallEntity;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentToolCallStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentToolCallRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/agent-trace-repository-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AgentTraceRepositoryTest {

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private AgentToolCallRepository agentToolCallRepository;

    @Test
    void persistsAgentRunAndFindsToolCallsByRunIdOrderedByStartedAt() {
        AgentRunEntity run = new AgentRunEntity();
        run.setId("agent-run-1");
        run.setUserGoal("Convert chapter one into a screenplay");
        run.setNovelId("novel-1");
        run.setConversionId("conversion-1");
        run.setScreenplayType(ScreenplayTypeEnum.FILM);
        run.setStatus(AgentRunStatusEnum.RUNNING);
        run.setCurrentStepIndex(2);
        run.setFinalArtifactRef("artifact://draft");

        AgentRunEntity savedRun = agentRunRepository.saveAndFlush(run);

        AgentToolCallEntity toolCall = new AgentToolCallEntity();
        toolCall.setId("tool-call-1");
        toolCall.setRunId(savedRun.getId());
        toolCall.setStepId("step-1");
        toolCall.setToolName("screenplay.searchContext");
        toolCall.setSideEffectLevel(AgentToolSideEffectLevelEnum.NONE);
        toolCall.setInputJson("{\"query\":\"chapter one\"}");
        toolCall.setOutputJson("{\"matches\":1}");
        toolCall.setStatus(AgentToolCallStatusEnum.COMPLETED);
        toolCall.setStartedAt(Instant.parse("2026-06-27T01:00:00Z"));
        toolCall.setCompletedAt(Instant.parse("2026-06-27T01:00:02Z"));

        agentToolCallRepository.saveAndFlush(toolCall);

        List<AgentToolCallEntity> toolCalls = agentToolCallRepository.findByRunIdOrderByStartedAtAsc(savedRun.getId());

        assertThat(savedRun.getId()).isEqualTo("agent-run-1");
        assertThat(savedRun.getUserGoal()).isEqualTo("Convert chapter one into a screenplay");
        assertThat(savedRun.getNovelId()).isEqualTo("novel-1");
        assertThat(savedRun.getScreenplayType()).isEqualTo(ScreenplayTypeEnum.FILM);
        assertThat(savedRun.getStatus()).isEqualTo(AgentRunStatusEnum.RUNNING);
        assertThat(savedRun.getCurrentStepIndex()).isEqualTo(2);
        assertThat(savedRun.getStartedAt()).isNotNull();
        assertThat(savedRun.getUpdatedAt()).isNotNull();

        assertThat(toolCalls).hasSize(1);
        AgentToolCallEntity savedToolCall = toolCalls.get(0);
        assertThat(savedToolCall.getId()).isEqualTo("tool-call-1");
        assertThat(savedToolCall.getRunId()).isEqualTo("agent-run-1");
        assertThat(savedToolCall.getToolName()).isEqualTo("screenplay.searchContext");
        assertThat(savedToolCall.getSideEffectLevel()).isEqualTo(AgentToolSideEffectLevelEnum.NONE);
        assertThat(savedToolCall.getInputJson()).isEqualTo("{\"query\":\"chapter one\"}");
        assertThat(savedToolCall.getOutputJson()).isEqualTo("{\"matches\":1}");
        assertThat(savedToolCall.getStatus()).isEqualTo(AgentToolCallStatusEnum.COMPLETED);
        assertThat(savedToolCall.getStartedAt()).isEqualTo(Instant.parse("2026-06-27T01:00:00Z"));
        assertThat(savedToolCall.getCompletedAt()).isEqualTo(Instant.parse("2026-06-27T01:00:02Z"));
    }
}
