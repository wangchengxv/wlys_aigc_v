import { useEffect, useState } from "react";
import { http, type ApiResponse } from "../api/http";

type TaskStatus = "PENDING" | "RUNNING" | "SUCCESS" | "FAILED" | "CANCELLED";

interface TaskData {
  id: number;
  status: TaskStatus;
  progress: number;
  errorMessage?: string;
}

export function useTaskPolling(taskId?: number) {
  const [task, setTask] = useState<TaskData | undefined>();

  useEffect(() => {
    if (!taskId) return;
    const timer = window.setInterval(async () => {
      const resp = await http.get<ApiResponse<TaskData>>(`/ai-tasks/${taskId}`);
      setTask(resp.data.data);
      if (["SUCCESS", "FAILED", "CANCELLED"].includes(resp.data.data.status)) {
        window.clearInterval(timer);
      }
    }, 3000);
    return () => window.clearInterval(timer);
  }, [taskId]);

  return task;
}
