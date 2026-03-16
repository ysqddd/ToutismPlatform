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
          @keyup.enter="sendMessage"
          type="text" 
          placeholder="请输入您的问题..."
          class="chat-input"
          :disabled="isLoading"
        />
        <button @click="sendMessage" class="send-btn" :disabled="isLoading">
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
import axios from 'axios'

export default {
  name: 'Assistant',
  data() {
    return {
      inputMessage: '',
      messages: [
        { type: 'bot', text: '您好！我是旅游平台智能小助手，有什么问题可以问我哦~' }
      ],
      quickQuestions: [
        '如何自定义旅游方案',
        '退改政策是什么？',
        '如何联系客服？',
        '有哪些优惠活动？'
      ],
      isLoading: false
    }
  },
  methods: {
    async sendMessage() {
      if (this.inputMessage.trim()) {
        this.messages.push({
          type: 'user',
          text: this.inputMessage
        })
        
        this.isLoading = true
        
        try {
          // 调用后端 RAG API
          const response = await axios.post('/api/rag/generate', {
            query: this.inputMessage
          })
          
          // 显示机器人回复
          this.messages.push({
            type: 'bot',
            text: response.data.answer
          })
        } catch (error) {
          console.error('调用 RAG API 失败:', error)
          this.messages.push({
            type: 'bot',
            text: '抱歉，服务暂时不可用，请稍后再试。'
          })
        } finally {
          this.isLoading = false
        }
        
        this.inputMessage = ''
        this.scrollToBottom()
      }
    },
    askQuestion(question) {
      this.inputMessage = question
      this.sendMessage()
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
