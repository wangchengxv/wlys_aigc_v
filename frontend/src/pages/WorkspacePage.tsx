import { Card, Tabs } from "antd";

export function WorkspacePage() {
  return (
    <div style={{ maxWidth: 1080, margin: "24px auto" }}>
      <Card title="项目工作台">
        <Tabs
          items={[
            { key: "global", label: "全局设置", children: "一期占位：项目全局设定" },
            { key: "script", label: "剧本", children: "一期占位：剧本生成/上传与分集管理" },
            { key: "subject", label: "主体", children: "一期占位：角色/场景/道具管理" },
            { key: "asset", label: "资产", children: "一期占位：图片资产列表与筛选" },
          ]}
        />
      </Card>
    </div>
  );
}
