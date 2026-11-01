package tutien.cultivation.service;

import tutien.cultivation.model.CongPhap;
import tutien.element.ElementRelationService;
import tutien.element.LinhCanData;

import java.util.List;
import java.util.Set;

/**
 * Dịch vụ tính toán hệ số tương thích Φ giữa Linh Căn tu sĩ và Công Pháp (Phase 2.3):
 *
 * Công thức chuẩn hóa:
 *   A_e = max_{r in Roots} ( Relation(r, e) * Quality(r) / 100 )
 *   Phi = Clamp(0.40, 1.60, sum_{e in E_req} w_e * A_e)
 *
 * Trong đó:
 * - Roots: Những thuộc tính linh căn người chơi sở hữu.
 * - E_req: Những thuộc tính công pháp yêu cầu (hoặc danh sách thuộc tính của công pháp).
 * - w_e: Trọng số phân bổ đều (sum w_e = 1.0).
 * - Relation(r, e): Đồng hệ (1.50), Linh căn sinh công pháp (1.20), Dị biến hợp gốc (1.35), Tương khắc (0.60), Khác (1.00).
 * - LinhCanPurity KHÔNG nhân trực tiếp vào Phi để tránh nhân đôi lợi thế Quality.
 */
public class ElementCompatibilityService {

    public static final double MIN_PHI = 0.40;
    public static final double MAX_PHI = 1.60;
    public static final double DEFAULT_PHI = 1.00;

    private final ElementRelationService relationService;

    public ElementCompatibilityService(ElementRelationService relationService) {
        this.relationService = relationService != null ? relationService : new ElementRelationService();
    }

    public ElementRelationService getRelationService() {
        return relationService;
    }

    /**
     * Tính toán hệ số tương thích Phi giữa Linh Căn của tu sĩ và Công Pháp.
     */
    public double calculateCompatibility(LinhCanData linhCan, CongPhap cp) {
        if (cp == null) return DEFAULT_PHI;

        // Ưu tiên lấy required-elements, nếu rỗng thì lấy danh sách elements của công pháp
        List<String> targetElements = !cp.getRequiredElements().isEmpty()
                ? cp.getRequiredElements()
                : cp.getElements();

        // Nếu công pháp phổ quát, không yêu cầu nguyên tố ngũ hành cụ thể (như Cự Khí Quyết)
        if (targetElements == null || targetElements.isEmpty()) {
            return DEFAULT_PHI;
        }

        if (linhCan == null || linhCan.getElementQualityMap().isEmpty()) {
            return MIN_PHI;
        }

        Set<String> roots = linhCan.getElements();
        double sumAe = 0.0;
        int count = targetElements.size();
        double weight = 1.0 / count;

        for (String reqElem : targetElements) {
            double bestAe = 0.0;
            for (String root : roots) {
                double rel = relationService.getRelation(root, reqElem);
                double qual = linhCan.getQuality(root);
                double ae = rel * (qual / 100.0);
                if (ae > bestAe) {
                    bestAe = ae;
                }
            }
            sumAe += weight * bestAe;
        }

        return clamp(sumAe, MIN_PHI, MAX_PHI);
    }

    private static double clamp(double val, double min, double max) {
        if (Double.isNaN(val)) return min;
        return Math.max(min, Math.min(max, val));
    }
}
