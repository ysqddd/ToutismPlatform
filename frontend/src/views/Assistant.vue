<template>
  <div class="assistant-container">
    <h2>🤖 智能小助手</h2>

    <div class="chat-container">
      <div class="chat-messages" ref="messagesContainer">
        <div
            v-for="(message, index) in messages"
            :key="index"
            :class="['message', message.type]"
        >
          <div class="message-avatar">
            {{ message.type === 'user' ? '👤' : '🤖' }}
          </div>
          <div class="message-content">
            {{ message.text }}
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <input
            v-model="inputMessage"
            @keyup.enter="sendMessage()"
            type="text"
            placeholder="请输入您的问题..."
            class="chat-input"
            :disabled="isLoading"
        />
        <button @click="sendMessage()" class="send-btn" :disabled="isLoading">
          {{ isLoading ? '发送中...' : '发送' }}
        </button>
      </div>
    </div>

    <div class="quick-questions">
      <h3>常见问题：</h3>
      <button
          v-for="(question, index) in quickQuestions"
          :key="index"
          @click="askQuestion(question)"
          class="question-btn"
      >
        {{ question }}
      </button>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

const ASSISTANT_CHAT_CACHE_KEY = 'assistant_chat_state_v1'
const ASSISTANT_CHAT_USER_CACHE_PREFIX = 'assistant_chat_state_v1:'
const DEFAULT_WELCOME_MESSAGES = [
  { type: 'bot', text: '您好！我是旅游平台智能小助手，有什么问题可以问我哦~' }
]

export default {
  name: 'Assistant',
  data() {
    return {
      inputMessage: '',
      messages: [...DEFAULT_WELCOME_MESSAGES],
      quickQuestions: [
        '有哪些好玩的地方',
        '有哪些知名景点',
        '有哪些好吃的地方',
        '有哪些优惠套餐'
      ],
      isLoading: false,
      userId: null,
      username: ''
    }
  },
  async created() {
    this.restoreChatState()
    await this.loadUserInfo()
    this.restoreChatState()
    this.persistChatState()
  },
  mounted() {
    this.scrollToBottom()
  },
  activated() {
    this.restoreChatState()
    this.scrollToBottom()
  },
  watch: {
    messages: {
      deep: true,
      handler() {
        this.persistChatState()
        this.scrollToBottom()
      }
    },
    inputMessage() {
      this.persistChatState()
    },
    userId() {
      this.persistChatState()
    },
    username() {
      this.persistChatState()
    }
  },
  methods: {
    getCurrentChatCacheKey() {
      if (this.userId != null && !Number.isNaN(this.userId)) {
        return `${ASSISTANT_CHAT_USER_CACHE_PREFIX}user_${this.userId}`
      }
      if (this.username) {
        return `${ASSISTANT_CHAT_USER_CACHE_PREFIX}name_${this.username}`
      }
      return ASSISTANT_CHAT_CACHE_KEY
    },

    normalizeMessages(messages) {
      if (!Array.isArray(messages)) {
        return [...DEFAULT_WELCOME_MESSAGES]
      }

      const normalized = messages
          .filter(item => item && (item.type === 'user' || item.type === 'bot'))
          .map(item => ({
            type: item.type,
            text: String(item.text || '').trim()
          }))
          .filter(item => item.text)

      return normalized.length > 0 ? normalized : [...DEFAULT_WELCOME_MESSAGES]
    },

    readChatStateByKey(key) {
      if (!key) {
        return null
      }

      try {
        const raw = localStorage.getItem(key)
        if (!raw) {
          return null
        }
        const parsed = JSON.parse(raw)
        return {
          inputMessage: typeof parsed.inputMessage === 'string' ? parsed.inputMessage : '',
          messages: this.normalizeMessages(parsed.messages),
          updatedAt: Number(parsed.updatedAt || 0)
        }
      } catch (error) {
        console.error('读取助手聊天缓存失败:', error)
        return null
      }
    },

    restoreChatState() {
      const userState = this.readChatStateByKey(this.getCurrentChatCacheKey())
      const commonState = this.readChatStateByKey(ASSISTANT_CHAT_CACHE_KEY)

      let stateToUse = null
      if (userState && commonState) {
        stateToUse = userState.updatedAt >= commonState.updatedAt ? userState : commonState
      } else {
        stateToUse = userState || commonState
      }

      if (!stateToUse) {
        this.messages = [...DEFAULT_WELCOME_MESSAGES]
        this.inputMessage = ''
        return
      }

      this.messages = this.normalizeMessages(stateToUse.messages)
      this.inputMessage = stateToUse.inputMessage || ''
    },

    persistChatState() {
      const payload = JSON.stringify({
        inputMessage: this.inputMessage,
        messages: this.normalizeMessages(this.messages),
        updatedAt: Date.now()
      })

      try {
        localStorage.setItem(ASSISTANT_CHAT_CACHE_KEY, payload)
        localStorage.setItem(this.getCurrentChatCacheKey(), payload)
      } catch (error) {
        console.error('保存助手聊天缓存失败:', error)
      }
    },

    clearChatState() {
      this.messages = [...DEFAULT_WELCOME_MESSAGES]
      this.inputMessage = ''
      try {
        localStorage.removeItem(ASSISTANT_CHAT_CACHE_KEY)
        localStorage.removeItem(this.getCurrentChatCacheKey())
      } catch (error) {
        console.error('清理助手聊天缓存失败:', error)
      }
    },

    async loadUserInfo() {
      const savedUserId = localStorage.getItem('userId')
      const savedUsername = localStorage.getItem('username')

      if (savedUserId) {
        this.userId = Number(savedUserId)
      }
      if (savedUsername) {
        this.username = savedUsername
      }

      try {
        const response = await apiClient.get('/api/auth/current-user')
        if (response && response.data) {
          this.userId = response.data.id != null ? Number(response.data.id) : this.userId
          this.username = response.data.username || this.username || ''

          if (this.userId != null && !Number.isNaN(this.userId)) {
            localStorage.setItem('userId', String(this.userId))
          }
          if (this.username) {
            localStorage.setItem('username', this.username)
          }
        }
      } catch (error) {
        console.error('获取当前用户信息失败，已尝试使用本地缓存:', error)
      }
    },

    buildRagPayload(query) {
      const payload = { query }

      if (this.userId != null && !Number.isNaN(this.userId)) {
        payload.userId = this.userId
      }
      if (this.username) {
        payload.username = this.username
      }

      return payload
    },

    isCartActionMessage(text) {
      const normalized = String(text || '').replace(/\s+/g, '')
      return [
        '加入购物车',
        '放入购物车',
        '加到购物车',
        '将你方案放入购物车',
        '将方案放入购物车',
        '帮我加入购物车',
        '帮我放入购物车'
      ].some(keyword => normalized.includes(keyword))
    },

    isCartUpdatedReply(text) {
      const normalized = String(text || '').replace(/\s+/g, '')
      return (
          normalized.includes('购物车') &&
          (
              normalized.includes('已按') ||
              normalized.includes('已将') ||
              normalized.includes('加入成功') ||
              normalized.includes('原本就在购物车中') ||
              normalized.includes('无需重复加入')
          )
      )
    },

    async sendMessage(customQuestion = null) {
      const isDirectQuestion = typeof customQuestion === 'string'
      const rawQuestion = isDirectQuestion ? customQuestion : this.inputMessage
      const question = String(rawQuestion || '').trim()
      if (!question || this.isLoading) return

      this.messages.push({
        type: 'user',
        text: question
      })

      if (!isDirectQuestion) {
        this.inputMessage = ''
      }

      this.isLoading = true
      this.persistChatState()
      this.scrollToBottom()

      try {
        if (this.userId == null || Number.isNaN(this.userId) || !this.username) {
          await this.loadUserInfo()
        }

        const payload = this.buildRagPayload(question)
        const response = await apiClient.post('/api/rag/generate', payload)

        const answer = response && response.data && response.data.answer
            ? response.data.answer
            : '抱歉，暂时没有获取到有效回复。'

        this.messages.push({
          type: 'bot',
          text: answer
        })

        if (this.isCartUpdatedReply(answer) || this.isCartActionMessage(question)) {
          localStorage.setItem('cartRefreshAt', String(Date.now()))
        }
      } catch (error) {
        console.error('调用 RAG API 失败:', error)
        this.messages.push({
          type: 'bot',
          text: '抱歉，服务暂时不可用，请稍后再试。'
        })
      } finally {
        this.isLoading = false
        this.persistChatState()
        this.scrollToBottom()
      }
    },

    askQuestion(question) {
      this.sendMessage(question)
    },

    scrollToBottom() {
      this.$nextTick(() => {
        if (this.$refs.messagesContainer) {
          this.$refs.messagesContainer.scrollTop = this.$refs.messagesContainer.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/assistant.css';
</style>
