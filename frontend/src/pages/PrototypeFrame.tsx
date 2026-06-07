import type { ReactNode } from 'react'
import { buildFlowSteps, type FlowStepKey } from './prototypeFlow'
import type { FlowStepNavigation } from './appNavigation'

type PrototypeFrameProps = {
  currentStep: FlowStepKey
  children: ReactNode
  maxWidth?: number
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}

type PrototypeHeroProps = {
  eyebrow: string
  title: ReactNode
  meta?: ReactNode
  action?: ReactNode
}

type PrototypePanelTitleProps = {
  code: string
  title: ReactNode
  meta?: ReactNode
}

export function PrototypeFrame({
  currentStep,
  children,
  maxWidth = 1180,
  flowNavigation,
  onNavigateStep,
}: PrototypeFrameProps) {
  return (
    <div className="prototype-shell">
      <header className="prototype-topbar">
        <div className="prototype-brand">
          <span className="prototype-seal">活</span>
          <div>
            <div className="prototype-brand-en">Lively Novel</div>
            <div className="prototype-brand-zh">活字成剧</div>
          </div>
        </div>
      </header>

      <main className="prototype-page" style={{ maxWidth }}>
        <FlowNav currentStep={currentStep} flowNavigation={flowNavigation} onNavigateStep={onNavigateStep} />
        {children}
      </main>
    </div>
  )
}

export function FlowNav({
  currentStep,
  flowNavigation,
  onNavigateStep,
}: {
  currentStep: FlowStepKey
  flowNavigation?: FlowStepNavigation
  onNavigateStep?: (step: FlowStepKey) => void
}) {
  return (
    <nav className="prototype-flow" aria-label="转换流程">
      {buildFlowSteps(currentStep).map((step, index, steps) => {
        const navigation = flowNavigation?.[step.key]
        const clickable = Boolean(onNavigateStep && navigation?.clickable)
        const itemClassName = `prototype-flow-item${step.active ? ' active' : ''}${step.done ? ' done' : ''}${clickable ? ' clickable' : ''}${navigation && !navigation.enabled ? ' blocked' : ''}`

        const handleClick = clickable && onNavigateStep ? () => onNavigateStep(step.key) : undefined

        return (
          <span className="prototype-flow-item-wrap" key={step.key}>
            {clickable ? (
              <button className={itemClassName} type="button" onClick={handleClick}>
                {step.number ? <span>{step.number}</span> : null}
                {step.label}
              </button>
            ) : (
              <span className={itemClassName}>
                {step.number ? <span>{step.number}</span> : null}
                {step.label}
              </span>
            )}
            {index < steps.length - 1 ? <span className="prototype-flow-sep">→</span> : null}
          </span>
        )
      })}
    </nav>
  )
}

export function PrototypeHero({ eyebrow, title, meta, action }: PrototypeHeroProps) {
  return (
    <div className="prototype-hero">
      <div>
        <div className="prototype-eyebrow">{eyebrow}</div>
        <h1 className="prototype-title">{title}</h1>
        {meta ? <div className="prototype-title-meta">{meta}</div> : null}
      </div>
      {action ? <div className="prototype-hero-action">{action}</div> : null}
    </div>
  )
}

export function PrototypePanelTitle({ code, title, meta }: PrototypePanelTitleProps) {
  return (
    <div className="prototype-panel-title">
      <span className="prototype-panel-code">{code}</span>
      <span>{title}</span>
      {meta ? <span className="prototype-panel-meta">{meta}</span> : null}
    </div>
  )
}
