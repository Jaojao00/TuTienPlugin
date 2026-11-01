package tutien.element;

import tutien.core.TuTienPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Quản lý logic nghiệp vụ và sinh trưởng Linh Căn:
 * - Trắc Linh bẩm sinh khi Khai Mạch Nhập Đạo.
 * - Đánh giá và phân loại LinhCanType tự động.
 * - Cơ chế Tẩy Linh Căn / Dị biến bằng Đan Dược & Cơ Duyên.
 * - Chuyển đổi tương thích ngược từ dữ liệu Linh Căn cũ.
 */
public class LinhCanManager {

    private final TuTienPlugin plugin;
    private final ElementRelationService relationService;

    public LinhCanManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.relationService = new ElementRelationService();
    }

    public ElementRelationService getRelationService() {
        return relationService;
    }

    /**
     * Tự động thẩm định loại Linh Căn từ danh sách thuộc tính và phẩm chất.
     */
    public LinhCanType evaluateType(Map<String, Double> elements) {
        if (elements == null || elements.isEmpty()) {
            return LinhCanType.NGUY_LINH_CAN;
        }

        // 1. Nếu chứa bất kỳ Dị Linh Căn nào -> Dị Linh Căn
        for (String elem : elements.keySet()) {
            if (DiLinhCan.fromString(elem).isPresent()) {
                return LinhCanType.DI_LINH_CAN;
            }
        }

        int count = elements.size();
        if (count == 1) {
            return LinhCanType.THIEN_LINH_CAN;
        } else if (count == 2 || count == 3) {
            return LinhCanType.CHAN_LINH_CAN;
        } else {
            return LinhCanType.NGUY_LINH_CAN;
        }
    }

    /**
     * Sinh ngẫu nhiên Linh Căn bẩm sinh khi Phàm Nhân Khai Mạch Nhập Đạo:
     * - Thiên Linh Căn (2.5%): 1 ngũ hành duy nhất, Quality 85 - 100, Purity 0.90 - 1.00.
     * - Dị Linh Căn (3.5%): Lôi, Băng, Phong,... Quality 80 - 95, Purity 0.85 - 0.98.
     * - Chân Linh Căn (30.0%): 2 hoặc 3 ngũ hành, Quality 55 - 85, Purity 0.75 - 0.90.
     * - Ngụy Linh Căn (64.0%): 4 hoặc 5 ngũ hành, Quality 25 - 65, Purity 0.70 - 0.82.
     */
    public LinhCanData generateRandomLinhCan() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int roll = rand.nextInt(1000); // 0 -> 999

        if (roll < 25) { // 2.5% - Thiên Linh Căn
            return generateThienLinhCan(rand);
        } else if (roll < 60) { // 3.5% - Dị Linh Căn
            return generateDiLinhCan(rand);
        } else if (roll < 360) { // 30.0% - Chân Linh Căn
            return generateChanLinhCan(rand);
        } else { // 64.0% - Ngụy Linh Căn (Tạp Linh Căn)
            return generateNguyLinhCan(rand);
        }
    }

    private LinhCanData generateThienLinhCan(ThreadLocalRandom rand) {
        NguHanh[] allNh = NguHanh.values();
        NguHanh chosen = allNh[rand.nextInt(allNh.length)];

        double purity = 0.90 + (rand.nextDouble() * 0.10); // 0.90 - 1.00
        double quality = 85.0 + (rand.nextDouble() * 15.0); // 85 - 100

        LinhCanData data = new LinhCanData(LinhCanType.THIEN_LINH_CAN, LinhCanType.THIEN_LINH_CAN, purity);
        data.setQuality(chosen.name(), quality);
        return data;
    }

    private LinhCanData generateDiLinhCan(ThreadLocalRandom rand) {
        DiLinhCan[] allDi = DiLinhCan.values();
        DiLinhCan chosen = allDi[rand.nextInt(allDi.length)];

        double purity = 0.85 + (rand.nextDouble() * 0.13); // 0.85 - 0.98
        double quality = 80.0 + (rand.nextDouble() * 16.0); // 80 - 96

        LinhCanData data = new LinhCanData(LinhCanType.DI_LINH_CAN, LinhCanType.DI_LINH_CAN, purity);
        data.setQuality(chosen.name(), quality);
        return data;
    }

    private LinhCanData generateChanLinhCan(ThreadLocalRandom rand) {
        int count = rand.nextBoolean() ? 2 : 3;
        List<NguHanh> available = new ArrayList<>(Arrays.asList(NguHanh.values()));
        Collections.shuffle(available, rand);

        double purity = 0.75 + (rand.nextDouble() * 0.15); // 0.75 - 0.90
        LinhCanData data = new LinhCanData(LinhCanType.CHAN_LINH_CAN, LinhCanType.CHAN_LINH_CAN, purity);

        for (int i = 0; i < count; i++) {
            NguHanh nh = available.get(i);
            double quality = 55.0 + (rand.nextDouble() * 30.0); // 55 - 85
            data.setQuality(nh.name(), quality);
        }
        return data;
    }

    private LinhCanData generateNguyLinhCan(ThreadLocalRandom rand) {
        int count = rand.nextBoolean() ? 4 : 5;
        List<NguHanh> available = new ArrayList<>(Arrays.asList(NguHanh.values()));
        Collections.shuffle(available, rand);

        double purity = 0.70 + (rand.nextDouble() * 0.12); // 0.70 - 0.82
        LinhCanData data = new LinhCanData(LinhCanType.NGUY_LINH_CAN, LinhCanType.NGUY_LINH_CAN, purity);

        for (int i = 0; i < count; i++) {
            NguHanh nh = available.get(i);
            double quality = 25.0 + (rand.nextDouble() * 45.0); // 25 - 70
            data.setQuality(nh.name(), quality);
        }
        return data;
    }

    /**
     * Chuyển đổi tương thích từ enum cũ (LinhCan cũ) sang LinhCanData chuẩn Phase 2.1.
     */
    public LinhCanData createFromLegacyEnum(String legacyEnumName) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        if (legacyEnumName == null) legacyEnumName = "PHE_LINH_CAN";

        switch (legacyEnumName.toUpperCase()) {
            case "THIEN_LINH_CAN":
                return generateThienLinhCan(rand);
            case "HON_DON_LINH_CAN": {
                // Hỗn Độn: Dị linh căn cấp tối thượng hoặc Ngũ Hành viên mãn
                LinhCanData data = new LinhCanData(LinhCanType.DI_LINH_CAN, LinhCanType.DI_LINH_CAN, 1.0);
                data.setQuality(DiLinhCan.QUANG.name(), 98.0);
                data.addMutation("HON_DON_KHOI_NGUYEN", "Thừa hưởng tư chất Hỗn Độn vi thượng cổ");
                return data;
            }
            case "DIA_LINH_CAN":
            case "CHAN_LINH_CAN":
                return generateChanLinhCan(rand);
            case "TAP_LINH_CAN":
            case "PHE_LINH_CAN":
            default:
                return generateNguyLinhCan(rand);
        }
    }
}
