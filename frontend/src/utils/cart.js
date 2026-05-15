import apiClient from './axios'

function getStoredUserId() {
  const value = localStorage.getItem('userId')
  if (!value) return null

  const userId = Number(value)
  return Number.isFinite(userId) ? userId : null
}

function ensureLoggedIn() {
  if (!localStorage.getItem('token')) {
    throw new Error('LOGIN_REQUIRED')
  }
}

async function addItemToCart(itemType, itemId) {
  ensureLoggedIn()

  const userId = getStoredUserId()
  const payload = {
    itemType,
    itemId: Number(itemId),
    quantity: 1
  }

  if (userId !== null) {
    payload.userId = userId
  }

  const response = await apiClient.post('/api/cart', payload)
  localStorage.setItem('cartRefreshAt', String(Date.now()))
  return response
}

export function addScenicAreaToCart(scenicAreaId) {
  return addItemToCart('SCENIC_AREA', scenicAreaId)
}

export function addProductToCart(productId) {
  return addItemToCart('PRODUCT', productId)
}
