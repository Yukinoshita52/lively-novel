package com.livelynovel.agent;

import com.livelynovel.model.dto.AgentGuardrailTraceDTO;
import com.livelynovel.model.dto.AgentStepTraceDTO;
import com.livelynovel.model.dto.AgentToolCallTraceDTO;
import com.livelynovel.model.dto.AgentTraceDTO;
import com.livelynovel.model.entity.AgentGuardrailResultEntity;
import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.entity.AgentToolCallEntity;
import com.livelynovel.repository.AgentGuardrailResultRepository;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentStepRepository;
import com.livelynovel.repository.AgentToolCallRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AgentTraceService {

    private final AgentRunRepository runRepository;
    private final AgentStepRepository stepRepository;
    private final AgentGuardrailResultRepository guardrailResultRepository;
    private final AgentToolCallRepository toolCallRepository;

    public AgentTraceService(
            AgentRunRepository runRepository,
            AgentStepRepository stepRepository,
            AgentGuardrailResultRepository guardrailResultRepository,
            AgentToolCallRepository toolCallRepository
    ) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.guardrailResultRepository = guardrailResultRepository;
        this.toolCallRepository = toolCallRepository;
    }

    public Optional<AgentTraceDTO> getTrace(String runId) {
        return runRepository.findById(runId).map(this::toTrace);
    }

    private AgentTraceDTO toTrace(AgentRunEntity run) {
        AgentTraceDTO trace = mapRun(run);
        Map<String, AgentStepTraceDTO> stepsById = new LinkedHashMap<>();
        for (AgentStepEntity step : stepRepository.findByRunIdOrderByStepIndexAsc(run.getId())) {
            AgentStepTraceDTO stepTrace = mapStep(step);
            trace.getSteps().add(stepTrace);
            stepsById.put(step.getId(), stepTrace);
        }
        for (AgentGuardrailResultEntity guardrail :
                guardrailResultRepository.findByRunIdOrderByCreatedAtAsc(run.getId())) {
            AgentStepTraceDTO stepTrace = stepsById.get(guardrail.getStepId());
            if (stepTrace != null) {
                stepTrace.getGuardrails().add(mapGuardrail(guardrail));
            }
        }
        for (AgentToolCallEntity toolCall : toolCallRepository.findByRunIdOrderByStartedAtAsc(run.getId())) {
            AgentToolCallTraceDTO toolCallTrace = mapToolCall(toolCall);
            AgentStepTraceDTO stepTrace = stepsById.get(toolCall.getStepId());
            if (stepTrace == null) {
                trace.getUnassignedToolCalls().add(toolCallTrace);
            } else {
                stepTrace.getToolCalls().add(toolCallTrace);
            }
        }
        return trace;
    }

    private AgentTraceDTO mapRun(AgentRunEntity run) {
        AgentTraceDTO dto = new AgentTraceDTO();
        dto.setRunId(run.getId());
        dto.setUserGoal(run.getUserGoal());
        dto.setNovelId(run.getNovelId());
        dto.setConversionId(run.getConversionId());
        dto.setScreenplayType(run.getScreenplayType() == null ? null : run.getScreenplayType().name());
        dto.setStatus(run.getStatus() == null ? null : run.getStatus().name());
        dto.setCurrentStepIndex(run.getCurrentStepIndex());
        dto.setStartedAt(run.getStartedAt());
        dto.setUpdatedAt(run.getUpdatedAt());
        dto.setCompletedAt(run.getCompletedAt());
        dto.setFinalArtifactRef(run.getFinalArtifactRef());
        dto.setErrorMessage(run.getErrorMessage());
        return dto;
    }

    private AgentStepTraceDTO mapStep(AgentStepEntity step) {
        AgentStepTraceDTO dto = new AgentStepTraceDTO();
        dto.setStepId(step.getId());
        dto.setStepIndex(step.getStepIndex());
        dto.setStepName(step.getStepName());
        dto.setAgentName(step.getAgentName());
        dto.setStatus(step.getStatus() == null ? null : step.getStatus().name());
        dto.setInputSummary(step.getInputSummary());
        dto.setOutputSummary(step.getOutputSummary());
        dto.setStartedAt(step.getStartedAt());
        dto.setCompletedAt(step.getCompletedAt());
        dto.setErrorMessage(step.getErrorMessage());
        return dto;
    }

    private AgentGuardrailTraceDTO mapGuardrail(AgentGuardrailResultEntity guardrail) {
        AgentGuardrailTraceDTO dto = new AgentGuardrailTraceDTO();
        dto.setGuardrailResultId(guardrail.getId());
        dto.setGuardrailName(guardrail.getGuardrailName());
        dto.setStatus(guardrail.getStatus() == null ? null : guardrail.getStatus().name());
        dto.setMessage(guardrail.getMessage());
        dto.setPayloadJson(guardrail.getPayloadJson());
        dto.setCreatedAt(guardrail.getCreatedAt());
        return dto;
    }

    private AgentToolCallTraceDTO mapToolCall(AgentToolCallEntity toolCall) {
        AgentToolCallTraceDTO dto = new AgentToolCallTraceDTO();
        dto.setToolCallId(toolCall.getId());
        dto.setStepId(toolCall.getStepId());
        dto.setToolName(toolCall.getToolName());
        dto.setSideEffectLevel(toolCall.getSideEffectLevel() == null ? null : toolCall.getSideEffectLevel().name());
        dto.setInputJson(toolCall.getInputJson());
        dto.setOutputJson(toolCall.getOutputJson());
        dto.setStatus(toolCall.getStatus() == null ? null : toolCall.getStatus().name());
        dto.setStartedAt(toolCall.getStartedAt());
        dto.setCompletedAt(toolCall.getCompletedAt());
        dto.setErrorMessage(toolCall.getErrorMessage());
        return dto;
    }
}
