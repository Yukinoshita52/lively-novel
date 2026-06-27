package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentStepStatusEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentStepRepository;
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
        "spring.datasource.url=jdbc:sqlite:./target/agent-step-repository-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AgentStepRepositoryTest {

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private AgentStepRepository agentStepRepository;

    @Test
    void persistsAgentStepsAndFindsByRunIdOrderedByStepIndex() {
        AgentRunEntity run = new AgentRunEntity();
        run.setId("agent-run-step");
        run.setUserGoal("Convert chapter one into a screenplay");
        run.setNovelId("novel-1");
        run.setScreenplayType(ScreenplayTypeEnum.ANIME);
        run.setStatus(AgentRunStatusEnum.RUNNING);
        run.setCurrentStepIndex(0);
        agentRunRepository.saveAndFlush(run);

        agentStepRepository.save(step("step-2", run.getId(), 1, "second_step"));
        agentStepRepository.saveAndFlush(step("step-1", run.getId(), 0, "start_existing_conversion"));

        List<AgentStepEntity> steps = agentStepRepository.findByRunIdOrderByStepIndexAsc(run.getId());

        assertThat(steps).extracting(AgentStepEntity::getId).containsExactly("step-1", "step-2");
        assertThat(steps.get(0).getStepName()).isEqualTo("start_existing_conversion");
        assertThat(steps.get(0).getAgentName()).isEqualTo("ScreenplayConversionAgent");
        assertThat(steps.get(0).getStatus()).isEqualTo(AgentStepStatusEnum.RUNNING);
        assertThat(steps.get(0).getInputSummary()).contains("novel-1");
    }

    private AgentStepEntity step(String id, String runId, int stepIndex, String stepName) {
        AgentStepEntity step = new AgentStepEntity();
        step.setId(id);
        step.setRunId(runId);
        step.setStepIndex(stepIndex);
        step.setStepName(stepName);
        step.setAgentName("ScreenplayConversionAgent");
        step.setStatus(AgentStepStatusEnum.RUNNING);
        step.setInputSummary("{\"novelId\":\"novel-1\"}");
        step.setStartedAt(Instant.parse("2026-06-27T12:55:00Z"));
        return step;
    }
}
