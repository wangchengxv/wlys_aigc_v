import { useEffect, useState } from 'react';
import { Card, Row, Col, Typography, Button, Space } from 'antd';
import { PlayCircleOutlined, EditOutlined, TeamOutlined, VideoCameraAddOutlined } from '@ant-design/icons';
import { get } from '@/api/request';

const { Title, Paragraph } = Typography;

interface HealthInfo {
  message: string;
}

function Home() {
  const [health, setHealth] = useState<string>('');

  useEffect(() => {
    checkHealth();
  }, []);

  const checkHealth = async () => {
    try {
      const res = await get<HealthInfo>('/health');
      setHealth(res.data.message);
    } catch (error) {
      setHealth('后端服务未连接');
    }
  };

  return (
    <div className="home-page">
      <Title level={2}>欢迎使用AIGC漫剧工具</Title>
      
      <Card 
        title="系统状态" 
        style={{ marginBottom: 24 }}
        extra={<Button onClick={checkHealth}>刷新</Button>}
      >
        <Paragraph>
          <strong>后端服务：</strong>
          {health || '检查中...'}
        </Paragraph>
      </Card>

      <Title level={4}>快速开始</Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card 
            hoverable 
            cover={<EditOutlined style={{ fontSize: 48, color: '#1890ff', margin: 20 }} />}
          >
            <Card.Meta 
              title="创建剧本" 
              description="编写漫剧故事脚本"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card 
            hoverable 
            cover={<TeamOutlined style={{ fontSize: 48, color: '#52c41a', margin: 20 }} />}
          >
            <Card.Meta 
              title="角色设计" 
              description="创建AI角色形象"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card 
            hoverable 
            cover={<VideoCameraAddOutlined style={{ fontSize: 48, color: '#faad14', margin: 20 }} />}
          >
            <Card.Meta 
              title="分镜生成" 
              description="AI智能生成分镜"
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card 
            hoverable 
            cover={<PlayCircleOutlined style={{ fontSize: 48, color: '#f5222d', margin: 20 }} />}
          >
            <Card.Meta 
              title="视频生成" 
              description="一键生成漫剧视频"
            />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 24 }}>
        <Title level={4}>使用说明</Title>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Paragraph>
            1. 首先在「角色管理」中创建AI角色，定义角色的外貌和性格特征
          </Paragraph>
          <Paragraph>
            2. 在剧本编辑页面编写故事脚本，支持分章节和多场景
          </Paragraph>
          <Paragraph>
            3. 使用「分镜生成」功能，AI会根据剧本自动创建分镜描述
          </Paragraph>
          <Paragraph>
            4. 最后点击「生成视频」，AI将根据分镜生成完整的漫剧视频
          </Paragraph>
        </Space>
      </Card>
    </div>
  );
}

export default Home;
