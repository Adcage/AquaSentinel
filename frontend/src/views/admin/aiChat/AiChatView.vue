<template>
  <div class="ai-chat-view">
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" size="small" @click="createNewConversation">
          新建对话
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conversation-item', { active: conv.id === currentConversationId }]"
          @click="selectConversation(conv.id)"
        >
          <span class="conv-title">{{ conv.title || "新对话" }}</span>
          <el-button
            text
            size="small"
            class="delete-btn"
            @click.stop="deleteConversation(conv.id)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div class="main-area">
      <AiChatPanel
        :title="currentConversation?.title || 'AI 助手'"
        :conversation-id="currentConversationId"
        @conversation-created="onConversationCreated"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Delete } from "@element-plus/icons-vue";
import { ref, onMounted } from "vue";
import AiChatPanel from "@/components/business/AiChatPanel.vue";
import type { AiConversation } from "@/types/business";
import { listConversations, createConversation, deleteConversation as deleteConv } from "@/services/aiChatService";
import { ElMessage } from "element-plus";

const conversations = ref<AiConversation[]>([]);
const currentConversationId = ref<number | null>(null);
const currentConversation = ref<AiConversation | null>(null);

onMounted(() => {
  loadConversations();
});

async function loadConversations() {
  try {
    conversations.value = await listConversations();
    if (conversations.value.length > 0 && !currentConversationId.value) {
      selectConversation(conversations.value[0].id);
    }
  } catch {
    ElMessage.error("加载会话列表失败");
  }
}

function selectConversation(id: number) {
  currentConversationId.value = id;
  currentConversation.value = conversations.value.find((c) => c.id === id) || null;
}

async function createNewConversation() {
  try {
    const conv = await createConversation({ title: "新对话" });
    conversations.value.unshift(conv);
    selectConversation(conv.id);
    ElMessage.success("新建对话成功");
  } catch {
    ElMessage.error("新建对话失败");
  }
}

async function deleteConversation(id: number) {
  try {
    await deleteConv(id);
    conversations.value = conversations.value.filter((c) => c.id !== id);
    if (currentConversationId.value === id) {
      if (conversations.value.length > 0) {
        selectConversation(conversations.value[0].id);
      } else {
        currentConversationId.value = null;
        currentConversation.value = null;
      }
    }
    ElMessage.success("删除成功");
  } catch {
    ElMessage.error("删除失败");
  }
}

function onConversationCreated(id: number) {
  if (!currentConversationId.value) {
    currentConversationId.value = id;
    loadConversations();
  }
}
</script>

<style scoped>
.ai-chat-view {
  display: flex;
  height: 100%;
  background: var(--color-bg-page);
}

.sidebar {
  width: 260px;
  background: #fff;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.conversation-item:hover {
  background: var(--color-bg-secondary);
}

.conversation-item.active {
  background: var(--color-primary-light);
}

.conv-title {
  font-size: 14px;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>