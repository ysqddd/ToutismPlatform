<template>
  <div class="admin-path-management-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="page-header">
        <h1>路径管理</h1>
      </div>
      
      <div class="action-bar">
        <button class="add-btn" @click="showAddEdgeModal = true">
          <span class="btn-icon">➕</span>
          <span>添加路径</span>
        </button>
      </div>
      
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>起点景区</th>
              <th>终点景区</th>
              <th>距离 (米)</th>
              <th>预计时间 (分钟)</th>
              <th>路径描述</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="edge in edges" :key="edge.id">
              <td>{{ edge.id }}</td>
              <td>{{ getAreaName(edge.fromAreaId) }}</td>
              <td>{{ getAreaName(edge.toAreaId) }}</td>
              <td>{{ edge.distance }}</td>
              <td>{{ edge.duration }}</td>
              <td>{{ edge.description || '-' }}</td>
              <td>
                <div class="action-buttons">
                  <button class="edit-btn" @click="editEdge(edge)">✏️</button>
                  <button class="delete-btn" @click="deleteEdge(edge.id)">🗑️</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    
    <!-- 添加/编辑路径模态框 -->
    <div v-if="showAddEdgeModal || showEditEdgeModal" class="modal-overlay" @click="closeEdgeModal">
      <div class="modal-content" @click.stop>
        <h2>{{ showAddEdgeModal ? '添加路径' : '编辑路径' }}</h2>
        <form @submit.prevent="saveEdge">
          <div class="form-group">
            <label>起点景区</label>
            <select v-model="edgeFormData.fromAreaId" required>
              <option value="">请选择起点景区</option>
              <option v-for="area in scenicAreas" :key="area.id" :value="area.id">
                {{ area.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>终点景区</label>
            <select v-model="edgeFormData.toAreaId" required>
              <option value="">请选择终点景区</option>
              <option v-for="area in scenicAreas" :key="area.id" :value="area.id">
                {{ area.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>距离 (米)</label>
            <input type="number" v-model="edgeFormData.distance" step="0.1" required />
          </div>
          <div class="form-group">
            <label>预计时间 (分钟)</label>
            <input type="number" v-model="edgeFormData.duration" required />
          </div>
          <div class="form-group">
            <label>路径描述</label>
            <textarea v-model="edgeFormData.description" rows="3" placeholder="描述这条路径的特点、景观等信息"></textarea>
          </div>
          <div class="modal-actions">
            <button type="button" class="cancel-btn" @click="closeEdgeModal">取消</button>
            <button type="submit" class="save-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import AdminNavBar from '@/components/AdminNavBar.vue'
import apiClient from '@/utils/axios'

export default {
  name: 'AdminPathManagement',
  components: {
    AdminNavBar
  },
  data() {
    return {
      scenicAreas: [],
      edges: [],
      showAddEdgeModal: false,
      showEditEdgeModal: false,
      edgeFormData: {
        id: null,
        fromAreaId: '',
        toAreaId: '',
        distance: '',
        duration: '',
        description: ''
      }
    }
  },
  mounted() {
    this.loadScenicAreas()
    this.loadEdges()
  },
  methods: {
    async loadScenicAreas() {
      try {
        const response = await apiClient.get('/api/large-areas/all')
        this.scenicAreas = response.data
      } catch (error) {
        console.error('加载景区列表失败:', error)
        alert('加载景区列表失败，请稍后重试')
      }
    },
    async loadEdges() {
      try {
        const response = await apiClient.get('/api/scenic-area-edges')
        this.edges = response.data
      } catch (error) {
        console.error('加载路径列表失败:', error)
        alert('加载路径列表失败，请稍后重试')
      }
    },
    getAreaName(areaId) {
      const area = this.scenicAreas.find(a => a.id === areaId)
      return area ? area.name : '未知景区'
    },
    editEdge(edge) {
      this.edgeFormData = {
        id: edge.id,
        fromAreaId: edge.fromAreaId,
        toAreaId: edge.toAreaId,
        distance: edge.distance,
        duration: edge.duration,
        description: edge.description || ''
      }
      this.showEditEdgeModal = true
    },
    async deleteEdge(id) {
      if (confirm('确定要删除这条路径吗？')) {
        try {
          await apiClient.delete(`/api/scenic-area-edges/${id}`)
          this.loadEdges()
          alert('路径删除成功')
        } catch (error) {
          console.error('删除路径失败:', error)
          alert('删除路径失败，请稍后重试')
        }
      }
    },
    async saveEdge() {
      try {
        if (this.showAddEdgeModal) {
          await apiClient.post('/api/scenic-area-edges', this.edgeFormData)
        } else {
          await apiClient.put(`/api/scenic-area-edges/${this.edgeFormData.id}`, this.edgeFormData)
        }
        this.closeEdgeModal()
        this.loadEdges()
        alert('路径保存成功')
      } catch (error) {
        console.error('保存路径失败:', error)
        alert('保存路径失败，请稍后重试')
      }
    },
    closeEdgeModal() {
      this.showAddEdgeModal = false
      this.showEditEdgeModal = false
      this.edgeFormData = {
        id: null,
        fromAreaId: '',
        toAreaId: '',
        distance: '',
        duration: '',
        description: ''
      }
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/admin.css';

.admin-path-management-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.admin-content {
  padding: 30px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h1 {
  color: #333;
  font-size: 24px;
  margin: 0;
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-start;
}

.add-btn {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background-color 0.3s;
}

.add-btn:hover {
  background: #45a049;
}

.btn-icon {
  font-size: 16px;
}

.table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.data-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.data-table tr:hover {
  background-color: #f5f5f5;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.edit-btn,
.delete-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.edit-btn:hover {
  background-color: #e3f2fd;
}

.delete-btn:hover {
  background-color: #ffebee;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  padding: 30px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
  font-size: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 600;
  color: #555;
  font-size: 14px;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
  resize: none;
  min-height: 80px;
  max-height: 200px;
  overflow-y: auto;
  max-width: 100%;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.cancel-btn,
.save-btn {
  padding: 8px 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn {
  background: white;
  color: #333;
}

.cancel-btn:hover {
  background: #f5f5f5;
}

.save-btn {
  background: #4CAF50;
  color: white;
  border-color: #4CAF50;
}

.save-btn:hover {
  background: #45a049;
}
</style>
