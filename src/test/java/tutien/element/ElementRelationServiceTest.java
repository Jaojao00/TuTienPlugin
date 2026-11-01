package tutien.element;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ElementRelationServiceTest {

    private ElementRelationService relationService;

    @BeforeEach
    public void setup() {
        relationService = new ElementRelationService();
    }

    @Test
    @DisplayName("Kiểm tra vòng Ngũ Hành Tương Sinh: Mộc -> Hỏa -> Thổ -> Kim -> Thủy -> Mộc")
    public void testNguHanhTuongSinh() {
        assertEquals(NguHanh.HOA, NguHanh.MOC.getSinh());
        assertEquals(NguHanh.THO, NguHanh.HOA.getSinh());
        assertEquals(NguHanh.KIM, NguHanh.THO.getSinh());
        assertEquals(NguHanh.THUY, NguHanh.KIM.getSinh());
        assertEquals(NguHanh.MOC, NguHanh.THUY.getSinh());

        assertTrue(NguHanh.MOC.isSinh(NguHanh.HOA));
        assertTrue(NguHanh.HOA.isDuocSinh(NguHanh.MOC));
    }

    @Test
    @DisplayName("Kiểm tra vòng Ngũ Hành Tương Khắc: Mộc -> Thổ -> Thủy -> Hỏa -> Kim -> Mộc")
    public void testNguHanhTuongKhac() {
        assertEquals(NguHanh.THO, NguHanh.MOC.getKhac());
        assertEquals(NguHanh.THUY, NguHanh.THO.getKhac());
        assertEquals(NguHanh.HOA, NguHanh.THUY.getKhac());
        assertEquals(NguHanh.KIM, NguHanh.HOA.getKhac());
        assertEquals(NguHanh.MOC, NguHanh.KIM.getKhac());

        assertTrue(NguHanh.MOC.isKhac(NguHanh.THO));
        assertTrue(NguHanh.THO.isBiKhac(NguHanh.MOC));
    }

    @Test
    @DisplayName("Đính chính nguồn gốc Lôi và Băng Linh Căn theo Chương 127 Phàm Nhân Tu Tiên")
    public void testDiLinhCanOrigin() {
        // Lôi: Thổ + Thủy
        assertTrue(DiLinhCan.LOI.containsNguHanhGoc(NguHanh.THO));
        assertTrue(DiLinhCan.LOI.containsNguHanhGoc(NguHanh.THUY));
        assertFalse(DiLinhCan.LOI.containsNguHanhGoc(NguHanh.HOA));

        // Băng: Kim + Thủy
        assertTrue(DiLinhCan.BANG.containsNguHanhGoc(NguHanh.KIM));
        assertTrue(DiLinhCan.BANG.containsNguHanhGoc(NguHanh.THUY));
        assertFalse(DiLinhCan.BANG.containsNguHanhGoc(NguHanh.THO));
    }

    @Test
    @DisplayName("Kiểm tra bảng quan hệ Relation(r, e): Đồng hệ = 1.50, Tương sinh = 1.20, Tương khắc = 0.60")
    public void testRelationValues() {
        // Đồng hệ
        assertEquals(1.50, relationService.getRelation("HOA", "HOA"), 0.001);
        assertEquals(1.50, relationService.getRelation("LOI", "LOI"), 0.001);

        // Linh căn sinh công pháp (Mộc sinh Hỏa)
        assertEquals(1.20, relationService.getRelation("MOC", "HOA"), 0.001);
        assertEquals(1.20, relationService.getRelation("HOA", "THO"), 0.001);

        // Tương khắc (Thủy khắc Hỏa, Hỏa khắc Kim)
        assertEquals(0.60, relationService.getRelation("THUY", "HOA"), 0.001);
        assertEquals(0.60, relationService.getRelation("HOA", "THUY"), 0.001);
        assertEquals(0.60, relationService.getRelation("HOA", "KIM"), 0.001);

        // Dị biến: Lôi (Thổ + Thủy) tu công pháp hệ Thủy / Thổ -> 1.35
        assertEquals(1.35, relationService.getRelation("LOI", "THUY"), 0.001);
        assertEquals(1.35, relationService.getRelation("LOI", "THO"), 0.001);
    }
}
