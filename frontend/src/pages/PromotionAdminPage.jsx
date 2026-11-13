
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { getPromotions } from '../api/promotionAdmin'

export function PromotionAdminPage() {
    const [promotions, setPromotions] = useState([])
    const [loading, setLoading] = useState(false)

    const navigate = useNavigate()

    async function loadPromotions() {
        try {
            setLoading(true)
            const result = await getPromotions()
            setPromotions(result || [])
        } catch (error) {
            toast.error(
                error.response?.data?.message ||
                'Không tải được danh sách khuyến mãi!'
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadPromotions()
    }, [])

    function handleAdd() {
        navigate('/admin/promotions/new')
    }

    function handleEdit(id) {
        navigate(`/admin/promotions/${id}/edit`)
    }

    function handleDetail(id) {
        navigate(`/admin/promotions/${id}`)
    }

    function getPromotionCode(id) {
        return `KM${String(id).padStart(4, '0')}`
    }

    function getPromotionType(type) {
        if (type === 'PERCENT') return 'Giảm theo phần trăm'
        if (type === 'FIXED') return 'Giảm số tiền'
        return type || 'Chưa xác định'
    }

    function getPromotionStatus(item) {
        const now = new Date()
        const startDate = new Date(item.startsAt)
        const endDate = new Date(item.endsAt)

        if (item.status === 'INACTIVE') {
            return 'Ngừng hoạt động'
        }

        if (item.status === 'EXPIRED' || now > endDate) {
            return 'Đã hết hạn'
        }

        if (now < startDate) {
            return 'Sắp diễn ra'
        }

        return 'Đang diễn ra'
    }

    function formatDate(date) {
        if (!date) return ''

        return new Date(date).toLocaleDateString('vi-VN')
    }

    function formatDiscountValue(item) {
        if (item.discountType === 'PERCENT') {
            return `${item.discountValue}%`
        }

        return `${Number(item.discountValue).toLocaleString('vi-VN')} VNĐ`
    }

    return (
        <section className="role-dashboard">
            <p className="eyebrow">QUẢN TRỊ</p>

            <h1>Quản lý khuyến mãi</h1>

            <div className="feature-card">
                <h2>Danh sách khuyến mãi</h2>

                <button type="button" onClick={handleAdd}>
                    + Thêm khuyến mãi
                </button>

                {loading ? (
                    <p>Đang tải dữ liệu...</p>
                ) : (
                    <table>
                        <thead>
                        <tr>
                            <th>STT</th>
                            <th>Mã khuyến mãi</th>
                            <th>Tên khuyến mãi</th>
                            <th>Loại khuyến mãi</th>
                            <th>Giá trị ưu đãi</th>
                            <th>Ngày bắt đầu</th>
                            <th>Ngày kết thúc</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>

                        <tbody>
                        {promotions.map((item, index) => (
                            <tr key={item.id}>
                                <td>{index + 1}</td>

                                <td>{getPromotionCode(item.id)}</td>

                                <td>{item.title}</td>

                                <td>
                                    {getPromotionType(item.discountType)}
                                </td>

                                <td>{formatDiscountValue(item)}</td>

                                <td>{formatDate(item.startsAt)}</td>

                                <td>{formatDate(item.endsAt)}</td>

                                <td>{getPromotionStatus(item)}</td>

                                <td>
                                    <button
                                        type="button"
                                        onClick={() => handleDetail(item.id)}
                                    >
                                        Xem chi tiết
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() => handleEdit(item.id)}
                                    >
                                        Chỉnh sửa
                                    </button>
                                </td>
                            </tr>
                        ))}

                        {promotions.length === 0 && (
                            <tr>
                                <td colSpan="9">
                                    Chưa có khuyến mãi
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                )}
            </div>
        </section>
    )
}
