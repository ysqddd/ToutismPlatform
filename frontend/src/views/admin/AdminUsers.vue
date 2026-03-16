<template>
  <div class="admin-users-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="page-header">
        <h1>管理用户</h1>
        <button class="add-btn" @click="showAddModal = true">
          <span class="btn-icon">➕</span>
          <span>添加用户</span>
        </button>
      </div>
      
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>是否管理员</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.email }}</td>
              <td>
                <span :class="['role-tag', user.role]">{{ user.role }}</span>
              </td>
              <td>
                <span :class="['status-badge', user.admin ? 'active' : 'inactive']">
                  {{ user.admin ? '是' : '否' }}
                </span>
              </td>
              <td>
                <div class="action-buttons">
                  <button class="edit-btn" @click="editUser(user)">✏️</button>
                  <button class="delete-btn" @click="deleteUser(user.id)">🗑️</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    
    <!-- 添加/编辑模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h2>{{ showAddModal ? '添加用户' : '编辑用户' }}</h2>
        <form @submit.prevent="saveUser">
          <div class="form-group">
            <label>用户名</label>
            <input type="text" v-model="formData.username" :disabled="showEditModal" required />
          </div>
          <div class="form-group" v-if="showAddModal">
            <label>密码</label>
            <input type="password" v-model="formData.password" required />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input type="email" v-model="formData.email" required />
          </div>
          <div class="form-group">
            <label>角色</label>
            <select v-model="formData.role">
              <option value="USER">普通用户</option>
              <option value="ADMIN">管理员</option>
            </select>
          </div>
          <div class="form-group">
            <label>是否管理员</label>
            <select v-model="formData.admin">
              <option :value="false">否</option>
              <option :value="true">是</option>
            </select>
          </div>
          <div class="modal-actions">
            <button type="button" class="cancel-btn" @click="closeModal">取消</button>
            <button type="submit" class="save-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import AdminNavBar from '../../components/AdminNavBar.vue'
import apiClient from '../../utils/axios'

export default {
  name: 'AdminUsers',
  components: {
    AdminNavBar
  },
  data() {
    return {
      users: [],
      showAddModal: false,
      showEditModal: false,
      formData: {
        id: null,
        username: '',
        password: '',
        email: '',
        role: 'USER',
        admin: false
      }
    }
  },
  mounted() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      try {
        const response = await apiClient.get('/api/users')
        this.users = response.data
      } catch (error) {
        console.error('加载用户列表失败:', error)
        alert('加载用户列表失败，请稍后重试')
      }
    },
    editUser(user) {
      this.formData = {
        id: user.id,
        username: user.username,
        password: '',
        email: user.email,
        role: user.role,
        admin: user.admin
      }
      this.showEditModal = true
    },
    async deleteUser(id) {
      if (confirm('确定要删除这个用户吗？')) {
        try {
          await apiClient.delete(`/api/users/${id}`)
          this.loadUsers()
          alert('删除成功')
        } catch (error) {
          console.error('删除失败:', error)
          alert('删除失败，请稍后重试')
        }
      }
    },
    async saveUser() {
      try {
        if (this.showAddModal) {
          await apiClient.post('/api/users', this.formData)
          alert('添加成功')
        } else {
          await apiClient.put(`/api/users/${this.formData.id}`, this.formData)
          alert('更新成功')
        }
        this.closeModal()
        this.loadUsers()
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败，请稍后重试')
      }
    },
    closeModal() {
      this.showAddModal = false
      this.showEditModal = false
      this.formData = {
        id: null,
        username: '',
        password: '',
        email: '',
        role: 'USER',
        admin: false
      }
    }
  }
}
</script>

<style scoped>
.admin-users-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.admin-content {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 20px 30px;
  border-radius: 10px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.page-header h1 {
  color: #333;
  font-size: 24px;
  margin: 0;
}

.add-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  transition: transform 0.2s;
}

.add-btn:hover {
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 16px;
}

.table-container {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: #f8f9fa;
}

.data-table th {
  padding: 15px;
  text-align: left;
  color: #555;
  font-weight: 600;
  border-bottom: 2px solid #e9ecef;
}

.data-table td {
  padding: 15px;
  border-bottom: 1px solid #e9ecef;
}

.role-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #e3f2fd;
  color: #2196f3;
}

.role-tag.ADMIN {
  background: #f3e5f5;
  color: #9c27b0;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #ffebee;
  color: #f44336;
}

.status-badge.active {
  background: #e8f5e9;
  color: #4caf50;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.edit-btn, .delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: transform 0.2s;
}

.edit-btn:hover, .delete-btn:hover {
  transform: scale(1.1);
}

.delete-btn {
  background: #ffebee;
}

.edit-btn {
  background: #e3f2fd;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 10px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content h2 {
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.cancel-btn, .save-btn {
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
}

.save-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}
</style>
