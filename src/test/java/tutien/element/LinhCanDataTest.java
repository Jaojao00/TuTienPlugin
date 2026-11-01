package tutien.element;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LinhCanDataTest {

    @Test
    @DisplayName("Kiểm tra phân loại LinhCanType theo số lượng thuộc tính và dị biến")
    public void testLinhCanTypeEvaluation() {
        LinhCanManager manager = new LinhCanManager(null);

        // 1 thuộc tính ngũ hành -> Thiên Linh Căn
        Map<String, Double> thien = new HashMap<>();
        thien.put("HOA", 95.0);
        assertEquals(LinhCanType.THIEN_LINH_CAN, manager.evaluateType(thien));

        // 2-3 thuộc tính ngũ hành -> Chân Linh Căn
        Map<String, Double> chan = new HashMap<>();
        chan.put("MOC", 80.0);
        chan.put("HOA", 70.0);
        assertEquals(LinhCanType.CHAN_LINH_CAN, manager.evaluateType(chan));

        // 4-5 thuộc tính ngũ hành -> Ngụy Linh Căn
        Map<String, Double> nguy = new HashMap<>();
        nguy.put("KIM", 50.0);
        nguy.put("MOC", 45.0);
        nguy.put("THUY", 60.0);
        nguy.put("HOA", 55.0);
        assertEquals(LinhCanType.NGUY_LINH_CAN, manager.evaluateType(nguy));

        // Chứa thuộc tính dị biến -> Dị Linh Căn
        Map<String, Double> di = new HashMap<>();
        di.put("LOI", 90.0);
        assertEquals(LinhCanType.DI_LINH_CAN, manager.evaluateType(di));
    }

    @Test
    @DisplayName("Kiểm tra Serialization và Deserialization của LinhCanData")
    public void testSerialization() {
        LinhCanData data = new LinhCanData(LinhCanType.CHAN_LINH_CAN, LinhCanType.CHAN_LINH_CAN, 0.85);
        data.setQuality("MOC", 88.0);
        data.setQuality("HOA", 72.0);
        data.addMutation("TAY_TUY_DAN", "Tẩy trừ tạp căn Hỏa, tăng cường Mộc căn");

        assertEquals("MOC", data.getPrimaryElement());
        assertEquals(88.0, data.getQuality("MOC"));
        assertEquals(72.0, data.getQuality("HOA"));
        assertEquals(0.0, data.getQuality("KIM"));

        // Chuyển sang Map
        Map<String, Object> map = data.toMap();
        assertNotNull(map);
        assertEquals("CHAN_LINH_CAN", map.get("originType"));
        assertEquals("CHAN_LINH_CAN", map.get("currentType"));
        assertEquals(0.85, (Double) map.get("purity"), 0.001);

        // Khôi phục từ Map
        LinhCanData restored = LinhCanData.fromMap(map);
        assertEquals(LinhCanType.CHAN_LINH_CAN, restored.getOriginType());
        assertEquals(LinhCanType.CHAN_LINH_CAN, restored.getCurrentType());
        assertEquals(0.85, restored.getPurity(), 0.001);
        assertEquals(88.0, restored.getQuality("MOC"));
        assertEquals(72.0, restored.getQuality("HOA"));
        assertEquals("MOC", restored.getPrimaryElement());
        assertEquals(1, restored.getMutations().size());
        assertEquals("TAY_TUY_DAN", restored.getMutations().get(0).getReason());
    }

    @Test
    @DisplayName("Kiểm tra tạo ngẫu nhiên Linh Căn không null và nằm trong giới hạn chuẩn")
    public void testRandomGeneration() {
        LinhCanManager manager = new LinhCanManager(null);
        for (int i = 0; i < 50; i++) {
            LinhCanData data = manager.generateRandomLinhCan();
            assertNotNull(data);
            assertNotNull(data.getCurrentType());
            assertTrue(data.getPurity() >= 0.70 && data.getPurity() <= 1.00);
            assertFalse(data.getElementQualityMap().isEmpty());
            for (double q : data.getElementQualityMap().values()) {
                assertTrue(q >= 1.0 && q <= 100.0);
            }
        }
    }
}
