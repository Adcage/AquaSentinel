<template>
  <div class="ai-chat-panel">
    <div class="chat-header">
      <span class="chat-title">{{ title || "AI 助手" }}</span>
      <el-button v-if="showClose" text class="close-btn" @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0 && !isLoading" class="welcome-section">
        <p class="welcome-text">你好，我是 AquaSentinel 智能助手。</p>
        <p class="welcome-subtext">你可以问我：</p>
        <div class="quick-questions">
          <el-button
            v-for="q in quickQuestions"
            :key="q"
            size="small"
            round
            @click="sendQuickQuestion(q)"
          >
            {{ q }}
          </el-button>
        </div>
      </div>

      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="['message-item', msg.role]"
      >
        <div class="message-bubble">
          <div v-if="msg.role === 'function'" class="function-info">
            <el-tag size="small" type="info">调用: {{ msg.functionName }}</el-tag>
          </div>
          <div
            v-if="msg.role === 'assistant'"
            class="message-content markdown-body"
            v-html="renderMarkdown(msg.content)"
          ></div>
          <div v-else class="message-content">{{ msg.content }}</div>
        </div>
      </div>

      <div v-if="isLoading" class="message-item assistant loading">
        <div class="message-bubble">
          <span class="loading-dots">
            <span>.</span><span>.</span><span>.</span>
          </span>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入你的问题..."
        resize="none"
        @keydown.enter.exact="handleSend"
      />
      <el-button
        type="primary"
        :disabled="!inputText.trim() || isLoading"
        :loading="isLoading"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Close } from "@element-plus/icons-vue";
import { ref, onMounted, nextTick, watch } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
import type { AiChatMessageItem } from "@/types/business";
import { chatStream, createConversation, getMessages } from "@/services/aiChatService";

marked.setOptions({
  breaks: true,
  gfm: true,
});

interface Props {
  title?: string;
  conversationId?: number | null;
  showClose?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  title: "AI 助手",
  conversationId: null,
  showClose: false,
});

const emit = defineEmits<{
  close: [];
  conversationCreated: [id: number];
}>();

const messagesRef = ref<HTMLElement | null>(null);
const inputText = ref("");
const isLoading = ref(false);
const messages = ref<AiChatMessageItem[]>([]);
const currentConversationId = ref<number | null>(props.conversationId);

const quickQuestions = [
  "今天有多少次报警？",
  "3号泳池的设备状态如何？",
  "当前值班救生员是谁？",
];

watch(
  () => props.conversationId,
  (newId) => {
    if (newId !== currentConversationId.value) {
      currentConversationId.value = newId;
      loadMessages();
    }
  },
);

onMounted(() => {
  loadMessages();
});

function renderMarkdown(content: string): string {
  if (!content) return "";
  const rawHtml = marked.parse(content) as string;
  return DOMPurify.sanitize(rawHtml);
}

async function loadMessages() {
  if (currentConversationId.value) {
    try {
      const msgs = await getMessages(currentConversationId.value);
      messages.value = msgs;
      scrollToBottom();
    } catch {
      messages.value = [];
    }
  } else {
    messages.value = [];
  }
}

async function handleSend() {
  const text = inputText.value.trim();
  if (!text || isLoading.value) return;

  const userMsg: AiChatMessageItem = {
    id: Date.now(),
    role: "user",
    content: text,
    createdAt: new Date().toISOString(),
  };
  messages.value.push(userMsg);
  inputText.value = "";
  scrollToBottom();

  isLoading.value = true;
  const assistantMsg: AiChatMessageItem = {
    id: Date.now() + 1,
    role: "assistant",
    content: "",
    createdAt: new Date().toISOString(),
  };
  messages.value.push(assistantMsg);

  try {
    await chatStream(
      currentConversationId.value,
      text,
      (token) => {
        assistantMsg.content += token;
        scrollToBottom();
      },
      () => {
        assistantMsg.content = "抱歉，AI服务暂时不可用，请稍后再试。";
        isLoading.value = false;
      },
      () => {
        isLoading.value = false;
        if (!currentConversationId.value) {
          initConversation();
        }
      },
    );
  } catch {
    assistantMsg.content = "抱歉，发送失败，请稍后再试。";
    isLoading.value = false;
  }

  scrollToBottom();
}

async function initConversation() {
  try {
    const conv = await createConversation({ title: "新对话" });
    currentConversationId.value = conv.id;
    emit("conversationCreated", conv.id);
  } catch {
    console.error("创建会话失败");
  }
}

function sendQuickQuestion(q: string) {
  inputText.value = q;
  handleSend();
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.close-btn {
  color: var(--color-text-secondary);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.welcome-section {
  text-align: center;
  padding: 24px 16px;
}

.welcome-text {
  font-size: 16px;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.welcome-subtext {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.message-item {
  margin-bottom: 16px;
}

.message-item.user {
  display: flex;
  justify-content: flex-end;
}

.message-item.assistant {
  display: flex;
  justify-content: flex-start;
}

.message-item.function {
  display: flex;
  justify-content: flex-start;
}

.message-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--color-bg-secondary);
}

.message-item.user .message-bubble {
  background: var(--color-primary);
  color: #fff;
}

.message-item.assistant .message-bubble {
  background: #f5f5f5;
  color: var(--color-text-primary);
}

.message-item.function .message-bubble {
  background: #e8f4ff;
  color: var(--color-text-primary);
}

.function-info {
  margin-bottom: 8px;
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-item.assistant .message-content {
  white-space: normal;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 8px;
  margin-bottom: 4px;
  font-weight: 600;
}

.markdown-body :deep(h1) {
  font-size: 18px;
}

.markdown-body :deep(h2) {
  font-size: 16px;
}

.markdown-body :deep(h3) {
  font-size: 14px;
}

.markdown-body :deep(p) {
  margin-bottom: 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin-bottom: 8px;
}

.markdown-body :deep(code) {
  background: #e8e8e8;
  padding: 2px 4px;
  border-radius: 3px;
  font-size: 13px;
  font-family: monospace;
}

.markdown-body :deep(pre) {
  background: #f0f0f0;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 8px;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  margin-bottom: 8px;
  width: 100%;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ddd;
  padding: 6px 10px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f5f5;
  font-weight: 600;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--color-primary);
  padding-left: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.markdown-body :deep(strong) {
  font-weight: 600;
}

.markdown-body :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.loading-dots {
  animation: blink 1s infinite;
}

.loading-dots span {
  animation: blink 1s infinite;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%, 50%, 100% {
    opacity: 1;
  }
  25%, 75% {
    opacity: 0;
  }
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
}

.chat-input .el-textarea {
  flex: 1;
}
</style>