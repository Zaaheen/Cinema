
import { apiClient } from './client'

export async function getPromotions() {
    const { data } = await apiClient.get('/admin/promotions')
    return data.data
}

export async function createPromotion(promotion) {
    const { data } = await apiClient.post('/admin/promotions', promotion)
    return data
}

export async function updatePromotion(id, promotion) {
    const { data } = await apiClient.patch(
        `/admin/promotions/${id}`,
        promotion
    )
    return data
}

export async function deletePromotion(id) {
    const { data } = await apiClient.delete(`/admin/promotions/${id}`)
    return data
}
