package tutien.cultivation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tutien.core.CanhGioi;

import static org.junit.jupiter.api.Assertions.*;

public class RealmTuViBalanceTest {

    @Test
    @DisplayName("Kiểm thử 1: CanhGioi calculateTang cho Luyện Khí (13 Tầng)")
    public void testLuyenKhiCalculateTang() {
        CanhGioi lk = CanhGioi.LUYEN_KHI;
        assertEquals(13, lk.getMaxSubStages());
        assertEquals(150, lk.getTuViMoiTang());
        assertEquals(1950, lk.getTuViYeuCau());

        // Tầng 1: 0 -> 149
        assertEquals(1, CanhGioi.calculateTang(lk, 0));
        assertEquals(1, CanhGioi.calculateTang(lk, 149));

        // Tầng 2: 150 -> 299
        assertEquals(2, CanhGioi.calculateTang(lk, 150));
        assertEquals(2, CanhGioi.calculateTang(lk, 299));

        // Tầng 3: 300
        assertEquals(3, CanhGioi.calculateTang(lk, 300));

        // Tầng 12: 1650 -> 1799
        assertEquals(12, CanhGioi.calculateTang(lk, 1650));
        assertEquals(12, CanhGioi.calculateTang(lk, 1799));

        // Tầng 13 Viên Mãn: 1800 -> 1950
        assertEquals(13, CanhGioi.calculateTang(lk, 1800));
        assertEquals(13, CanhGioi.calculateTang(lk, 1950));

        // Không bao giờ vượt quá tầng 13
        assertEquals(13, CanhGioi.calculateTang(lk, 2000));
        assertEquals(13, CanhGioi.calculateTang(lk, 6000));
    }

    @Test
    @DisplayName("Kiểm thử 2: CanhGioi calculateTang cho Trúc Cơ (4 Kỳ)")
    public void testTrucCoCalculateTang() {
        CanhGioi tc = CanhGioi.TRUC_CO;
        assertEquals(4, tc.getMaxSubStages());
        assertEquals(1500, tc.getTuViMoiTang());
        assertEquals(6000, tc.getTuViYeuCau());

        // Sơ Kỳ: 0 -> 1499
        assertEquals(1, CanhGioi.calculateTang(tc, 0));
        assertEquals(1, CanhGioi.calculateTang(tc, 1499));

        // Trung Kỳ: 1500 -> 2999
        assertEquals(2, CanhGioi.calculateTang(tc, 1500));
        assertEquals(2, CanhGioi.calculateTang(tc, 2999));

        // Hậu Kỳ: 3000 -> 4499
        assertEquals(3, CanhGioi.calculateTang(tc, 3000));
        assertEquals(3, CanhGioi.calculateTang(tc, 4499));

        // Đại Viên Mãn: 4500 -> 6000
        assertEquals(4, CanhGioi.calculateTang(tc, 4500));
        assertEquals(4, CanhGioi.calculateTang(tc, 6000));

        // Capped tại 4
        assertEquals(4, CanhGioi.calculateTang(tc, 10000));
    }

    @Test
    @DisplayName("Kiểm thử 3: CanhGioi calculateTang cho Phàm Nhân (1 Kỳ)")
    public void testPhamNhanCalculateTang() {
        CanhGioi pn = CanhGioi.PHAM_NHAN;
        assertEquals(1, pn.getMaxSubStages());
        assertEquals(100, pn.getTuViYeuCau());

        assertEquals(1, CanhGioi.calculateTang(pn, 0));
        assertEquals(1, CanhGioi.calculateTang(pn, 50));
        assertEquals(1, CanhGioi.calculateTang(pn, 100));
        assertEquals(1, CanhGioi.calculateTang(pn, 500));
    }

    @Test
    @DisplayName("Kiểm thử 4: Cân bằng Kim Đan cảnh giới")
    public void testKimDanTuVi() {
        CanhGioi kd = CanhGioi.KIM_DAN;
        assertEquals(4, kd.getMaxSubStages());
        assertEquals(6000, kd.getTuViMoiTang());
        assertEquals(24000, kd.getTuViYeuCau());

        assertEquals(1, CanhGioi.calculateTang(kd, 0));
        assertEquals(2, CanhGioi.calculateTang(kd, 6000));
        assertEquals(3, CanhGioi.calculateTang(kd, 12000));
        assertEquals(4, CanhGioi.calculateTang(kd, 18000));
        assertEquals(4, CanhGioi.calculateTang(kd, 24000));
    }
}
