import {
  buildFlowSteps,
  buildPipelinePhases,
  formatStreamEvent,
} from './prototypeFlow.ts'

function assert(condition: boolean, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

const importSteps = buildFlowSteps('import')
assert(importSteps.length === 5, '流程条应包含导入、转换、预览、打磨、导出五个步骤')
assert(importSteps[0].active, '导入页应高亮导入步骤')
assert(importSteps[0].label === '导入', '导入步骤文案应简洁')

const convertSteps = buildFlowSteps('convert')
assert(convertSteps[0].done, '转换页应将导入步骤标记为已完成')
assert(convertSteps[1].active, '转换页应高亮转换步骤')
assert(!convertSteps[2].done, '转换页不应提前标记预览完成')

const runningPhases = buildPipelinePhases({
  completed: false,
  convertError: null,
  finishedSceneCount: 2,
  totalSceneCount: 5,
})
assert(runningPhases[0].status === 'done', '已有切场总数时全局分析应显示完成')
assert(runningPhases[1].status === 'active', '逐场生成中应显示 active')
assert(runningPhases[1].progress === 40, '逐场生成进度应按已完成场数计算')
assert(runningPhases[2].status === 'idle', '未完成前组装落库应保持等待')

const completedPhases = buildPipelinePhases({
  completed: true,
  convertError: null,
  finishedSceneCount: 5,
  totalSceneCount: 5,
})
assert(completedPhases.every((phase) => phase.status === 'done'), '转换完成后所有阶段应显示完成')

const failedPhases = buildPipelinePhases({
  completed: false,
  convertError: '转换未完成',
  finishedSceneCount: 1,
  totalSceneCount: 5,
})
assert(failedPhases[1].status === 'failed', '转换失败时当前逐场生成阶段应显示失败')

assert(
  formatStreamEvent({ type: 'scene_completed', message: '已生成第 1 章第 2 场：天台' }) ===
    'event: scene_completed 已生成第 1 章第 2 场：天台',
  '事件流应使用 event: 前缀形成原型里的 SSE 观感',
)
