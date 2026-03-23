<template>
  <div class="admin-scenic-edge-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="page-header">
        <h1>景区内路径管理</h1>
        <p class="page-desc">管理景区内部小景点之间的路径连接</p>
      </div>
      
      <div class="filter-bar">
        <div class="filter-group">
          <label>选择大景区：</label>
          <select v-model="selectedLargeAreaId" @change="loadScenicSpots">
            <option value="">请选择大景区</option>
            <option v-for="area in scenicAreas" :key="area.id" :value="area.id">
              {{ area.name }}
            </option>
          </select>
        </div>
      </div>
      
      <div class="action-bar" v-if="selectedLargeAreaId">
        <button class="add-btn" @click="showAddEdgeModal = true">
          <span class="btn-icon">➕</span>
          <span>添加景区内路径</span>
        </button>
      </div>
      
      <div class="table-container" v-if="selectedLargeAreaId">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>起始小景点</th>
              <th>结束小景点</th>
              <th>距离 (米)</th>
              <th>时间 (分钟)</th>
              <th>交通方式</th>
              <th>强度等级</th>
              <th>路径描述</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="edge in edges" :key="edge.id">
              <td>{{ edge.id }}</td>
              <td>{{ getSpotName(edge.startSpotId) }}</td>
              <td>{{ getSpotName(edge.endSpotId) }}</td>
              <td>{{ edge.distance }}</td>
              <td>{{ edge.timeCost }}</td>
              <td>{{ getTransportModeLabel(edge.transportMode) }}</td>
              <td>{{ edge.intensityLevel }}</td>
              <td>{{ edge.pathDescription || '-' }}</td>
              <td>
                <div class="action-buttons">
                  <button class="edit-btn" @click="editEdge(edge)">✏️</button>
                  <button class="delete-btn" @click="deleteEdge(edge.id)">🗑️</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="edges.length === 0" class="empty-state">
          暂无路径数据，请添加
        </div>
      </div>
      
      <div v-else class="empty-state">
        请先选择一个大景区
      </div>
    </div>
    
    <div v-if="showAddEdgeModal || showEditEdgeModal" class="modal-overlay" @click="closeEdgeModal">
      <div class="modal-content" @click.stop>
        <h2>{{ showAddEdgeModal ? '添加景区内路径' : '编辑景区内路径' }}</h2>
        <form @submit.prevent="saveEdge">
          <div class="form-group">
            <label>起始小景点</label>
            <select v-model="edgeFormData.startSpotId" required>
              <option value="">请选择起始小景点</option>
              <option v-for="spot in scenicSpots" :key="spot.id" :value="spot.id">
                {{ spot.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>结束小景点</label>
            <select v-model="edgeFormData.endSpotId" required>
              <option value="">请选择结束小景点</option>
              <option v-for="spot in scenicSpots" :key="spot.id" :value="spot.id">
                {{ spot.name }}
              </option>
            </select>
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>距离 (米)</label>
              <input type="number" v-model="edgeFormData.distance" step="0.1" required />
            </div>
            <div class="form-group half">
              <label>时间 (分钟)</label>
              <input type="number" v-model="edgeFormData.timeCost" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>交通方式</label>
              <select v-model="edgeFormData.transportMode" required>
                <option value="WALK">步行</option>
                <option value="SHUTTLE">摆渡车</option>
                <option value="CABLEWAY">缆车</option>
              </select>
            </div>
            <div class="form-group half">
              <label>强度等级 (1-5)</label>
              <input type="number" v-model="edgeFormData.intensityLevel" min="1" max="5" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>沿途风景分 (0-5)</label>
              <input type="number" v-model="edgeFormData.scenicScore" step="0.1" min="0" max="5" required />
            </div>
            <div class="form-group half">
              <label>舒适度分 (0-5)</label>
              <input type="number" v-model="edgeFormData.comfortScore" step="0.1" min="0" max="5" required />
            </div>
          </div>
          <div class="form-group">
            <label>老人友好分 (0-5)</label>
            <input type="number" v-model="edgeFormData.elderlyFriendlyScore" step="0.1" min="0" max="5" required />
          </div>
          <div class="form-group">
            <label>路径描述</label>
            <textarea v-model="edgeFormData.pathDescription" rows="3" placeholder="描述这条路径的特点、景观等信息"></textarea>
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
  name: 'AdminScenicEdgeManagement',
  components: {
    AdminNavBar
  },
  data() {
    return {
      scenicAreas: [],
      scenicSpots: [],
      edges: [],
      selectedLargeAreaId: '',
      showAddEdgeModal: false,
      showEditEdgeModal: false,
      edgeFormData: {
        id: null,
        largeAreaId: null,
        startSpotId: '',
        endSpotId: '',
        distance: '',
        timeCost: '',
        pathDescription: '',
        transportMode: 'WALK',
        intensityLevel: 2,
        scenicScore: 3.0,
        comfortScore: 3.0,
        elderlyFriendlyScore: 3.0
      }
    }
  },
  mounted() {
    this.loadScenicAreas()
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
    async loadScenicSpots() {
      if (!this.selectedLargeAreaId) {
        this.scenicSpots = []
        this.edges = []
        return
      }
      try {
        const [spotsRes, edgesRes] = await Promise.all([
          apiClient.get(`/api/small-spots/large-area/${this.selectedLargeAreaId}`),
          apiClient.get(`/api/scenic-edges/large-area/${this.selectedLargeAreaId}`)
        ])
        this.scenicSpots = spotsRes.data
        this.edges = edgesRes.data
      } catch (error) {
        console.error('加载数据失败:', error)
        alert('加载数据失败，请稍后重试')
      }
    },
    getSpotName(spotId) {
      const spot = this.scenicSpots.find(s => s.id === spotId)
      return spot ? spot.name : '未知景点'
    },
    getTransportModeLabel(mode) {
      const modes = {
        'WALK': '步行',
        'SHUTTLE': '摆渡车',
        'CABLEWAY': '缆车'
      }
      return modes[mode] || mode
    },
    editEdge(edge) {
      this.edgeFormData = {
        id: edge.id,
        largeAreaId: edge.largeAreaId,
        startSpotId: edge.startSpotId,
        endSpotId: edge.endSpotId,
        distance: edge.distance,
        timeCost: edge.timeCost,
        pathDescription: edge.pathDescription || '',
        transportMode: edge.transportMode || 'WALK',
        intensityLevel: edge.intensityLevel || 2,
        scenicScore: edge.scenicScore || 3.0,
        comfortScore: edge.comfortScore || 3.0,
        elderlyFriendlyScore: edge.elderlyFriendlyScore || 3.0
      }
      this.showEditEdgeModal = true
    },
    async deleteEdge(id) {
      if (confirm('确定要删除这条路径吗？')) {
        try {
          await apiClient.delete(`/api/scenic-edges/${id}`)
          this.loadScenicSpots()
          alert('路径删除成功')
        } catch (error) {
          console.error('删除路径失败:', error)
          alert('删除路径失败，请稍后重试')
        }
      }
    },
    async saveEdge() {
      try {
        const data = {
          ...this.edgeFormData,
          largeAreaId: this.selectedLargeAreaId
        }
        if (this.showAddEdgeModal) {
          await apiClient.post('/api/scenic-edges', data)
        } else {
          await apiClient.put(`/api/scenic-edges/${this.edgeFormData.id}`, data)
        }
        this.closeEdgeModal()
        this.loadScenicSpots()
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
        largeAreaId: null,
        startSpotId: '',
        endSpotId: '',
        distance: '',
        timeCost: '',
        pathDescription: '',
        transportMode: 'WALK',
        intensityLevel: 2,
        scenicScore: 3.0,
        comfortScore: 3.0,
        elderlyFriendlyScore: 3.0
      }
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/admin.css';

.admin-scenic-edge-container {
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

.page-desc {
  color: #666;
  font-size: 14px;
  margin-top: 8px;
}

.filter-bar {
  margin-bottom: 20px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-group label {
  font-weight: 600;
  color: #555;
  font-size: 14px;
}

.filter-group select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 200px;
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

.empty-state {
  padding: 40px;
  text-align: center;
  color: #999;
  font-size: 16px;
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
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
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

.form-row {
  display: flex;
  gap: 15px;
}

.form-group.half {
  flex: 1;
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
