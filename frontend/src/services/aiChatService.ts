import request from "@/request";
import { unwrapApiData } from "./serviceUtils";
import type { AiConversation, AiChatMessageItem, SimilarAlertItem } from "@/types/business";

export interface ChatRequestParams {
  conversationId?: number | null;
  message: string;
}

export interface ChatResponse {
  conversationId: number;
  message: string;
  functionName?: string;
  functionArgs?: string;
  functionResult?: string;
}

export interface CreateConversationParams {
  title?: string;
}

export interface SimilarSearchParams {
  query: string;
  maxResults?: number;
}

export async function chat(params: ChatRequestParams): Promise<ChatResponse> {
  const response = await request.post("/ai/chat", params);
  return unwrapApiData(response, "AI 聊天请求失败");
}

export async function listConversations(): Promise<AiConversation[]> {
  const response = await request.get("/ai/conversations");
  return unwrapApiData(response, "获取会话列表失败");
}

export async function createConversation(
  params: CreateConversationParams,
): Promise<AiConversation> {
  const response = await request.post("/ai/conversations", params);
  return unwrapApiData(response, "创建会话失败");
}

export async function getMessages(
  conversationId: number,
): Promise<AiChatMessageItem[]> {
  const response = await request.get(
    `/ai/conversations/${conversationId}/messages`,
  );
  return unwrapApiData(response, "获取消息列表失败");
}

export async function deleteConversation(conversationId: number): Promise<void> {
  await request.delete(`/ai/conversations/${conversationId}`);
}

export async function analyzeAlert(alertId: number): Promise<string> {
  const response = await request.post(`/ai/alerts/${alertId}/analyze`);
  return unwrapApiData(response, "报警分析请求失败");
}

export async function searchSimilarAlerts(
  params: SimilarSearchParams,
): Promise<SimilarAlertItem[]> {
  const response = await request.post("/ai/alerts/search-similar", params);
  return unwrapApiData(response, "相似报警搜索失败");
}

export function createEventSource(
  conversationId: number | null,
  message: string,
  onMessage: (token: string) => void,
  onError: (error: Error) => void,
  onComplete: () => void,
): EventSource {
  const authToken = sessionStorage.getItem("token") || "";
  const baseUrl = import.meta.env.VITE_API_BASE_URL || "/api";
  const url = `${baseUrl}/ai/chat/stream?conversationId=${conversationId ?? ""}&message=${encodeURIComponent(message)}&token=${encodeURIComponent(authToken)}`;

  const eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    if (event.data === "[DONE]") {
      eventSource.close();
      onComplete();
      return;
    }
    onMessage(event.data);
  };

  eventSource.onerror = () => {
    eventSource.close();
    onError(new Error("SSE connection error"));
  };

  return eventSource;
}

export async function chatStream(
  conversationId: number | null,
  message: string,
  onMessage: (token: string) => void,
  onError: (error: Error) => void,
  onComplete: () => void,
): Promise<void> {
  const authToken = sessionStorage.getItem("token") || "";
  const baseUrl = import.meta.env.VITE_API_BASE_URL || "/api";

  const response = await fetch(
    `${baseUrl}/ai/chat/stream?conversationId=${conversationId ?? ""}&message=${encodeURIComponent(message)}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${authToken}`,
        Accept: "text/event-stream",
      },
    },
  );

  if (!response.ok) {
    onError(new Error(`HTTP ${response.status}`));
    return;
  }

  const reader = response.body?.getReader();
  if (!reader) {
    onError(new Error("No response body"));
    return;
  }

  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      onComplete();
      break;
    }

    buffer += decoder.decode(value, { stream: true });

    const lines = buffer.split("\n");
    buffer = lines.pop() || "";

    for (const line of lines) {
      if (line.startsWith("data:")) {
        const data = line.slice(5).trim();
        if (data === "[DONE]") {
          onComplete();
          return;
        }
        if (data) {
          onMessage(data);
        }
      }
    }
  }
}