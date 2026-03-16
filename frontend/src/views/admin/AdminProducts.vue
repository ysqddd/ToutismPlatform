<template>
  <div class="admin-products-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="page-header">
        <h1>管理套餐</h1>
        <button class="add-btn" @click="showAddModal = true">
          <span class="btn-icon">➕</span>
          <span>添加套餐</span>
        </button>
      </div>
      
      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>套餐名称</th>
              <th>描述</th>
              <th>价格</th>
              <th>图片</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="product in products" :key="product.id">
              <td>{{ product.id }}</td>
              <td>{{ product.name }}</td>
              <td>{{ product.description || '-' }}</td>
              <td>¥{{ product.price }}</td>
              <td>
                <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" style="width: 50px; height: 50px; object-fit: cover;" />
                <span v-else>-</span>
              </td>
              <td>
                <span :class="['status', { active: product.status === 'ON_SALE' }]">
                  {{ product.status === 'ON_SALE' ? '在售' : '下架' }}
                </span>
              </td>
              <td>
                <div class="action-buttons">
                  <button class="edit-btn" @click="editProduct(product)">✏️</button>
                  <button class="delete-btn" @click="deleteProduct(product.id)">🗑️</button>
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
        <h2>{{ showAddModal ? '添加套餐' : '编辑套餐' }}</h2>
        <form @submit.prevent="saveProduct">
          <div class="form-group">
            <label>套餐名称</label>
            <input type="text" v-model="formData.name" required />
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="formData.description" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>选择景区</label>
            <div class="scenic-area-selector">
              <div v-for="area in availableAreas" :key="area.id" class="area-checkbox">
                <input 
                  type="checkbox" 
                  :id="'area-' + area.id" 
                  :value="area.id"
                  v-model="selectedAreaIds"
                  @change="calculatePrice"
                />
                <label :for="'area-' + area.id">
                  {{ area.name }} (¥{{ area.price }})
                </label>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>价格</label>
            <div class="price-display">¥{{ formData.price }}</div>
          </div>
          <div class="form-group">
            <label>选择图片</label>
            <input type="file" accept="image/*" @change="handleImageUpload" />
            <div v-if="formData.imageUrl" class="image-preview">
              <img :src="formData.imageUrl" alt="预览图片" style="max-width: 200px; max-height: 150px;" />
              <button type="button" class="remove-image" @click="removeImage">移除</button>
            </div>
          </div>
          <div class="form-group">
            <label>状态</label>
            <select v-model="formData.status">
              <option value="ON_SALE">在售</option>
              <option value="OFF_SALE">下架</option>
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
  name: 'AdminProducts',
  components: {
    AdminNavBar
  },
  data() {
    return {
      products: [],
      availableAreas: [],
      showAddModal: false,
      showEditModal: false,
      selectedAreaIds: [],
      formData: {
        id: null,
        name: '',
        description: '',
        price: 0,
        imageUrl: '',
        status: 'ON_SALE',
        largeScenicAreas: []
      }
    }
  },
  mounted() {
    this.loadProducts()
    this.loadAvailableAreas()
  },
  methods: {
    async loadProducts() {
      try {
        const response = await apiClient.get('/api/products')
        this.products = response.data
      } catch (error) {
        console.error('加载产品列表失败:', error)
        alert('加载产品列表失败，请稍后重试')
      }
    },
    async loadAvailableAreas() {
      try {
        const response = await apiClient.get('/api/large-areas')
        this.availableAreas = response.data
      } catch (error) {
        console.error('加载景区列表失败:', error)
      }
    },
    calculatePrice() {
      // 前端临时计算价格（后端会重新计算）
      let totalPrice = 0
      const selectedAreas = this.availableAreas.filter(area => 
        this.selectedAreaIds.includes(area.id.toString())
      )
      
      selectedAreas.forEach((area, index) => {
        if (index < 3) {
          totalPrice += parseFloat(area.price)
        } else {
          totalPrice += parseFloat(area.price) * 0.95
        }
      })
      
      if (selectedAreas.length > 3) {
        totalPrice *= 0.99
      }
      
      this.formData.price = Math.round(totalPrice)
      
      // 更新largeScenicAreas
      this.formData.largeScenicAreas = selectedAreas.map(area => ({
        id: area.id,
        name: area.name,
        price: area.price,
        productId: this.formData.id
      }))
    },
    editProduct(product) {
      this.formData = { 
        id: product.id,
        name: product.name || '',
        description: product.description || '',
        price: product.price || 0,
        imageUrl: product.imageUrl || '',
        status: product.status || 'ON_SALE',
        largeScenicAreas: product.largeScenicAreas || []
      }
      
      // 设置已选择的景区ID
      this.selectedAreaIds = (product.largeScenicAreas || []).map(area => area.id.toString())
      
      this.showEditModal = true
    },
    async deleteProduct(id) {
      if (confirm('确定要删除这个套餐吗？')) {
        try {
          await apiClient.delete(`/api/products/${id}`)
          this.loadProducts()
          alert('删除成功')
        } catch (error) {
          console.error('删除失败:', error)
          alert('删除失败，请稍后重试')
        }
      }
    },
    async saveProduct() {
      try {
        if (this.showAddModal) {
          await apiClient.post('/api/products', this.formData)
          alert('添加成功')
        } else {
          await apiClient.put(`/api/products/${this.formData.id}`, this.formData)
          alert('更新成功')
        }
        this.closeModal()
        this.loadProducts()
      } catch (error) {
        console.error('保存失败:', error)
        alert('保存失败，请稍后重试')
      }
    },
    closeModal() {
      this.showAddModal = false
      this.showEditModal = false
      this.selectedAreaIds = []
      this.formData = {
        id: null,
        name: '',
        description: '',
        price: 0,
        imageUrl: '',
        status: 'ON_SALE',
        largeScenicAreas: []
      }
    },
    handleImageUpload(event) {
      const file = event.target.files[0]
      if (file) {
        // 创建临时URL用于预览
        const reader = new FileReader()
        reader.onload = (e) => {
          this.formData.imageUrl = e.target.result
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
            this.formData.imageUrl = response.data.imageUrl
          }
        }).catch(error => {
          console.error('上传失败:', error)
          alert('上传失败，请稍后重试')
        })
      }
    },
    removeImage() {
      this.formData.imageUrl = ''
    }
  }
}
</script>

<style scoped>
.admin-products-container {
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

.status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: #ffebee;
  color: #f44336;
}

.status.active {
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

.scenic-area-selector {
  max-height: 200px;
  overflow-y: auto;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  padding: 10px;
}

.area-checkbox {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.area-checkbox input[type="checkbox"] {
  margin-right: 8px;
}

.price-display {
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  background-color: #f8f9fa;
  font-size: 16px;
  font-weight: 600;
  color: #333;
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
