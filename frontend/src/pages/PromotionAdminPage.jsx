
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import {
    getPromotions,
    deletePromotion,
} from '../api/promotionAdmin'

export function PromotionAdminPage() {
    const [promotions, setPromotions] = useState([])
    const [loading, setLoading] = useState(false)

    const [showDeleteModal, setShowDeleteModal] = useState(false)
    const [selectedPromotion, setSelectedPromotion] = useState(null)
    const [deleting, setDeleting] = useState(false)

    const navigate = useNavigate()
    const location = useLocation()

    async function loadPromotions() {
        try {
            setLoading(true)
            const result = await getPromotions()
            setPromotions(result || [])
        } catch (error) {
            toast.error(
                error.response?.data?.message ||
                'Không tải được danh sách khuyến mãi!',
                { toastId: 'load-promotions-error' }
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadPromotions()

        if (location.state?.message) {
            toast.success(location.state.message, {
                toastId: location.state.message,
            })

            navigate(location.pathname, {
                replace: true,
                state: null,
            })
        }
    }, [])

    function handleAdd() {
        navigate('/admin/promotions/new')
    }

    function handleEdit(id) {
        navigate(`/admin/promotions/${id}/edit`)
    }


    function handleDelete(item) {
        setSelectedPromotion(item)
        setShowDeleteModal(true)
    }

    function closeDeleteModal() {
        if (deleting) return

        setShowDeleteModal(false)
        setSelectedPromotion(null)
    }

    async function confirmDelete() {
        if (!selectedPromotion) return

        try {
            setDeleting(true)

            await deletePromotion(selectedPromotion.id)

            setShowDeleteModal(false)
            setSelectedPromotion(null)

            await loadPromotions()

            toast.success('Xóa khuyến mãi thành công!', {
                toastId: 'delete-promotion-success',
            })
        } catch (error) {
            toast.error(
                error.response?.data?.message ||
                'Xóa khuyến mãi thất bại!',
                { toastId: 'delete-promotion-error' }
            )
        } finally {
            setDeleting(false)
        }
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
                            <th>ID</th>
                            <th>Tên khuyến mãi</th>
                            <th>Loại giảm</th>
                            <th>Giá trị</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>

                        <tbody>
                        {promotions.map((item) => (
                            <tr key={item.id}>
                                <td>{item.id}</td>
                                <td>{item.title}</td>
                                <td>{item.discountType}</td>
                                <td>{item.discountValue}</td>
                                <td>{item.status}</td>

                                <td>
                                    <button
                                        type="button"
                                        onClick={() => handleEdit(item.id)}
                                    >
                                        Sửa
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() => handleDelete(item)}
                                    >
                                        Xóa
                                    </button>
                                </td>
                            </tr>
                        ))}

                        {promotions.length === 0 && (
                            <tr>
                                <td colSpan="6">
                                    Chưa có khuyến mãi
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                )}
            </div>

            {showDeleteModal && selectedPromotion && (
                <div
                    style={{
                        position: 'fixed',
                        inset: 0,
                        backgroundColor: 'rgba(0, 0, 0, 0.5)',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        zIndex: 9999,
                    }}
                >
                    <div
                        style={{
                            backgroundColor: '#fff',
                            padding: '28px',
                            borderRadius: '12px',
                            width: '90%',
                            maxWidth: '420px',
                            boxShadow: '0 8px 30px rgba(0,0,0,0.2)',
                        }}
                    >
                        <h2 style={{ marginTop: 0 }}>
                            Xác nhận xóa khuyến mãi
                        </h2>

                        <p>
                            Bạn có chắc chắn muốn xóa khuyến mãi
                            <strong> {selectedPromotion.title}</strong> không?
                        </p>

                        <div
                            style={{
                                display: 'flex',
                                justifyContent: 'flex-end',
                                gap: '10px',
                                marginTop: '24px',
                            }}
                        >
                            <button
                                type="button"
                                onClick={closeDeleteModal}
                                disabled={deleting}
                            >
                                Hủy
                            </button>

                            <button
                                type="button"
                                onClick={confirmDelete}
                                disabled={deleting}
                                style={{
                                    backgroundColor: '#dc3545',
                                    color: '#fff',
                                    border: 'none',
                                    padding: '8px 16px',
                                    borderRadius: '5px',
                                    cursor: deleting ? 'not-allowed' : 'pointer',
                                }}
                            >
                                {deleting ? 'Đang xóa...' : 'Xác nhận xóa'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </section>
    )
}
