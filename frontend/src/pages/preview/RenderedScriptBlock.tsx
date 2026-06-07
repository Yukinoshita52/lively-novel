import type { ScriptBlockRow } from './screenplayPreview'

type RenderedScriptBlockProps = {
  block: ScriptBlockRow
}

const LABELED_TEXT_BLOCKS = new Set(['SHOT', 'INSERT', 'SFX'])

function RenderedScriptBlock({ block }: RenderedScriptBlockProps) {
  if (block.type === 'DIALOGUE' || block.type === 'VO') {
    return (
      <div className={`sp-dialogue-block${block.type === 'VO' ? ' sp-voice-over-block' : ''}`} key={block.key}>
        <div className="sp-character">{block.character}</div>
        {block.parenthetical ? <div className="sp-parenthetical">{block.parenthetical}</div> : null}
        <div className="sp-line">{block.line}</div>
      </div>
    )
  }

  if (LABELED_TEXT_BLOCKS.has(block.type)) {
    return (
      <p className="sp-labeled-line" key={block.key}>
        <span>{block.type}</span>
        {block.text}
      </p>
    )
  }

  return (
    <p className={block.type === 'TRANSITION' ? 'sp-transition' : 'sp-action-line'} key={block.key}>
      {block.text}
    </p>
  )
}

export default RenderedScriptBlock
