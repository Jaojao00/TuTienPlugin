package tutien.alchemy.escrow;

/**
 * Các trạng thái trong máy trạng thái giao dịch Ký Quỹ Escrow (State Machine).
 * Đảm bảo Zero-Loss và khả năng phục hồi sau sự cố máy chủ (Crash Recovery).
 */
public enum EscrowState {
    /**
     * Ghi nhận bản kê nguyên liệu xuống đĩa trước khi trừ vật phẩm trong túi đồ (Write-Ahead Log)
     */
    PREPARED,

    /**
     * Đã xác nhận trừ nguyên liệu khỏi túi đồ, phiên luyện đan đang chạy hợp lệ
     */
    ACTIVE,

    /**
     * Mẻ luyện đã kết đan thành công, thành phẩm được bảo toàn trên đĩa chờ người chơi nhận
     */
    COMPLETED,

    /**
     * Đang trong tiến trình trao thành phẩm kèm Receipt (Two-Phase Claim)
     */
    CLAIMING,

    /**
     * Người chơi đã nhận thành phẩm thành công vào túi đồ (Có receipt duy nhất)
     */
    CLAIMED,

    /**
     * Phiên luyện bị hủy hoặc gặp sự cố crash, nguyên liệu đang chờ hoàn trả
     */
    REFUND_PENDING,

    /**
     * Đã hoàn trả nguyên liệu thành công về túi đồ hoặc Hòm Thư Đền Bù (Idempotent, không lặp lại)
     */
    REFUNDED,

    /**
     * Giao dịch bị hủy bỏ an toàn vì đối soát chứng minh nguyên liệu chưa hề bị trừ (Chống nhân đôi)
     */
    DISCARDED,

    /**
     * Giao dịch nghi vấn sau sự cố: Inventory không khớp BeforeHash lẫn AfterHash.
     * Được giữ nguyên trên đĩa vĩnh viễn chờ quản trị viên đối soát thủ công, TUYỆT ĐỐI KHÔNG TỰ XÓA!
     */
    NEEDS_RECONCILIATION
}
