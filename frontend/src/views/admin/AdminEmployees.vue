<template>
  <div class="admin-employees-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="page-header">
        <h1>管理员工</h1>
        <button class="add-btn" @click="showAddModal = true">
          <span class="btn-icon">➕</span>
          <span>添加员工</span>
        </button>
      </div>
      
      <div class="filter-bar">
        <div class="filter-group">
          <label>按部门筛选：</label>
          <select v-model="filterDepartment" @change="loadEmployees">
            <option value="">全部</option>
            <option v-for="department in departments" :key="department" :value="department">
              {{ department }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label>按状态筛选：</label>
          <select v-model="filterStatus" @change="loadEmployees">
            <option value="">全部</option>
            <option value="ACTIVE">在职</option>
            <option value="INACTIVE">离职</option>
          </select>
        </div>
      </div>
      
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>真实姓名</th>
              <th>部门</th>
              <th>职位</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="employee in employees" :key="employee.id">
              <td>{{ employee.id }}</td>
              <td>{{ employee.username }}</td>
              <td>{{ employee.realName || '-' }}</td>
              <td>{{ employee.department || '-' }}</td>
              <td>{{ employee.position || '-' }}</td>
              <td>
                <span :class="['status-badge', employee.status]">
                  {{ employee.status === 'ACTIVE' ? '在职' : '离职' }}
                </span>
              </td>
              <td>
                <div class="action-buttons">
                  <button class="edit-btn" @click="editEmployee(employee)">✏️</button>
                  <button class="delete-btn" @click="deleteEmployee(employee.id)">🗑️</button>
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
        <h2>{{ showAddModal ? '添加员工' : '编辑员工' }}</h2>
        <form @submit.prevent="saveEmployee">
          <div class="form-group">
            <label>需要登录管理平台</label>
            <input type="checkbox" v-model="formData.needsLogin" />
          </div>
          <div class="form-group" v-if="formData.needsLogin">
            <label>用户名</label>
            <input type="text" v-model="formData.username" :disabled="showEditModal" required />
          </div>
          <div class="form-group" v-if="formData.needsLogin && showAddModal">
            <label>密码</label>
            <input type="password" v-model="formData.password" required />
          </div>
          <div class="form-group" v-if="formData.needsLogin">
            <label>角色</label>
            <select v-model="formData.roleId">
              <option v-for="role in roles" :key="role.id" :value="role.id">
                {{ role.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>真实姓名</label>
            <input type="text" v-model="formData.realName" required />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input type="email" v-model="formData.email" required />
          </div>
          <div class="form-group">
            <label>手机号</label>
            <input type="tel" v-model="formData.phone" />
          </div>
          <div class="form-group">
            <label>部门</label>
            <input type="text" v-model="formData.department" />
          </div>
          <div class="form-group">
            <label>职位</label>
            <input type="text" v-model="formData.position" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="formData.status">
              <option value="ACTIVE">在职</option>
              <option value="INACTIVE">离职</option>
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
  name: 'AdminEmployees',
  components: {
    AdminNavBar
  },
  data() {
    return {
      employees: [],
      departments: [],
      roles: [],
      filterDepartment: '',
      filterStatus: '',
      showAddModal: false,
      showEditModal: false,
      formData: {
        id: null,
        needsLogin: false,
        username: '',
        password: '',
        roleId: 1,
        realName: '',
        email: '',
        phone: '',
        department: '',
        position: '',
        status: 'ACTIVE'
      }
    }
  },
  mounted() {
    this.loadDepartments()
    this.loadRoles()
    this.loadEmployees()
  },
  methods: {
    async loadDepartments() {
      try {
        const response = await apiClient.get('/api/employees/departments')
        this.departments = response.data
      } catch (error) {
        console.error('加载部门列表失败:', error)
      }
    },
    async loadRoles() {
      try {
        const response = await apiClient.get('/api/permissions/roles')
        this.roles = response.data
      } catch (error) {
        console.error('加载角色列表失败:', error)
      }
    },
    async loadEmployees() {
      try {
        let url = '/api/employees'
        
        if (this.filterDepartment) {
          url = `/api/employees/department/${this.filterDepartment}`
        } else if (this.filterStatus) {
          url = `/api/employees/status/${this.filterStatus}`
        }
        
        const response = await apiClient.get(url)
        this.employees = response.data
      } catch (error) {
        console.error('加载员工列表失败:', error)
        alert('加载员工列表失败，请稍后重试')
      }
    },

    editEmployee(employee) {
      this.formData = {
        id: employee.id,
        needsLogin: !!employee.username, // 有用户名则需要登录
        username: employee.username || '',
        roleId: employee.roleId || 1,
        realName: employee.realName || '',
        email: employee.email,
        phone: employee.phone || '',
        department: employee.department || '',
        position: employee.position || '',
        status: employee.status
      }
      this.showEditModal = true
    },
    async deleteEmployee(id) {
      if (confirm('确定要删除这个员工吗？')) {
        try {
          // 查找要删除的员工，判断是否需要登录
          const employee = this.employees.find(emp => emp.id === id)
          const endpoint = employee.username ? `/api/employees/${id}` : `/api/employees/non-login-employees/${id}`
          await apiClient.delete(endpoint)
          this.loadEmployees()
          alert('删除成功')
        } catch (error) {
          console.error('删除失败:', error)
          alert('删除失败，请稍后重试')
        }
      }
    },
    async saveEmployee() {
      try {
        if (this.showAddModal) {
          const endpoint = this.formData.needsLogin ? '/api/employees' : '/api/employees/non-login-employees'
          await apiClient.post(endpoint, this.formData)
          alert('添加成功')
        } else {
          // 编辑时，根据needsLogin决定调用哪个API
          if (this.formData.needsLogin) {
            await apiClient.put(`/api/employees/${this.formData.id}`, this.formData)
          } else {
            await apiClient.put(`/api/employees/non-login-employees/${this.formData.id}`, this.formData)
          }
          alert('更新成功')
        }
        this.closeModal()
        this.loadEmployees()
      } catch (error) {
        console.error('保存失败:', error)
        if (error.response && error.response.status === 400) {
          alert('用户名或邮箱已存在')
        } else {
          alert('保存失败，请稍后重试')
        }
      }
    },
    closeModal() {
      this.showAddModal = false
      this.showEditModal = false
      this.formData = {
        id: null,
        needsLogin: false,
        username: '',
        password: '',
        roleId: 1,
        realName: '',
        email: '',
        phone: '',
        department: '',
        position: '',
        status: 'ACTIVE'
      }
    }
  }
}
</script>

<style scoped>
.admin-employees-container {
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

.filter-bar {
  background: white;
  padding: 20px 30px;
  border-radius: 10px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 30px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group label {
  color: #555;
  font-weight: 500;
  white-space: nowrap;
}

.filter-group select {
  padding: 8px 15px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.filter-group select:focus {
  outline: none;
  border-color: #667eea;
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

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #e8f5e9;
  color: #4caf50;
}

.status-badge.INACTIVE {
  background: #ffebee;
  color: #f44336;
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
  max-width: 600px;
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
