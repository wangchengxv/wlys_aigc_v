import { Button, Card, Form, Input, List, message } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { http, type ApiResponse } from "../api/http";

interface Project {
  id: number;
  name: string;
  description?: string;
}

export function ProjectListPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const { data, refetch } = useQuery({
    queryKey: ["projects"],
    queryFn: async () => {
      const resp = await http.get<ApiResponse<Project[]>>("/projects");
      return resp.data.data;
    },
  });

  const createProject = async () => {
    const values = await form.validateFields();
    await http.post("/projects", values);
    message.success("项目创建成功");
    form.resetFields();
    refetch();
  };

  return (
    <div style={{ maxWidth: 920, margin: "24px auto", display: "grid", gap: 16 }}>
      <Card title="新建项目">
        <Form layout="inline" form={form}>
          <Form.Item name="name" rules={[{ required: true }]}>
            <Input placeholder="项目名" />
          </Form.Item>
          <Form.Item name="description">
            <Input placeholder="项目描述" />
          </Form.Item>
          <Button type="primary" onClick={createProject}>
            创建
          </Button>
        </Form>
      </Card>

      <Card title="项目列表">
        <List
          dataSource={data ?? []}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button key="open" type="link" onClick={() => navigate(`/workspace/${item.id}`)}>
                  进入工作台
                </Button>,
              ]}
            >
              {item.name}
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
