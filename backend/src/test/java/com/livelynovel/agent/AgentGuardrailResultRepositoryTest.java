package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentGuardrailResultEntity;
import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.enums.AgentGuardrailStatusEnum;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentStepStatusEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentGuardrailResultRepository;
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
        "spring.datasource.url=jdbc:sqlite:./target/agent-guardrail-result-repository-test.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AgentGuardrailResultRepositoryTest {

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private AgentStepRepository agentStepRepository;

    @Autowired
    private AgentGuardrailResultRepository guardrailResultRepository;

    @Test
    void persistsGuardrailResultsAndFindsByRunAndStep() {
        AgentRunEntity run = new AgentRunEntity();
        run.setId("agent-run-guardrail");
        run.setUserGoal("Convert chapter one into a screenplay");
        run.setNovelId("novel-1");
        run.setScreenplayType(ScreenplayTypeEnum.ANIME);
        run.setStatus(AgentRunStatusEnum.RUNNING);
        run.setCurrentStepIndex(0);
        agentRunRepository.saveAndFlush(run);

        AgentStepEntity step = new AgentStepEntity();
        step.setId("step-guardrail");
        step.setRunId(run.getId());
        step.setStepIndex(0);
        step.setStepName("start_existing_conversion");
        step.setAgentName("ScreenplayConversionAgent");
        step.setStatus(AgentStepStatusEnum.RUNNING);
        step.setStartedAt(Instant.parse("2026-06-27T12:55:00Z"));
        agentStepRepository.saveAndFlush(step);

        AgentGuardrailResultEntity result = new AgentGuardrailResultEntity();
        result.setId("guardrail-1");
        result.setRunId(run.getId());
        result.setStepId(step.getId());
        result.setGuardrailName("tool_side_effect_allowed");
        result.setStatus(AgentGuardrailStatusEnum.PASS);
        result.setMessage("Tool side effect WRITE_COST is allowed");
        result.setPayloadJson("{\"sideEffectLevel\":\"WRITE_COST\"}");
        result.setCreatedAt(Instant.parse("2026-06-27T12:56:00Z"));
        guardrailResultRepository.saveAndFlush(result);

        List<AgentGuardrailResultEntity> byRun =
                guardrailResultRepository.findByRunIdOrderByCreatedAtAsc(run.getId());
        List<AgentGuardrailResultEntity> byStep =
                guardrailResultRepository.findByStepIdOrderByCreatedAtAsc(step.getId());

        assertThat(byRun).hasSize(1);
        assertThat(byStep).hasSize(1);
        assertThat(byRun.get(0).getGuardrailName()).isEqualTo("tool_side_effect_allowed");
        assertThat(byRun.get(0).getStatus()).isEqualTo(AgentGuardrailStatusEnum.PASS);
        assertThat(byRun.get(0).getPayloadJson()).contains("WRITE_COST");
    }
}
