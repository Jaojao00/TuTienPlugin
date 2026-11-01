package tutien.element;

import java.util.Optional;

/**
 * Dịch vụ tính toán quan hệ nguyên tố giữa Linh Căn của tu sĩ (root r)
 * và yêu cầu nguyên tố của Công Pháp / Thần Thông (required e).
 *
 * Bảng quan hệ chuẩn (Phase 2.3 & 2.1):
 * - Đồng hệ (r == e): 1.50
 * - Linh căn sinh công pháp (r sinh e): 1.20
 * - Tương khắc (r khắc e hoặc e khắc r): 0.60
 * - Dị linh căn tương hợp nguyên tố gốc (vd Lôi -> Thổ/Thủy): 1.35
 * - Không thuộc hệ Ngũ Hành đã định nghĩa / Khác: 1.00
 */
public class ElementRelationService {

    public static final double RELATION_SAME = 1.50;
    public static final double RELATION_GENERATING = 1.20; // r sinh e
    public static final double RELATION_MUTATION_MATCH = 1.35; // Dị linh căn với ngũ hành gốc
    public static final double RELATION_OVERCOMING = 0.60; // r khắc e hoặc e khắc r
    public static final double RELATION_DEFAULT = 1.00;

    /**
     * Tính hệ số quan hệ giữa một thuộc tính linh căn của người chơi (root)
     * và một thuộc tính yêu cầu của công pháp (required).
     *
     * @param root Thuộc tính linh căn người chơi sở hữu (ví dụ "KIM", "MOC", "LOI")
     * @param req Thuộc tính công pháp yêu cầu (ví dụ "HOA", "LOI")
     * @return Hệ số quan hệ Relation(r, e)
     */
    public double getRelation(String root, String req) {
        if (root == null || req == null) {
            return RELATION_DEFAULT;
        }

        String rClean = root.trim().toUpperCase();
        String reqClean = req.trim().toUpperCase();

        // 1. Đồng hệ
        if (rClean.equals(reqClean)) {
            return RELATION_SAME;
        }

        // 2. Kiểm tra nếu cả hai đều thuộc Ngũ Hành chuẩn
        Optional<NguHanh> rootNguHanh = NguHanh.fromString(rClean);
        Optional<NguHanh> reqNguHanh = NguHanh.fromString(reqClean);

        if (rootNguHanh.isPresent() && reqNguHanh.isPresent()) {
            NguHanh r = rootNguHanh.get();
            NguHanh e = reqNguHanh.get();

            // Linh căn sinh công pháp: r sinh e (vd Mộc sinh Hỏa)
            if (r.isSinh(e)) {
                return RELATION_GENERATING;
            }

            // Tương khắc: r khắc e (vd Thủy khắc Hỏa) hoặc e khắc r
            if (r.isKhac(e) || e.isKhac(r)) {
                return RELATION_OVERCOMING;
            }

            // Nếu không sinh không khắc trực tiếp trong ngũ hành (ví dụ Kim và Hỏa là khắc, Kim và Mộc là khắc, Kim và Thổ là sinh)
            // Trong ngũ hành 5 phần tử: mỗi hành sinh 1 hành, bị sinh bởi 1 hành, khắc 1 hành, bị khắc bởi 1 hành.
            // Do đó giữa 2 hành bất kỳ luôn có quan hệ sinh hoặc khắc!
            return RELATION_DEFAULT;
        }

        // 3. Xử lý Dị Linh Căn
        Optional<DiLinhCan> rootDi = DiLinhCan.fromString(rClean);
        Optional<DiLinhCan> reqDi = DiLinhCan.fromString(reqClean);

        // Trường hợp root là Dị Linh Căn, req là Ngũ Hành
        if (rootDi.isPresent() && reqNguHanh.isPresent()) {
            DiLinhCan rd = rootDi.get();
            NguHanh e = reqNguHanh.get();
            if (rd.containsNguHanhGoc(e)) {
                return RELATION_MUTATION_MATCH; // Vd Lôi tu công pháp hệ Thủy hoặc Thổ
            }
            // Kiểm tra xem ngũ hành gốc có sinh ra e không
            for (NguHanh goc : rd.getNguHanhGoc()) {
                if (goc.isSinh(e)) {
                    return RELATION_GENERATING;
                }
                if (goc.isKhac(e) || e.isKhac(goc)) {
                    return RELATION_OVERCOMING;
                }
            }
        }

        // Trường hợp root là Ngũ Hành, req là Dị Linh Căn
        if (rootNguHanh.isPresent() && reqDi.isPresent()) {
            NguHanh r = rootNguHanh.get();
            DiLinhCan ed = reqDi.get();
            if (ed.containsNguHanhGoc(r)) {
                return 1.15; // Ngũ hành gốc tu công pháp biến dị
            }
        }

        // Mặc định cho các hệ đặc thù khác (Ám, Quang, Huyết, Mộng...)
        return RELATION_DEFAULT;
    }
}
