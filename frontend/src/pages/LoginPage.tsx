import { useEffect, useMemo, useState } from "react";
import { Alert, Button, Card, Checkbox, Form, Input, Space, Tabs, Typography, message } from "antd";
import { useNavigate } from "react-router-dom";
import { getWechatQrcodeApi, phoneLoginApi, pollWechatStatusApi, sendCodeApi } from "../api/authApi";
import { useUserStore } from "../stores/userStore";

export function LoginPage() {
  const navigate = useNavigate();
  const [phoneForm] = Form.useForm();
  const [countdown, setCountdown] = useState(0);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [sendLoading, setSendLoading] = useState(false);
  const [wechatMessage, setWechatMessage] = useState("正在加载微信登录状态...");
  const [pollId] = useState(() => `poll-${Date.now()}`);
  const setAuth = useUserStore((s) => s.setAuth);

  useEffect(() => {
    if (countdown <= 0) {
      return;
    }
    const timer = window.setTimeout(() => setCountdown((prev) => prev - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [countdown]);

  useEffect(() => {
    let canceled = false;
    getWechatQrcodeApi()
      .then((resp) => {
        if (canceled) {
          return;
        }
        setWechatMessage(resp.message || "微信扫码登录暂不可用");
      })
      .catch(() => {
        if (canceled) {
          return;
        }
        setWechatMessage("微信登录服务暂不可用，请稍后重试");
      });
    return () => {
      canceled = true;
    };
  }, []);

  useEffect(() => {
    const timer = window.setInterval(async () => {
      try {
        const result = await pollWechatStatusApi(pollId);
        if (result.status === "unsupported") {
          setWechatMessage("当前环境未启用微信登录，请使用手机号验证码登录");
        }
      } catch {
        // noop
      }
    }, 5000);
    return () => window.clearInterval(timer);
  }, [pollId]);

  const protocolText = useMemo(() => "登录即表示您同意并遵守《用户协议》与《隐私政策》", []);

  const onSendCode = async () => {
    const phone = phoneForm.getFieldValue("phone") as string;
    if (!/^1[3-9]\d{9}$/.test(phone ?? "")) {
      message.error("请输入正确的手机号");
      return;
    }

    setSendLoading(true);
    try {
      const result = await sendCodeApi(phone);
      setCountdown(result.resendAfterSeconds ?? 60);
      message.success(result.debugCode ? `验证码已发送（开发环境验证码：${result.debugCode}）` : "验证码已发送");
    } catch (error: any) {
      message.error(error?.response?.data?.message ?? "发送失败，请稍后重试");
    } finally {
      setSendLoading(false);
    }
  };

  const onPhoneLogin = async (values: { phone: string; code: string; autoLogin?: boolean }) => {
    setSubmitLoading(true);
    try {
      const result = await phoneLoginApi({
        phone: values.phone,
        code: values.code,
        autoLogin: Boolean(values.autoLogin),
      });
      if (values.autoLogin) {
        localStorage.setItem("miioo_token", result.token);
        sessionStorage.removeItem("miioo_token");
      } else {
        sessionStorage.setItem("miioo_token", result.token);
        localStorage.removeItem("miioo_token");
      }
      setAuth({
        userId: result.userInfo.userId,
        username: result.userInfo.username,
        phone: result.userInfo.phone,
        token: result.token,
      });
      message.success("登录成功");
      navigate("/projects");
    } catch (error: any) {
      message.error(error?.response?.data?.message ?? "登录失败，请稍后重试");
    } finally {
      setSubmitLoading(false);
    }
  };

  return (
    <Card title="Miioo 登录" style={{ maxWidth: 420, margin: "80px auto" }}>
      <Tabs
        defaultActiveKey="phone"
        items={[
          {
            key: "phone",
            label: "验证码登录",
            children: (
              <Form form={phoneForm} layout="vertical" onFinish={onPhoneLogin}>
                <Form.Item
                  name="phone"
                  label="手机号"
                  rules={[{ required: true, pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号" }]}
                >
                  <Input placeholder="请输入手机号" />
                </Form.Item>
                <Form.Item
                  name="code"
                  label="验证码"
                  rules={[{ required: true, pattern: /^\d{6}$/, message: "请输入6位验证码" }]}
                >
                  <Input
                    placeholder="请输入验证码"
                    suffix={
                      <Button type="link" onClick={onSendCode} loading={sendLoading} disabled={countdown > 0}>
                        {countdown > 0 ? `${countdown}s后重发` : "获取验证码"}
                      </Button>
                    }
                  />
                </Form.Item>
                <Form.Item name="autoLogin" valuePropName="checked">
                  <Checkbox>下次自动登录</Checkbox>
                </Form.Item>
                <Space direction="vertical" style={{ width: "100%" }}>
                  <Button type="primary" htmlType="submit" loading={submitLoading} block>
                    登录/注册
                  </Button>
                  <Typography.Text type="secondary">{protocolText}</Typography.Text>
                </Space>
              </Form>
            ),
          },
          {
            key: "wechat",
            label: "微信登录",
            children: (
              <Space direction="vertical" style={{ width: "100%" }}>
                <div
                  style={{
                    width: 200,
                    height: 200,
                    border: "1px dashed #d9d9d9",
                    borderRadius: 8,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    margin: "0 auto",
                    color: "#999",
                  }}
                >
                  微信二维码区域
                </div>
                <Alert type="info" message={wechatMessage} showIcon />
                <Typography.Text type="secondary">{protocolText}</Typography.Text>
              </Space>
            ),
          },
        ]}
      />
    </Card>
  );
}
