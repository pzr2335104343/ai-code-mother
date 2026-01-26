<template>
  <div id="appChatPage">
    <!-- 应用信息栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button type="text" @click="showAppDetail" size="small">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button
          type="text"
          @click="downloadCode"
          :loading="downloading"
          :disabled="!isOwner"
          size="small"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying" size="small">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content" :class="{ 'preview-hidden': !showPreviewSection }">
      <!-- 左侧对话区域 -->
      <div class="chat-section" :class="{ 'chat-centered': !showPreviewSection }">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message animate-message-in">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message animate-message-in">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <!-- AI思考消息折叠框 -->
                <div v-if="message.thinkingContent" class="thinking-collapse-wrapper">
                  <div
                    class="thinking-collapse"
                    :class="{ 'thinking-expanded': message.thinkingExpanded }"
                  >
                    <div class="thinking-header" @click="handleThinkingCollapseChange(index)">
                      <div class="thinking-header-left">
                        <div class="thinking-icon">
                          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path
                              d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM12 20C7.59 20 4 16.41 4 12C4 7.59 7.59 4 12 4C16.41 4 20 7.59 20 12C20 16.41 16.41 20 12 20Z"
                              fill="currentColor"
                            />
                            <path
                              d="M12 6V12L16 14"
                              stroke="currentColor"
                              stroke-width="2"
                              stroke-linecap="round"
                            />
                          </svg>
                        </div>
                        <span class="thinking-title">AI 思考过程</span>
                        <span v-if="message.thinkingExpanded" class="thinking-badge animate-pulse"
                          >思考中</span
                        >
                      </div>
                      <div
                        class="thinking-arrow"
                        :class="{ 'arrow-expanded': message.thinkingExpanded }"
                      >
                        <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                          <path
                            d="M2 4L6 8L10 4"
                            stroke="currentColor"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                        </svg>
                      </div>
                    </div>
                    <div v-if="message.thinkingExpanded" class="thinking-content">
                      <div class="thinking-text">{{ message.thinkingContent }}</div>
                    </div>
                  </div>
                </div>
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <div class="loading-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
          v-if="selectedElementInfo"
          class="selected-element-alert"
          type="info"
          closable
          @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              :placeholder="getInputPlaceholder()"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="isGenerating ? stopGeneration() : sendMessage()"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined v-if="!isGenerating" />
                  <StopOutlined v-else />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section" :class="{ 'preview-hidden': !showPreviewSection }">
        <div class="preview-header">
          <div class="preview-title-wrapper">
            <div class="preview-icon">🌐</div>
            <h3>生成后的网页展示</h3>
          </div>
          <div class="preview-actions">
            <a-button
              v-if="isOwner && previewUrl"
              type="link"
              :danger="isEditMode"
              @click="toggleEditMode"
              :class="{ 'edit-mode-active': isEditMode }"
              style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <!-- 完成后显示的预览 -->
          <div v-if="previewUrl" class="preview-iframe-wrapper animate-preview-reveal">
            <iframe
              :src="previewUrl"
              class="preview-iframe"
              frameborder="0"
              @load="onIframeLoad"
            ></iframe>
          </div>
          <!-- 空状态占位 -->
          <div v-else class="preview-placeholder">
            <div class="placeholder-illustration">
              <div class="placeholder-glow"></div>
              <div class="placeholder-icon">🚀</div>
            </div>
            <p class="placeholder-title">开始你的创作之旅</p>
            <p class="placeholder-desc">描述你想要的网站，AI 将为你生成代码</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import {
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  getAppVoById,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { type ElementInfo, VisualEditor } from '@/utils/visualEditor'

import {
  CloudUploadOutlined,
  DownloadOutlined,
  EditOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  SendOutlined,
  StopOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  thinkingContent?: string
  thinkingExpanded?: boolean
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
let currentEventSource: EventSource | null = null
let currentAiMessageIndex: number | null = null

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)
const showPreviewSection = ref(false) // 控制预览区域的显示/隐藏

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
          .map((chat) => handleChatMessage(chat))
          .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
          // 恢复折叠框状态
          restoreThinkingCollapseState()
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

const handleChatMessage = (chat: API.ChatHistory) => {
  const obj = {
    type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
    content: chat.message || '',
    createTime: chat.createTime,
    thinkingContent: '',
    thinkingExpanded: false,
  }
  const msg = chat.message || ''
  const start = msg.indexOf(`<ai-thinking>`)
  const end = msg.lastIndexOf(`</ai-thinking>`)
  const endTagLen = `</ai-thinking>`.length
  if (start !== -1 && end !== -1 && end >= start) {
    obj.thinkingContent = msg
      .substring(start, end + endTagLen)
      .replace(/<\/ai-thinking>/g, '')
      .replace(/<ai-thinking>/g, '')
    obj.content = msg.substring(end + endTagLen) || ''
  }
  return obj
}
// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        showPreviewSection.value = true
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    thinkingContent: '',
    thinkingExpanded: true,
  })

  // 保存初始折叠框状态
  saveThinkingCollapseState()

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  currentAiMessageIndex = aiMessageIndex
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    thinkingContent: '',
    thinkingExpanded: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  currentAiMessageIndex = aiMessageIndex
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value != null ? String(appId.value) : '',
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    currentEventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''
    let thinkingContent = ''
    const THINKING_START = '<ai-thinking>'
    const THINKING_END = '</ai-thinking>'
    let pendingBuffer = ''
    let inThinking = false

    const flushToUI = () => {
      // 仅在对应区域有内容时更新，避免不必要的渲染抖动
      messages.value[aiMessageIndex].content = fullContent
      messages.value[aiMessageIndex].thinkingContent = thinkingContent
      messages.value[aiMessageIndex].loading = false
      scrollToBottom()
    }

    const processStreamChunk = (chunk: string) => {
      if (!chunk) return
      pendingBuffer += chunk

      // 循环处理：在 pendingBuffer 中不断找开始/结束标签并切换状态
      while (true) {
        if (!inThinking) {
          const startIdx = pendingBuffer.indexOf(THINKING_START)
          if (startIdx === -1) break

          // 开始标签前的内容属于正常回答
          if (startIdx > 0) {
            fullContent += pendingBuffer.slice(0, startIdx)
          }
          pendingBuffer = pendingBuffer.slice(startIdx + THINKING_START.length)
          inThinking = true
          continue
        } else {
          const endIdx = pendingBuffer.indexOf(THINKING_END)
          if (endIdx === -1) break

          // 结束标签前的内容属于思考过程
          if (endIdx > 0) {
            thinkingContent += pendingBuffer.slice(0, endIdx)
          }
          pendingBuffer = pendingBuffer.slice(endIdx + THINKING_END.length)
          inThinking = false
          continue
        }
      }

      // 处理剩余 buffer：只安全地 flush 不可能构成标签的部分，保留尾部以处理跨 chunk 标签拆分
      if (inThinking) {
        const keep = THINKING_END.length - 1
        if (pendingBuffer.length > keep) {
          const safePart = pendingBuffer.slice(0, pendingBuffer.length - keep)
          if (safePart) thinkingContent += safePart
          pendingBuffer = pendingBuffer.slice(pendingBuffer.length - keep)
        }
      } else {
        const keep = THINKING_START.length - 1
        if (pendingBuffer.length > keep) {
          const safePart = pendingBuffer.slice(0, pendingBuffer.length - keep)
          if (safePart) fullContent += safePart
          pendingBuffer = pendingBuffer.slice(pendingBuffer.length - keep)
        }
      }

      flushToUI()
    }

    // 处理接收到的消息
    if (!currentEventSource) return

    currentEventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容
        if (content !== undefined && content !== null) {
          processStreamChunk(String(content))
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    currentEventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false

      // 延迟显示预览区域，添加平滑过渡
      setTimeout(async () => {
        showPreviewSection.value = true
        // 只更新预览，不重新加载对话历史
        updatePreview()
      }, 300)

      currentEventSource?.close()
      currentEventSource = null
      currentAiMessageIndex = null
    })

    // 处理business-error事件（后端限流等错误）
    currentEventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        debugger
        currentEventSource?.close()
        currentEventSource = null
        currentAiMessageIndex = null
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理错误
    currentEventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (currentEventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        debugger
        currentEventSource?.close()
        currentEventSource = null
        currentAiMessageIndex = null

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  currentAiMessageIndex = null
}

// 手动停止生成
const stopGeneration = () => {
  if (!isGenerating.value) return
  isGenerating.value = false
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  if (currentAiMessageIndex !== null && messages.value[currentAiMessageIndex]) {
    messages.value[currentAiMessageIndex].loading = false
  }
  currentAiMessageIndex = null
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    previewUrl.value = getStaticPreviewUrl(codeGenType, String(appId.value))
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 保存折叠框状态到本地存储
const saveThinkingCollapseState = () => {
  try {
    const state = messages.value.map((msg) => ({
      contentHash: msg.content ? msg.content.substring(0, 100) : '', // 使用消息内容的前100个字符作为唯一标识
      thinkingExpanded: msg.thinkingExpanded,
    }))
    localStorage.setItem(`thinkingState_${appId.value}`, JSON.stringify(state))
  } catch (error) {
    console.error('保存折叠框状态失败:', error)
  }
}

// 从本地存储恢复折叠框状态
const restoreThinkingCollapseState = () => {
  try {
    const stateStr = localStorage.getItem(`thinkingState_${appId.value}`)
    if (stateStr) {
      const savedState: Array<{ contentHash: string; thinkingExpanded: boolean }> =
        JSON.parse(stateStr)
      messages.value.forEach((msg) => {
        const matchedState = savedState.find(
          (s) => s.contentHash === (msg.content ? msg.content.substring(0, 100) : ''),
        )
        if (matchedState !== undefined) {
          msg.thinkingExpanded = matchedState.thinkingExpanded
        } else if (msg.thinkingContent) {
          // 如果没有保存的状态但有思考内容，默认展开
          msg.thinkingExpanded = true
        }
      })
    } else {
      // 如果没有保存的状态，所有有思考内容的消息都默认展开
      messages.value.forEach((msg) => {
        if (msg.thinkingContent) {
          msg.thinkingExpanded = true
        }
      })
    }
  } catch (error) {
    console.error('恢复折叠框状态失败:', error)
    // 出错时，所有有思考内容的消息都默认展开
    messages.value.forEach((msg) => {
      if (msg.thinkingContent) {
        msg.thinkingExpanded = true
      }
    })
  }
}

// 处理AI思考折叠框状态变化
const handleThinkingCollapseChange = (messageIndex: number, keys?: string[]) => {
  if (keys !== undefined) {
    // 兼容旧版本调用
    messages.value[messageIndex].thinkingExpanded = keys.length > 0
  } else {
    // 新版本直接切换状态
    messages.value[messageIndex].thinkingExpanded = !messages.value[messageIndex].thinkingExpanded
  }
  saveThinkingCollapseState()
}

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: calc(100vh - 64px - 84px);
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: linear-gradient(135deg, #fafbfc 0%, #f5f7fa 40%, #f0f2f5 100%);
  background-size: 200% 200%;
  animation: pageBackgroundFloat 30s ease infinite;
  overflow: hidden;
  position: relative;
}

#appChatPage::before {
  content: '';
  position: absolute;
  top: -20%;
  left: -20%;
  width: 140%;
  height: 140%;
  background:
    radial-gradient(circle at 30% 30%, rgba(102, 126, 234, 0.04) 0%, transparent 50%),
    radial-gradient(circle at 70% 70%, rgba(118, 75, 162, 0.03) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, rgba(240, 147, 251, 0.02) 0%, transparent 60%);
  animation: backgroundGlow 20s ease-in-out infinite;
  pointer-events: none;
}

@keyframes pageBackgroundFloat {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes backgroundGlow {
  0%,
  100% {
    opacity: 0.6;
    transform: translate(0, 0) scale(1);
  }
  50% {
    opacity: 1;
    transform: translate(10px, -10px) scale(1.05);
  }
}

/* 应用信息栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  background: transparent;
  border: none;
  box-shadow: none;
  width: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.code-gen-type-tag {
  font-size: 11px;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.15);
  padding: 3px 10px;
  border-radius: 8px;
  color: #667eea;
  font-weight: 500;
}

.app-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.2px;
}

.header-right {
  display: flex;
  gap: 6px;
  align-items: center;
}

.header-right .ant-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 12px;
  height: 32px;
  font-size: 13px;
}

.header-right .ant-btn-text {
  color: #666;
}

.header-right .ant-btn-text:hover {
  color: #667eea;
  background: rgba(102, 126, 234, 0.06);
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 隐藏预览时的居中布局 */
.main-content.preview-hidden {
  justify-content: center;
}

/* 左侧对话区域 */
.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

/* 隐藏预览时居中且限宽 */
.chat-section.chat-centered {
  flex: 0 0 auto;
  width: 65%;
  max-width: 900px;
  min-width: 500px;
}

.messages-container {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(102, 126, 234, 0.2) transparent;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: rgba(102, 126, 234, 0.2);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: rgba(102, 126, 234, 0.3);
}

/* 隐藏预览时居中且限宽 */
.chat-section.chat-centered {
  flex: 0 0 auto;
  width: 65%;
  max-width: 900px;
  min-width: 500px;
}

.messages-container {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 20px;
}

.message-content {
  max-width: 75%;
  padding: 12px 18px;
  border-radius: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  transition: all 0.2s ease;
}

/* 用户消息 - 简洁清爽 */
.user-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
  border: none;
  font-weight: 400;
}

.user-message .message-content::before,
.user-message .message-content::after {
  display: none;
}

.user-message .message-content:hover {
  box-shadow: 0 3px 12px rgba(102, 126, 234, 0.2);
  transform: translateY(-1px);
}

.ai-message .message-content {
  background: #fafbfc;
  color: #1a1a1a;
  padding: 12px 16px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  border: 1px solid #e1e4e8;
}

.ai-message .message-content:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
}

/* 加载点动画 */
.loading-dots {
  display: flex;
  gap: 4px;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  background: #667eea;
  border-radius: 50%;
  animation: loadingDotPulse 1.4s ease-in-out infinite;
}

.loading-dots span:nth-child(1) {
  animation-delay: 0s;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes loadingDotPulse {
  0%,
  80%,
  100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1.1);
  }
}

#appChatPage::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background:
    radial-gradient(circle at 20% 80%, rgba(102, 126, 234, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(118, 75, 162, 0.06) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(240, 147, 251, 0.05) 0%, transparent 40%);
  animation: backgroundPulse 20s ease-in-out infinite;
  pointer-events: none;
}

@keyframes pageBackgroundFloat {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes backgroundPulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.1);
  }
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(102, 126, 234, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.9);
  position: relative;
  z-index: 10;
  transition: all 0.3s ease;
}

.header-bar:hover {
  box-shadow:
    0 12px 40px rgba(102, 126, 234, 0.15),
    0 4px 12px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
  border: 1px solid rgba(102, 126, 234, 0.2);
  padding: 4px 12px;
  border-radius: 12px;
  color: #667eea;
  font-weight: 500;
  transition: all 0.3s ease;
}

.code-gen-type-tag:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(118, 75, 162, 0.2));
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.3px;
  position: relative;
  display: inline-block;
}

.app-name::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #667eea, transparent);
  background-size: 200% 100%;
  animation: textUnderline 3s linear infinite;
}

@keyframes textUnderline {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 24px;
  padding: 20px 0;
  overflow: hidden;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 5;
}

/* 隐藏预览时的居中布局 */
.main-content.preview-hidden {
  justify-content: center;
}

/* 左侧对话区域 */
.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow:
    0 12px 48px rgba(102, 126, 234, 0.1),
    0 4px 16px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.9);
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.chat-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(102, 126, 234, 0.3),
    rgba(118, 75, 162, 0.3),
    transparent
  );
  animation: topBorderShimmer 4s linear infinite;
}

@keyframes topBorderShimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* 隐藏预览时居中且限宽 */
.chat-section.chat-centered {
  flex: 0 0 auto;
  width: 70%;
  max-width: 1000px;
  min-width: 600px;
}

.messages-container {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(102, 126, 234, 0.3) transparent;
}

.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.4), rgba(118, 75, 162, 0.4));
  border-radius: 4px;
  transition: all 0.3s ease;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.6), rgba(118, 75, 162, 0.6));
}

.message-item {
  margin-bottom: 24px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 12px;
}

.message-content {
  max-width: 72%;
  padding: 14px 20px;
  border-radius: 18px;
  line-height: 1.6;
  word-wrap: break-word;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

/* 用户消息 - 轻灵简洁样式 */
.user-message .message-content {
  background: linear-gradient(135deg, #e8ecff 0%, #e9ebf8 100%);
  color: #3b3b3b;
  box-shadow:
    0 2px 8px rgba(102, 126, 234, 0.1),
    0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.15);
  font-weight: 400;
}

.user-message .message-content::before,
.user-message .message-content::after {
  display: none;
}

.user-message .message-content:hover {
  background: linear-gradient(135deg, #dee3ff 0%, #e1e4f7 100%);
  box-shadow:
    0 3px 12px rgba(102, 126, 234, 0.12),
    0 1px 4px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.user-message .message-content::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at center, rgba(255, 255, 255, 0.15) 0%, transparent 60%);
  animation: shimmerEffect 4s ease-in-out infinite;
  pointer-events: none;
}

.user-message .message-content::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 50%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmerSlide 3s ease-in-out infinite;
  pointer-events: none;
}

.user-message .message-content:hover {
  transform: translateY(-4px) scale(1.03);
  box-shadow:
    0 12px 48px rgba(102, 126, 234, 0.5),
    0 4px 20px rgba(118, 75, 162, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.35),
    inset 0 -1px 0 rgba(0, 0, 0, 0.1);
}

@keyframes userGradientFlow {
  0%,
  100% {
    background-position: 0% 50%;
  }
  25% {
    background-position: 100% 50%;
  }
  50% {
    background-position: 100% 100%;
  }
  75% {
    background-position: 0% 100%;
  }
}

@keyframes shimmerEffect {
  0%,
  100% {
    transform: scale(1) translate(0, 0);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.2) translate(10px, -10px);
    opacity: 1;
  }
}

@keyframes shimmerSlide {
  0% {
    left: -100%;
  }
  50% {
    left: 100%;
  }
  100% {
    left: 100%;
  }
}

.ai-message .message-content {
  background: rgba(255, 255, 255, 0.95);
  color: #1a1a1a;
  padding: 12px 16px;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(102, 126, 234, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(102, 126, 234, 0.08);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.ai-message .message-content:hover {
  box-shadow:
    0 6px 24px rgba(0, 0, 0, 0.12),
    0 4px 12px rgba(102, 126, 234, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  border-color: rgba(102, 126, 234, 0.15);
  transform: translateY(-2px);
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #666;
  font-size: 14px;
}

/* 新增：加载点动画 */
.loading-dots {
  display: flex;
  gap: 4px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: var(--brand-primary);
  border-radius: 50%;
  animation: loadingDotPulse 1.4s ease-in-out infinite;
}

.loading-dots span:nth-child(1) {
  animation-delay: 0s;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes loadingDotPulse {
  0%,
  80%,
  100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1.1);
  }
}

/* ========== AI思考框轻灵样式 ========== */
.thinking-collapse-wrapper {
  margin-bottom: 12px;
}

.thinking-collapse {
  background: #f5f6f8;
  border: 1px solid #e8ecf0;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.thinking-collapse:hover {
  background: #f0f2f5;
  border-color: #dde0e8;
}

.thinking-expanded {
  background: #fafbfc;
  border-color: #d0d4e0;
}

.thinking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}

.thinking-header:hover {
  background: rgba(0, 0, 0, 0.02);
}

.thinking-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thinking-icon {
  width: 16px;
  height: 16px;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thinking-title {
  font-size: 13px;
  font-weight: 500;
  color: #666;
  letter-spacing: 0.2px;
}

.thinking-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: #667eea;
  color: white;
  border-radius: 10px;
  font-weight: 500;
}

.thinking-arrow {
  transition: transform 0.2s ease;
  color: #999;
}

.thinking-arrow.arrow-expanded {
  transform: rotate(180deg);
}

.thinking-content {
  padding: 12px 14px;
  border-top: 1px solid #e8ecf0;
  background: #fafbfc;
}

.thinking-text {
  font-size: 13px;
  line-height: 1.6;
  color: #666;
  white-space: pre-wrap;
  font-family: 'SF Mono', 'Monaco', 'Consolas', 'Menlo', monospace;
}

.thinking-collapse {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(102, 126, 234, 0.15);
  border-radius: 16px;
  overflow: hidden;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 4px 16px rgba(102, 126, 234, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  position: relative;
}

.thinking-collapse::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  background-size: 200% 100%;
  animation: topGradientShimmer 3s linear infinite;
  opacity: 0.8;
}

.thinking-collapse::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.8), transparent);
  background-size: 200% 100%;
  animation: topShimmer 2s linear infinite;
}

@keyframes topGradientShimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

@keyframes topShimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

.thinking-collapse:hover {
  box-shadow:
    0 8px 32px rgba(102, 126, 234, 0.15),
    0 4px 16px rgba(118, 75, 162, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  border-color: rgba(102, 126, 234, 0.25);
  transform: translateY(-2px);
}

.thinking-expanded {
  box-shadow:
    0 12px 48px rgba(102, 126, 234, 0.2),
    0 6px 24px rgba(118, 75, 162, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  border-color: rgba(102, 126, 234, 0.35);
}

.thinking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.06) 0%, rgba(118, 75, 162, 0.06) 100%);
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
}

.thinking-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
  transition: left 0.6s ease;
}

.thinking-header:hover::before {
  left: 100%;
}

.thinking-header:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
}

.thinking-header:active {
  transform: scale(0.98);
}

.thinking-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 2;
}

.thinking-icon {
  width: 22px;
  height: 22px;
  color: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.thinking-icon svg {
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.thinking-title {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
  letter-spacing: 0.4px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.thinking-badge {
  font-size: 11px;
  padding: 3px 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: 200% 200%;
  color: white;
  border-radius: 12px;
  font-weight: 500;
  animation: badgeGlow 2.5s ease-in-out infinite;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

@keyframes badgeGlow {
  0%,
  100% {
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
    background-position: 0% 50%;
  }
  50% {
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.5);
    background-position: 100% 50%;
  }
}

.thinking-arrow {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  color: #667eea;
  position: relative;
  z-index: 2;
}

.thinking-arrow svg {
  filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.3));
}

.thinking-arrow.arrow-expanded {
  transform: rotate(180deg);
}

.thinking-content {
  padding: 16px 18px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
  background: rgba(255, 255, 255, 0.6);
  animation: thinkingSlideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.thinking-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.2), transparent);
}

@keyframes thinkingSlideIn {
  from {
    opacity: 0;
    max-height: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    max-height: 800px;
    transform: translateY(0);
  }
}

.thinking-text {
  font-size: 13px;
  line-height: 1.7;
  color: #4a5568;
  white-space: pre-wrap;
  font-family: 'SF Mono', 'Monaco', 'Consolas', 'Menlo', monospace;
  position: relative;
  z-index: 1;
}

.thinking-text::selection {
  background: rgba(102, 126, 234, 0.2);
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

.load-more-container .ant-btn-link {
  color: #667eea;
  font-size: 13px;
  padding: 0;
}

.load-more-container .ant-btn-link:hover {
  color: #764ba2;
}

/* 输入区域 */
.input-container {
  padding: 14px 16px 16px;
  background: rgba(255, 255, 255, 0.5);
  border-top: 1px solid rgba(102, 126, 234, 0.06);
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
  padding: 12px 16px;
  border-radius: 14px;
  border: 1.5px solid rgba(102, 126, 234, 0.15);
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  font-size: 14px;
  line-height: 1.5;
}

.input-wrapper .ant-input:focus {
  border-color: #667eea;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.08);
}

.input-wrapper .ant-input:hover {
  border-color: rgba(102, 126, 234, 0.3);
}

.input-wrapper .ant-input::placeholder {
  color: #bbb;
}

.input-actions {
  position: absolute;
  bottom: 10px;
  right: 10px;
}

.input-actions button {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.2s ease;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.25);
}

.input-actions button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.input-actions button:active {
  transform: translateY(0);
}

.input-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(102, 126, 234, 0.2),
    rgba(118, 75, 162, 0.2),
    transparent
  );
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 60px;
  padding: 16px 20px;
  border-radius: 16px;
  border: 2px solid rgba(102, 126, 234, 0.15);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(255, 255, 255, 0.95);
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  font-size: 14px;
  line-height: 1.6;
}

.input-wrapper .ant-input:focus {
  border-color: #667eea;
  box-shadow:
    0 0 0 4px rgba(102, 126, 234, 0.12),
    0 4px 16px rgba(102, 126, 234, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  background: white;
  transform: translateY(-2px);
}

.input-wrapper .ant-input:hover {
  border-color: rgba(102, 126, 234, 0.3);
  box-shadow:
    0 6px 16px rgba(102, 126, 234, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.input-wrapper .ant-input::placeholder {
  color: #9ca3af;
  font-weight: 400;
}

.input-actions {
  position: absolute;
  bottom: 12px;
  right: 12px;
}

.input-actions button {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.input-actions button:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.input-actions button:active {
  transform: translateY(0) scale(0.98);
}

/* ========== 预览区域升级样式 ========== */
.preview-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

/* 预览区域隐藏时的样式 */
.preview-section.preview-hidden {
  flex: 0 0 0;
  padding: 0;
  margin: 0;
  border: none;
  box-shadow: none;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.06);
  background: rgba(255, 255, 255, 0.5);
}

.preview-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.preview-icon {
  font-size: 20px;
}

.preview-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.2px;
}

.preview-actions {
  display: flex;
  gap: 6px;
}

.preview-actions button {
  transition: all 0.2s ease;
}

.preview-actions button:hover {
  transform: translateY(-1px);
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fa 0%, #f5f7fa 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 预览iframe容器 */
.preview-iframe-wrapper {
  width: 100%;
  height: 100%;
  border-radius: 14px;
  overflow: hidden;
  background: white;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 占位符样式 */
.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  padding: 40px;
}

.placeholder-illustration {
  position: relative;
  margin-bottom: 20px;
}

.placeholder-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.12) 0%, transparent 60%);
  border-radius: 50%;
  animation: placeholderGlow 3s ease-in-out infinite;
}

@keyframes placeholderGlow {
  0%,
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 1;
  }
}

.placeholder-icon {
  position: relative;
  font-size: 56px;
  z-index: 1;
  animation: placeholderFloat 4s ease-in-out infinite;
}

@keyframes placeholderFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.placeholder-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.placeholder-desc {
  font-size: 13px;
  color: #999;
  margin: 0;
  line-height: 1.5;
}

/* 预览区域隐藏时的样式 */
.preview-section.preview-hidden {
  flex: 0 0 0;
  padding: 0;
  margin: 0;
  border: none;
  box-shadow: none;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #e8ecf0;
  background: rgba(255, 255, 255, 0.95);
}

.preview-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.preview-icon {
  font-size: 20px;
}

.preview-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.2px;
}

.preview-actions {
  display: flex;
  gap: 6px;
}

.preview-actions button {
  transition: all 0.2s ease;
}

.preview-actions button:hover {
  transform: translateY(-1px);
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #f8f9fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 预览iframe容器 */
.preview-iframe-wrapper {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 占位符样式 */
.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  padding: 40px;
}

.placeholder-illustration {
  position: relative;
  margin-bottom: 20px;
}

.placeholder-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.15) 0%, transparent 70%);
  border-radius: 50%;
}

.placeholder-icon {
  position: relative;
  font-size: 48px;
  z-index: 1;
}

.placeholder-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.placeholder-desc {
  font-size: 13px;
  color: #999;
  margin: 0;
  line-height: 1.5;
}

.preview-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  background-size: 200% 100%;
  animation: topGradientShimmer 4s linear infinite;
  opacity: 0.8;
}

/* 预览区域隐藏时的样式 */
.preview-section.preview-hidden {
  flex: 0 0 0;
  padding: 0;
  margin: 0;
  border: none;
  box-shadow: none;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transform: translateX(20px);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.08);
  background: linear-gradient(to right, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.98));
  position: relative;
  z-index: 2;
}

.preview-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-icon {
  font-size: 22px;
  animation: iconFloat 4s ease-in-out infinite;
  filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.2));
}

@keyframes iconFloat {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  25% {
    transform: translateY(-4px) rotate(5deg);
  }
  50% {
    transform: translateY(-2px) rotate(-3deg);
  }
  75% {
    transform: translateY(-5px) rotate(3deg);
  }
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.3px;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-actions button {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.preview-actions button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f8f9fc 0%, #f1f3f8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 预览iframe容器 */
.preview-iframe-wrapper {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  background: white;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(102, 126, 234, 0.06);
  animation: previewFadeIn 0.6s ease-out;
}

@keyframes previewFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 占位符样式 */
.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
  padding: 40px;
  position: relative;
}

.placeholder-illustration {
  position: relative;
  margin-bottom: 28px;
}

.placeholder-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 140px;
  height: 140px;
  background: radial-gradient(
    circle,
    rgba(102, 126, 234, 0.25) 0%,
    rgba(118, 75, 162, 0.15) 50%,
    rgba(240, 147, 251, 0.08) 70%,
    transparent 100%
  );
  border-radius: 50%;
  animation: glowPulse 4s ease-in-out infinite;
}

@keyframes glowPulse {
  0%,
  100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.8;
  }
  33% {
    transform: translate(-50%, -50%) scale(1.15);
    opacity: 1;
  }
  66% {
    transform: translate(-50%, -50%) scale(1.1);
    opacity: 0.9;
  }
}

.placeholder-icon {
  position: relative;
  font-size: 64px;
  z-index: 1;
  animation: rocketFloat 3s ease-in-out infinite;
  filter: drop-shadow(0 4px 8px rgba(102, 126, 234, 0.2));
}

@keyframes rocketFloat {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-12px) rotate(5deg);
  }
}

.placeholder-title {
  font-size: 19px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 10px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.5px;
}

.placeholder-desc {
  font-size: 14px;
  color: #888;
  margin: 0;
  line-height: 1.6;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 选中元素信息样式 */
.selected-element-info {
  line-height: 1.4;
  font-size: 13px;
}

.element-header {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.element-details {
  margin-top: 8px;
}

.element-item {
  margin-bottom: 4px;
  font-size: 13px;
  color: #666;
}

.element-item:last-child {
  margin-bottom: 0;
}

.element-tag {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.element-id {
  color: #52c41a;
  margin-left: 4px;
}

.element-class {
  color: #faad14;
  margin-left: 4px;
}

.element-selector-code {
  font-family: 'Monaco', 'Menlo', monospace;
  background: #f6f8fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #d73a49;
  border: 1px solid #e1e4e8;
}

/* 编辑模式按钮样式 */
.edit-mode-active {
  background-color: #52c41a !important;
  border-color: #52c41a !important;
  color: white !important;
}

.edit-mode-active:hover {
  background-color: #73d13d !important;
  border-color: #73d13d !important;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .chat-section.chat-centered {
    width: 75%;
  }
}

@media (max-width: 1024px) {
  #appChatPage {
    padding: 12px;
  }

  .main-content {
    flex-direction: column;
  }

  .chat-section.chat-centered {
    width: 100%;
    max-width: 100%;
  }

  .chat-section,
  .preview-section {
    flex: none;
  }

  .chat-section {
    height: 55vh;
  }

  .preview-section {
    height: 45vh;
  }
}

@media (max-width: 768px) {
  #appChatPage {
    padding: 8px;
  }

  .header-bar {
    padding: 12px 16px;
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-left {
    flex: 1;
    min-width: 0;
  }

  .app-name {
    font-size: 15px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .message-content {
    max-width: 85%;
  }

  .chat-section.chat-centered {
    min-width: auto;
  }
}

@media (max-width: 480px) {
  #appChatPage {
    padding: 6px;
  }

  .header-bar {
    padding: 10px 12px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .header-left {
    width: 100%;
  }

  .header-right {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .message-content {
    max-width: 90%;
  }

  .chat-section.chat-centered {
    min-width: auto;
  }
}
</style>
