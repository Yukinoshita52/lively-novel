package com.livelynovel.agent;

import com.livelynovel.model.entity.AgentGuardrailResultEntity;
import com.livelynovel.model.entity.AgentRunEntity;
import com.livelynovel.model.entity.AgentStepEntity;
import com.livelynovel.model.entity.AgentToolCallEntity;
import com.livelynovel.model.enums.AgentRunStatusEnum;
import com.livelynovel.model.enums.AgentStepStatusEnum;
import com.livelynovel.model.enums.AgentToolCallStatusEnum;
import com.livelynovel.model.enums.AgentToolSideEffectLevelEnum;
import com.livelynovel.model.enums.ScreenplayTypeEnum;
import com.livelynovel.repository.AgentGuardrailResultRepository;
import com.livelynovel.repository.AgentRunRepository;
import com.livelynovel.repository.AgentStepRepository;
import com.livelynovel.repository.AgentToolCallRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentOrchestrator {

    private static final String FIXED_PLAN_TOOL = ScreenplayAgentTools.RUN_EXISTING_SCREENPLAY_CONVERSION;
    private static final String FIXED_PLAN_STEP_NAME = "start_existing_conversion";
    private static final String FIXED_PLAN_AGENT_NAME = "ScreenplayConversionAgent";

    private final AgentRunRepository runRepository;
    private final AgentToolCallRepository toolCallRepository;
    private final AgentStepRepository stepRepository;
    private final AgentGuardrailResultRepository guardrailResultRepository;
    private final ToolRegistry toolRegistry;
    private final GuardrailService guardrailService;
    private final AgentSseEventFactory eventFactory;

    public AgentOrchestrator(
            AgentRunRepository runRepository,
            AgentToolCallRepository toolCallRepository,
            AgentStepRepository stepRepository,
            AgentGuardrailResultRepository guardrailResultRepository,
            ToolRegistry toolRegistry,
            GuardrailService guardrailService,
            AgentSseEventFactory eventFactory
    ) {
        this.runRepository = runRepository;
        this.toolCallRepository = toolCallRepository;
        this.stepRepository = stepRepository;
        this.guardrailResultRepository = guardrailResultRepository;
        this.toolRegistry = toolRegistry;
        this.guardrailService = guardrailService;
        this.eventFactory = eventFactory;
    }

    public SseEmitter convertNovel(String novelId, ScreenplayTypeEnum screenplayType) {
        SseEmitter emitter = new SseEmitter(0L);
        AgentRunEntity run = createRun(novelId, screenplayType);
        send(emitter, "agent_started", run.getId(), null, null, null, null, null, "Agent run started");
        send(emitter, "plan_created", run.getId(), null, null, null, null, null, "Fixed conversion plan loaded");
        executeFixedPlan(emitter, run);
        return emitter;
    }

    private AgentRunEntity createRun(String novelId, ScreenplayTypeEnum screenplayType) {
        AgentRunEntity run = new AgentRunEntity();
        run.setId(generateRunId());
        run.setUserGoal("将小说转换为动画剧本");
        run.setNovelId(novelId);
        run.setScreenplayType(screenplayType);
        run.setStatus(AgentRunStatusEnum.RUNNING);
        run.setCurrentStepIndex(0);
        return runRepository.save(run);
    }

    private void executeFixedPlan(SseEmitter emitter, AgentRunEntity run) {
        AgentStepEntity step = createStep(run);
        send(emitter, "step_started", run.getId(), step.getId(), null, run.getConversionId(), null, null,
                step.getStepName() + " started");
        AgentTool tool = toolRegistry.getRequired(FIXED_PLAN_TOOL);
        AgentToolContext context = new AgentToolContext(
                run.getId(),
                run.getNovelId(),
                run.getScreenplayType(),
                run.getConversionId()
        );
        List<AgentGuardrailCheckResult> guardrailResults = guardrailService.checkBeforeToolCall(
                tool,
                context,
                step,
                List.of(AgentToolSideEffectLevelEnum.WRITE_COST)
        );
        persistGuardrailResults(run, step, guardrailResults);
        guardrailResults.forEach(result -> send(
                emitter,
                "guardrail_checked",
                run.getId(),
                step.getId(),
                null,
                run.getConversionId(),
                result.guardrailName(),
                result.status().name(),
                result.message()
        ));

        if (hasBlockingGuardrail(guardrailResults)) {
            String message = guardrailResults.stream()
                    .filter(AgentGuardrailCheckResult::blocksExecution)
                    .findFirst()
                    .map(AgentGuardrailCheckResult::message)
                    .orElse("Guardrail blocked tool execution");
            markStepBlocked(step, message);
            markRunFailed(run, new IllegalStateException(message));
            send(emitter, "agent_failed", run.getId(), step.getId(), null, run.getConversionId(), null, null, message);
            emitter.complete();
            return;
        }

        AgentToolCallEntity toolCall = createToolCall(run, step, tool);
        send(emitter, "tool_call_started", run.getId(), step.getId(), toolCall.getId(), run.getConversionId(), null, null,
                tool.name() + " started");
        try {
            AgentToolResult result = tool.executor().execute(context);
            markToolCallCompleted(toolCall, result);
            markStepCompleted(step, result);
            markRunCompleted(run, result);
            send(emitter, "tool_call_completed", run.getId(), step.getId(), toolCall.getId(), run.getConversionId(),
                    null, null,
                    tool.name() + " completed");
            send(emitter, "step_completed", run.getId(), step.getId(), null, run.getConversionId(), null, null,
                    step.getStepName() + " completed");
            send(emitter, "agent_completed", run.getId(), null, null, run.getConversionId(), null, null,
                    "Agent run completed");
            emitter.complete();
        } catch (Exception e) {
            markToolCallFailed(toolCall, e);
            markStepFailed(step, e);
            markRunFailed(run, e);
            send(emitter, "agent_failed", run.getId(), step.getId(), toolCall.getId(), run.getConversionId(),
                    null, null, e.getMessage());
            emitter.complete();
        }
    }

    private AgentStepEntity createStep(AgentRunEntity run) {
        AgentStepEntity step = new AgentStepEntity();
        step.setId(generateStepId());
        step.setRunId(run.getId());
        step.setStepIndex(run.getCurrentStepIndex());
        step.setStepName(FIXED_PLAN_STEP_NAME);
        step.setAgentName(FIXED_PLAN_AGENT_NAME);
        step.setStatus(AgentStepStatusEnum.RUNNING);
        step.setInputSummary("{\"novelId\":\"" + run.getNovelId() + "\",\"screenplayType\":\""
                + run.getScreenplayType() + "\",\"toolName\":\"" + FIXED_PLAN_TOOL + "\"}");
        step.setStartedAt(Instant.now());
        return stepRepository.save(step);
    }

    private List<AgentGuardrailResultEntity> persistGuardrailResults(
            AgentRunEntity run,
            AgentStepEntity step,
            List<AgentGuardrailCheckResult> results
    ) {
        return results.stream()
                .map(result -> {
                    AgentGuardrailResultEntity entity = new AgentGuardrailResultEntity();
                    entity.setId(generateGuardrailResultId());
                    entity.setRunId(run.getId());
                    entity.setStepId(step.getId());
                    entity.setGuardrailName(result.guardrailName());
                    entity.setStatus(result.status());
                    entity.setMessage(result.message());
                    entity.setPayloadJson(result.payloadJson());
                    entity.setCreatedAt(Instant.now());
                    return guardrailResultRepository.save(entity);
                })
                .toList();
    }

    private boolean hasBlockingGuardrail(List<AgentGuardrailCheckResult> results) {
        return results.stream().anyMatch(AgentGuardrailCheckResult::blocksExecution);
    }

    private AgentToolCallEntity createToolCall(AgentRunEntity run, AgentStepEntity step, AgentTool tool) {
        AgentToolCallEntity toolCall = new AgentToolCallEntity();
        toolCall.setId(generateToolCallId());
        toolCall.setRunId(run.getId());
        toolCall.setStepId(step.getId());
        toolCall.setToolName(tool.name());
        toolCall.setSideEffectLevel(tool.sideEffectLevel());
        toolCall.setInputJson("{\"novelId\":\"" + run.getNovelId() + "\",\"screenplayType\":\""
                + run.getScreenplayType() + "\"}");
        toolCall.setStatus(AgentToolCallStatusEnum.RUNNING);
        toolCall.setStartedAt(Instant.now());
        return toolCallRepository.save(toolCall);
    }

    private void markToolCallCompleted(AgentToolCallEntity toolCall, AgentToolResult result) {
        toolCall.setStatus(AgentToolCallStatusEnum.COMPLETED);
        toolCall.setOutputJson(result.outputJson());
        toolCall.setCompletedAt(Instant.now());
        toolCallRepository.save(toolCall);
    }

    private void markStepCompleted(AgentStepEntity step, AgentToolResult result) {
        step.setStatus(AgentStepStatusEnum.COMPLETED);
        step.setOutputSummary(result.outputJson());
        step.setCompletedAt(Instant.now());
        stepRepository.save(step);
    }

    private void markStepFailed(AgentStepEntity step, Exception error) {
        step.setStatus(AgentStepStatusEnum.FAILED);
        step.setErrorMessage(error.getMessage());
        step.setCompletedAt(Instant.now());
        stepRepository.save(step);
    }

    private void markStepBlocked(AgentStepEntity step, String message) {
        step.setStatus(AgentStepStatusEnum.BLOCKED);
        step.setErrorMessage(message);
        step.setOutputSummary("{\"blocked\":true,\"message\":\"" + message + "\"}");
        step.setCompletedAt(Instant.now());
        stepRepository.save(step);
    }

    private void markRunCompleted(AgentRunEntity run, AgentToolResult result) {
        run.setConversionId(result.conversionId());
        run.setStatus(AgentRunStatusEnum.COMPLETED);
        run.setCompletedAt(Instant.now());
        run.setFinalArtifactRef(result.conversionId());
        runRepository.save(run);
    }

    private void markToolCallFailed(AgentToolCallEntity toolCall, Exception error) {
        toolCall.setStatus(AgentToolCallStatusEnum.FAILED);
        toolCall.setErrorMessage(error.getMessage());
        toolCall.setCompletedAt(Instant.now());
        toolCallRepository.save(toolCall);
    }

    private void markRunFailed(AgentRunEntity run, Exception error) {
        run.setStatus(AgentRunStatusEnum.FAILED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(error.getMessage());
        runRepository.save(run);
    }

    private void send(
            SseEmitter emitter,
            String eventName,
            String runId,
            String stepId,
            String toolCallId,
            String conversionId,
            String guardrailName,
            String guardrailStatus,
            String message
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(eventFactory.payload(
                            eventName,
                            runId,
                            stepId,
                            toolCallId,
                            conversionId,
                            guardrailName,
                            guardrailStatus,
                            message
                    )));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private String generateRunId() {
        return "ar-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateToolCallId() {
        return "atc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateStepId() {
        return "step-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateGuardrailResultId() {
        return "gr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
