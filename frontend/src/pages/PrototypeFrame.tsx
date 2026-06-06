import type { ReactNode } from 'react'
import { buildFlowSteps, type FlowStepKey } from './prototypeFlow'

type PrototypeFrameProps = {
  currentStep: FlowStepKey
  children: ReactNode
  maxWidth?: number
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

export function PrototypeFrame({ currentStep, children, maxWidth = 1180 }: PrototypeFrameProps) {
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
        <FlowNav currentStep={currentStep} />
        {children}
      </main>
    </div>
  )
}

export function FlowNav({ currentStep }: { currentStep: FlowStepKey }) {
  return (
    <nav className="prototype-flow" aria-label="转换流程">
      {buildFlowSteps(currentStep).map((step, index, steps) => (
        <span className="prototype-flow-item-wrap" key={step.key}>
          <span className={`prototype-flow-item${step.active ? ' active' : ''}${step.done ? ' done' : ''}`}>
            <span>{step.number}</span>
            {step.label}
          </span>
          {index < steps.length - 1 ? <span className="prototype-flow-sep">→</span> : null}
        </span>
      ))}
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
