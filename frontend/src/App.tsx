import { useEffect, useState } from 'react'
import { Card, Spin, Tag, Typography } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import './App.css'

const { Title, Text } = Typography

interface HealthResponse {
  code: number
  data: {
    status: string
  }
  message: string
}

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch('/api/health')
      .then((res) => res.json())
      .then((data) => {
        setHealth(data)
        setLoading(false)
      })
      .catch((err) => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f5f5f5' }}>
      <Card style={{ width: 400, textAlign: 'center' }}>
        <Title level={3}>Lively Novel</Title>
        <Text type="secondary">AI 小说转剧本工具</Text>

        <div style={{ marginTop: 24 }}>
          {loading ? (
            <Spin tip="检测后端状态..." />
          ) : error ? (
            <div>
              <CloseCircleOutlined style={{ fontSize: 32, color: '#ff4d4f' }} />
              <p style={{ marginTop: 8, color: '#ff4d4f' }}>后端连接失败</p>
              <Text type="secondary">{error}</Text>
            </div>
          ) : health?.data?.status === 'UP' ? (
            <div>
              <CheckCircleOutlined style={{ fontSize: 32, color: '#52c41a' }} />
              <p style={{ marginTop: 8 }}>
                <Tag color="success">后端：UP</Tag>
              </p>
              <Text type="secondary" style={{ fontSize: 12 }}>
                API 文档：<a href="http://localhost:8080/doc.html" target="_blank">/doc.html</a>
              </Text>
            </div>
          ) : (
            <div>
              <CloseCircleOutlined style={{ fontSize: 32, color: '#faad14' }} />
              <p style={{ marginTop: 8, color: '#faad14' }}>后端状态异常</p>
            </div>
          )}
        </div>
      </Card>
    </div>
  )
}

export default App
