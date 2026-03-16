<template>
  <div class="admin-scenic-management-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="page-header">
        <h1>景区管理</h1>
      </div>
      
      <!-- 标签页切换 -->
      <div class="tab-container">
        <div class="tabs">
          <div 
            class="tab" 
            :class="{ active: activeTab === 'areas' }"
            @click="activeTab = 'areas'"
          >
            大景区管理
          </div>
          <div 
            class="tab" 
            :class="{ active: activeTab === 'spots' }"
            @click="activeTab = 'spots'"
          >
            小景点管理
          </div>
        </div>
      </div>
      
      <!-- 大景区管理 -->
      <div v-if="activeTab === 'areas'" class="tab-content">
        <div class="action-bar">
          <button class="add-btn" @click="showAddAreaModal = true">
            <span class="btn-icon">➕</span>
            <span>添加大景区</span>
          </button>
        </div>
        
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>景区名称</th>
                <th>位置</th>
                <th>开放时间</th>
                <th>价格</th>
                <th>标签</th>
                <th>图片</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="area in scenicAreas" :key="area.id">
                <td>{{ area.id }}</td>
                <td>{{ area.name }}</td>
                <td>{{ area.location || '-' }}</td>
                <td>{{ area.openingHours || '-' }}</td>
                <td>{{ area.price || '-' }}</td>
                <td>{{ area.tags || '-' }}</td>
                <td>
                  <img v-if="area.imageUrl" :src="area.imageUrl" :alt="area.name" style="width: 50px; height: 50px; object-fit: cover;" />
                  <span v-else>-</span>
                </td>
                <td>
                  <div class="action-buttons">
                    <button class="edit-btn" @click="editArea(area)">✏️</button>
                    <button class="delete-btn" @click="deleteArea(area.id)">🗑️</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- 小景点管理 -->
      <div v-if="activeTab === 'spots'" class="tab-content">
        <div class="action-bar">
          <button class="add-btn" @click="showAddSpotModal = true">
            <span class="btn-icon">➕</span>
            <span>添加小景点</span>
          </button>
        </div>
        
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>景点名称</th>
                <th>所属景区</th>
                <th>介绍</th>
                <th>游览时长 (分钟)</th>
                <th>标签</th>
                <th>图片</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="spot in scenicSpots" :key="spot.id">
                <td>{{ spot.id }}</td>
                <td>{{ spot.name }}</td>
                <td>{{ spot.areaName || '-' }}</td>
                <td>{{ spot.description || '-' }}</td>
                <td>{{ spot.visitingDuration || '-' }}</td>
                <td>{{ spot.tags || '-' }}</td>
                <td>
                  <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name" style="width: 50px; height: 50px; object-fit: cover;" />
                  <span v-else>-</span>
                </td>
                <td>
                  <div class="action-buttons">
                    <button class="edit-btn" @click="editSpot(spot)">✏️</button>
                    <button class="delete-btn" @click="deleteSpot(spot.id)">🗑️</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    
    <!-- 添加/编辑大景区模态框 -->
    <div v-if="showAddAreaModal || showEditAreaModal" class="modal-overlay" @click="closeAreaModal">
      <div class="modal-content" @click.stop>
        <h2>{{ showAddAreaModal ? '添加大景区' : '编辑大景区' }}</h2>
        <form @submit.prevent="saveArea">
          <div class="form-group">
            <label>景区名称</label>
            <input type="text" v-model="areaFormData.name" required />
          </div>
          <div class="form-group">
            <label>位置</label>
            <input type="text" v-model="areaFormData.location" required />
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="areaFormData.description" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>开放时间</label>
            <input type="text" v-model="areaFormData.openingHours" placeholder="例如：8:00-17:00" />
          </div>
          <div class="form-group">
            <label>选择图片</label>
            <input type="file" accept="image/*" @change="handleAreaImageUpload" />
            <div v-if="areaFormData.imageUrl" class="image-preview">
              <img :src="areaFormData.imageUrl" alt="预览图片" style="max-width: 200px; max-height: 150px;" />
              <button type="button" class="remove-image" @click="removeAreaImage">移除</button>
            </div>
          </div>
          <div class="form-group">
            <label>价格</label>
            <input type="number" v-model="areaFormData.price" step="0.01" placeholder="0.00" required />
          </div>
          <div class="form-group">
            <label>标签（逗号分隔）</label>
            <input type="text" v-model="areaFormData.tags" placeholder="例如：自然景观,历史文化,休闲娱乐" />
          </div>
          <div class="modal-actions">
            <button type="button" class="cancel-btn" @click="closeAreaModal">取消</button>
            <button type="submit" class="save-btn">保存</button>
          </div>
        </form>
      </div>
    </div>
    
    <!-- 添加/编辑小景点模态框 -->
    <div v-if="showAddSpotModal || showEditSpotModal" class="modal-overlay" @click="closeSpotModal">
      <div class="modal-content" @click.stop>
        <h2>{{ showAddSpotModal ? '添加小景点' : '编辑小景点' }}</h2>
        <form @submit.prevent="saveSpot">
          <div class="form-group">
            <label>景点名称</label>
            <input type="text" v-model="spotFormData.name" required />
          </div>
          <div class="form-group">
            <label>所属景区</label>
            <select v-model="spotFormData.largeAreaId" required>
              <option value="">请选择景区</option>
              <option v-for="area in scenicAreas" :key="area.id" :value="area.id">
                {{ area.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>介绍</label>
            <textarea v-model="spotFormData.description" rows="4" required></textarea>
          </div>
          <div class="form-group">
            <label>游览时长 (分钟)</label>
            <input type="number" v-model="spotFormData.visitingDuration" />
          </div>
          <div class="form-group">
            <label>选择图片</label>
            <input type="file" accept="image/*" @change="handleSpotImageUpload" />
            <div v-if="spotFormData.imageUrl" class="image-preview">
              <img :src="spotFormData.imageUrl" alt="预览图片" style="max-width: 200px; max-height: 150px;" />
              <button type="button" class="remove-image" @click="removeSpotImage">移除</button>
            </div>
          </div>
          <div class="form-group">
            <label>标签（逗号分隔）</label>
            <input type="text" v-model="spotFormData.tags" placeholder="例如：自然景观,历史文化,休闲娱乐" />
          </div>
          <div class="modal-actions">
            <button type="button" class="cancel-btn" @click="closeSpotModal">取消</button>
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
  name: 'AdminScenicManagement',
  components: {
    AdminNavBar
  },
  data() {
    return {
      activeTab: 'areas',
      scenicAreas: [],
      scenicSpots: [],
      showAddAreaModal: false,
      showEditAreaModal: false,
      showAddSpotModal: false,
      showEditSpotModal: false,
      areaFormData: {
        id: null,
        name: '',
        description: '',
        location: '',
        imageUrl: '',
        openingHours: '',
        price: '',
        tags: ''
      },
      spotFormData: {
        id: null,
        name: '',
        largeAreaId: null,
        description: '',
        imageUrl: '',
        visitingDuration: 60,
        tags: ''
      }
    }
  },
  mounted() {
    this.loadScenicAreas()
    this.loadScenicSpots()
  },
  methods: {
    async loadScenicAreas() {
      try {
        const response = await apiClient.get('/api/large-areas')
        this.scenicAreas = response.data
      } catch (error) {
        console.error('加载大景区列表失败:', error)
        alert('加载大景区列表失败，请稍后重试')
      }
    },
    async loadScenicSpots() {
      try {
        const response = await apiClient.get('/api/small-spots')
        this.scenicSpots = response.data
      } catch (error) {
        console.error('加载小景点列表失败:', error)
        alert('加载小景点列表失败，请稍后重试')
      }
    },
    editArea(area) {
      this.areaFormData = { 
        id: area.id,
        name: area.name || '',
        description: area.description || '',
        location: area.location || '',
        imageUrl: area.imageUrl || '',
        openingHours: area.openingHours || '',
        price: area.price || '',
        tags: area.tags || ''
      }
      this.showEditAreaModal = true
    },
    async deleteArea(id) {
      if (confirm('确定要删除这个大景区吗？')) {
        try {
          await apiClient.delete(`/api/large-areas/${id}`)
          this.loadScenicAreas()
          this.loadScenicSpots()
          alert('删除成功')
        } catch (error) {
          console.error('删除失败:', error)
          alert('删除失败，请稍后重试')
        }
      }
    },
    async saveArea() {
      try {
        if (this.showAddAreaModal) {
          await apiClient.post('/api/large-areas', this.areaFormData)
          alert('添加成功')
        } else {
          await apiClient.put(`/api/large-areas/${this.areaFormData.id}`, this.areaFormData)
          alert('更新成功')
        }
        this.closeAreaModal()
        this.loadScenicAreas()
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败，请稍后重试')
      }
    },
    closeAreaModal() {
      this.showAddAreaModal = false
      this.showEditAreaModal = false
      this.areaFormData = {
        id: null,
        name: '',
        description: '',
        location: '',
        imageUrl: '',
        openingHours: '',
        price: '',
        tags: ''
      }
    },
    handleAreaImageUpload(event) {
      const file = event.target.files[0]
      if (file) {
        // 创建临时URL用于预览
        const reader = new FileReader()
        reader.onload = (e) => {
          this.areaFormData.imageUrl = e.target.result
        }
        reader.readAsDataURL(file)
        
        // 上传文件到后端
        const formData = new FormData()
        formData.append('image', file)
        apiClient.post('/api/upload/image', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }).then(response => {
          if (response.data.status === 'success') {
            this.areaFormData.imageUrl = response.data.imageUrl
          }
        }).catch(error => {
          console.error('上传失败:', error)
          alert('上传失败，请稍后重试')
        })
      }
    },
    removeAreaImage() {
      this.areaFormData.imageUrl = ''
    },
    editSpot(spot) {
      this.spotFormData = { 
        id: spot.id,
        name: spot.name || '',
        largeAreaId: spot.largeAreaId || null,
        description: spot.description || '',
        imageUrl: spot.imageUrl || '',
        visitingDuration: spot.visitingDuration || 60,
        tags: spot.tags || ''
      }
      this.showEditSpotModal = true
    },
    async deleteSpot(id) {
      if (confirm('确定要删除这个小景点吗？')) {
        try {
          await apiClient.delete(`/api/small-spots/${id}`)
          this.loadScenicSpots()
          alert('删除成功')
        } catch (error) {
          console.error('删除失败:', error)
          alert('删除失败，请稍后重试')
        }
      }
    },
    async saveSpot() {
      try {
        if (this.showAddSpotModal) {
          await apiClient.post('/api/small-spots', this.spotFormData)
          alert('添加成功')
        } else {
          await apiClient.put(`/api/small-spots/${this.spotFormData.id}`, this.spotFormData)
          alert('更新成功')
        }
        this.closeSpotModal()
        this.loadScenicSpots()
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败，请稍后重试')
      }
    },
    closeSpotModal() {
      this.showAddSpotModal = false
      this.showEditSpotModal = false
      this.spotFormData = {
        id: null,
        name: '',
        largeAreaId: null,
        description: '',
        imageUrl: '',
        visitingDuration: 60,
        tags: ''
      }
    },
    handleSpotImageUpload(event) {
      const file = event.target.files[0]
      if (file) {
        // 创建临时URL用于预览
        const reader = new FileReader()
        reader.onload = (e) => {
          this.spotFormData.imageUrl = e.target.result
        }
        reader.readAsDataURL(file)
        
        // 上传文件到后端
        const formData = new FormData()
        formData.append('image', file)
        apiClient.post('/api/upload/image', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        }).then(response => {
          if (response.data.status === 'success') {
            this.spotFormData.imageUrl = response.data.imageUrl
          }
        }).catch(error => {
          console.error('上传失败:', error)
          alert('上传失败，请稍后重试')
        })
      }
    },
    removeSpotImage() {
      this.spotFormData.imageUrl = ''
    }
  }
}
</script>

<style scoped>
.admin-scenic-management-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.admin-content {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
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

.tab-container {
  margin-bottom: 20px;
}

.tabs {
  display: flex;
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.tab {
  flex: 1;
  padding: 15px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 16px;
  font-weight: 500;
  color: #555;
}

.tab:hover {
  background-color: #f8f9fa;
}

.tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
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

.image-preview {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.remove-image {
  padding: 5px 10px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #666;
}

.remove-image:hover {
  background-color: #e0e0e0;
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
