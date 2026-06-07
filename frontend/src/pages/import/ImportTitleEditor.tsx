import { Button, Input } from 'antd'

type ImportTitleEditorProps = {
  title: string
  saving: boolean
  disabled: boolean
  onChange: (title: string) => void
  onSave: () => void
}

function ImportTitleEditor({
  title,
  saving,
  disabled,
  onChange,
  onSave,
}: ImportTitleEditorProps) {
  return (
    <div className="field-stack title-edit-stack">
      <label className="field-label" htmlFor="novel-title">作品标题</label>
      <div className="title-edit-row">
        <Input
          id="novel-title"
          size="large"
          value={title}
          onChange={(event) => onChange(event.target.value)}
          onPressEnter={onSave}
          placeholder="输入作品标题"
        />
        <Button
          size="large"
          onClick={onSave}
          loading={saving}
          disabled={disabled}
        >
          保存标题
        </Button>
      </div>
    </div>
  )
}

export default ImportTitleEditor
