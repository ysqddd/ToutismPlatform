<template>
  <div class="admin-permissions-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="page-header">
        <h1>管理权限</h1>
        <div class="header-actions">
          <button class="add-btn" @click="showAddModal = true">
            <span class="btn-icon">➕</span>
            <span>添加权限</span>
          </button>
          <button class="add-btn" @click="showAddRoleModal = true">
            <span class="btn-icon">👥</span>
            <span>添加角色</span>
          </button>
        </div>
      </div>
      
      <div class="tabs">
        <button :class="{ active: activeTab === 'permissions' }" @click="activeTab = 'permissions'">权限管理</button>
        <button :class="{ active: activeTab === 'roles' }" @click="activeTab = 'roles'">角色管理</button>
      </div>
      
      <!-- 权限管理 -->
      <div v-if="activeTab === 'permissions'" class="permissions-grid">
        <div v-for="permission in permissions" :key="permission.id" class="permission-card">
          <div class="permission-header">
            <h3>{{ permission.name }}</h3>
            <span class="user-info">{{ permission.code }}</span>
          </div>
          <p class="permission-desc">{{ permission.description }}</p>
          <div class="permission-info">
            <div class="info-item">
              <span class="label">模块:</span>
              <span class="value">{{ permission.module }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间:</span>
              <span class="value">{{ permission.createdAt }}</span>
            </div>
            <div class="info-item">
              <span class="label">更新时间:</span>
              <span class="value">{{ permission.updatedAt }}</span>
            </div>
          </div>
          <div class="permission-actions">
            <button class="action-btn edit" @click="editPermission(permission)">编辑</button>
            <button class="action-btn delete" @click="deletePermission(permission.id)">删除</button>
          </div>
        </div>
      </div>
      
      <!-- 角色管理 -->
      <div v-if="activeTab === 'roles'" class="roles-grid">
        <div v-for="role in roles" :key="role.id" class="role-card">
          <div class="role-header">
            <h3>{{ role.name }}</h3>
          </div>
          <p class="role-desc">{{ role.description }}</p>
          <div class="role-actions">
            <button class="action-btn edit" @click="editRole(role)">编辑</button>
            <button class="action-btn assign" @click="showAssignPermissionsModal(role)">分配权限</button>
            <button class="action-btn delete" @click="deleteRole(role.id)">删除</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 添加/编辑权限模态框 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h2>{{ showAddModal ? '添加权限' : '编辑权限' }}</h2>
        <form @submit.prevent="savePermission">
          <div class="form-group">
            <label>权限编码</label>
            <input v-model="formData.code" type="text" required />
          </div>
          <div class="form-group">
            <label>权限名称</label>
            <input v-model="formData.name" type="text" required />
          </div>
          <div class="form-group">
            <label>权限描述</label>
            <textarea v-model="formData.description" required rows="4"></textarea>
          </div>
          <div class="form-group">
            <label>所属模块</label>
            <input v-model="formData.module" type="text" required />
          </div>
          <div class="modal-actions">
            <button type="button" class="cancel-btn" @click="closeModal">取消</button>
            <button type="submit" class="save-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 添加/编辑角色模态框 -->
    <div v-if="showAddRoleModal || showEditRoleModal" class="modal-overlay" @click="closeRoleModal">
      <div class="modal-content" @click.stop>
        <h2>{{ showAddRoleModal ? '添加角色' : '编辑角色' }}</h2>
        <form @submit.prevent="saveRole">
          <div class="form-group">
            <label>角色名称</label>
            <input v-model="roleFormData.name" type="text" required />
          </div>
          <div class="form-group">
            <label>角色描述</label>
            <textarea v-model="roleFormData.description" required rows="4"></textarea>
          </div>
          <div class="modal-actions">
            <button type="button" class="cancel-btn" @click="closeRoleModal">取消</button>
            <button type="submit" class="save-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 分配权限模态框 -->
    <div v-if="showAssignPermissionsModalFlag" class="modal-overlay" @click="closeAssignPermissionsModal">
      <div class="modal-content" @click.stop style="max-width: 700px;">
        <h2>为角色分配权限 - {{ selectedRole.name }}</h2>
        <form @submit.prevent="assignPermissions">
          <div class="form-group">
            <label>选择权限</label>
            <div class="checkbox-group">
              <label v-for="permission in permissions" :key="permission.id" class="checkbox-label">
                <input type="checkbox" v-model="selectedPermissions" :value="permission.id" />
                <span>{{ permission.name }} ({{ permission.code }})</span>
              </label>
            </div>
          </div>
          <div class="modal-actions">
            <button type="button" class="cancel-btn" @click="closeAssignPermissionsModal">取消</button>
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
  name: 'AdminPermissions',
  components: {
    AdminNavBar
  },
  data() {
    return {
      activeTab: 'permissions',
      permissions: [],
      roles: [],
      showAddModal: false,
      showEditModal: false,
      showAddRoleModal: false,
      showEditRoleModal: false,
      showAssignPermissionsModalFlag: false,
      isSubmitting: false,
      formData: {
        code: '',
        name: '',
        description: '',
        module: ''
      },
      roleFormData: {
        name: '',
        description: ''
      },
      selectedRole: null,
      selectedPermissions: []
    }
  },
  mounted() {
    this.loadPermissions()
    this.loadRoles()
  },
  methods: {
    async loadPermissions() {
      try {
        const response = await apiClient.get('/api/permissions')
        this.permissions = response.data
      } catch (error) {
        console.error('加载权限列表失败:', error)
        alert('加载权限列表失败，请稍后重试')
      }
    },
    async loadRoles() {
      try {
        const response = await apiClient.get('/api/permissions/roles')
        this.roles = response.data
      } catch (error) {
        console.error('加载角色列表失败:', error)
        alert('加载角色列表失败，请稍后重试')
      }
    },
    editPermission(permission) {
      this.formData = {
        code: permission.code,
        name: permission.name,
        description: permission.description,
        module: permission.module
      }
      this.showEditModal = true
    },
    async savePermission() {
      try {
        // 防止重复提交
        if (this.isSubmitting) return
        this.isSubmitting = true
        
        if (this.showAddModal) {
          await apiClient.post('/api/permissions', this.formData)
          alert('添加成功')
        } else {
          // 后端API支持更新权限
          await apiClient.put(`/api/permissions/${this.formData.id}`, this.formData)
          alert('更新成功')
        }
        this.closeModal()
        this.loadPermissions()
      } catch (error) {
        console.error('保存权限失败:', error)
        alert('保存权限失败，请稍后重试')
      } finally {
        this.isSubmitting = false
      }
    },
    async deletePermission(id) {
      if (confirm('确定要删除这个权限吗？')) {
        try {
          await apiClient.delete(`/api/permissions/${id}`)
          this.loadPermissions()
          alert('删除成功')
        } catch (error) {
          console.error('删除权限失败:', error)
          alert('删除权限失败，请稍后重试')
        }
      }
    },
    closeModal() {
      this.showAddModal = false
      this.showEditModal = false
      this.formData = {
        code: '',
        name: '',
        description: '',
        module: ''
      }
    },
    editRole(role) {
      this.roleFormData = {
        name: role.name,
        description: role.description
      }
      this.showEditRoleModal = true
    },
    async saveRole() {
      try {
        // 防止重复提交
        if (this.isSubmitting) return
        this.isSubmitting = true
        
        if (this.showAddRoleModal) {
          await apiClient.post('/api/permissions/roles', this.roleFormData)
          alert('添加成功')
        } else {
          // 后端API支持更新角色
          await apiClient.put(`/api/permissions/roles/${this.roleFormData.id}`, this.roleFormData)
          alert('更新成功')
        }
        this.closeRoleModal()
        this.loadRoles()
      } catch (error) {
        console.error('保存角色失败:', error)
        alert('保存角色失败，请稍后重试')
      } finally {
        this.isSubmitting = false
      }
    },
    async deleteRole(id) {
      if (confirm('确定要删除这个角色吗？')) {
        try {
          await apiClient.delete(`/api/permissions/roles/${id}`)
          this.loadRoles()
          alert('删除成功')
        } catch (error) {
          console.error('删除角色失败:', error)
          alert('删除角色失败，请稍后重试')
        }
      }
    },
    closeRoleModal() {
      this.showAddRoleModal = false
      this.showEditRoleModal = false
      this.roleFormData = {
        name: '',
        description: ''
      }
    },
    async showAssignPermissionsModal(role) {
      this.selectedRole = role
      // 加载角色当前的权限
      try {
        const response = await apiClient.get(`/api/permissions/roles/${role.id}/permissions`)
        this.selectedPermissions = response.data.map(permission => permission.id)
      } catch (error) {
        console.error('加载角色权限失败:', error)
        this.selectedPermissions = []
      }
      this.showAssignPermissionsModalFlag = true
    },
    async assignPermissions() {
      try {
        // 防止重复提交
        if (this.isSubmitting) return
        this.isSubmitting = true
        
        await apiClient.post(`/api/permissions/roles/${this.selectedRole.id}/permissions`, {
          permissionIds: this.selectedPermissions
        })
        alert('权限分配成功')
        this.closeAssignPermissionsModal()
      } catch (error) {
        console.error('分配权限失败:', error)
        alert('分配权限失败，请稍后重试')
      } finally {
        this.isSubmitting = false
      }
    },
    closeAssignPermissionsModal() {
      this.showAssignPermissionsModalFlag = false
      this.selectedRole = null
      this.selectedPermissions = []
    }
  }
}
</script>

<style scoped>
.admin-permissions-container {
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

.header-actions {
  display: flex;
  gap: 10px;
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

.tabs {
  display: flex;
  background: white;
  border-radius: 10px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.tabs button {
  flex: 1;
  padding: 15px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  color: #666;
  transition: all 0.3s;
}

.tabs button:hover {
  background: #f0f0f0;
}

.tabs button.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.permissions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.permission-card {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.permission-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.permission-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 15px;
}

.permission-header h3 {
  color: #333;
  font-size: 18px;
  margin: 0 0 5px 0;
}

.user-info {
  font-size: 12px;
  color: #666;
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.permission-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.6;
}

.permission-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-item .label {
  color: #666;
  font-weight: 500;
}

.info-item .value {
  color: #333;
  font-weight: 600;
}

.permission-actions {
  display: flex;
  gap: 10px;
}

.roles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.role-card {
  background: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.role-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.role-header {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 15px;
}

.role-header h3 {
  color: #333;
  font-size: 18px;
  margin: 0 0 5px 0;
}

.role-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  line-height: 1.6;
}

.role-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  background: #f0f0f0;
  color: #333;
}

.action-btn:hover {
  transform: translateY(-2px);
}

.action-btn.edit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-btn.assign {
  background: #4CAF50;
  color: white;
}

.action-btn.delete {
  background: #f44336;
  color: white;
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
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  font-family: inherit;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  transition: background 0.2s;
}

.checkbox-label:hover {
  background: #f0f0f0;
}

.checkbox-label input[type="checkbox"] {
  width: auto;
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
