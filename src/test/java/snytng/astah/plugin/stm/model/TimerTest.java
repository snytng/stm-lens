package snytng.astah.plugin.stm.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TimerTest {

    @Test
    void testParseTimeEvent() {
        // SimulationEngine または TimerManager のパースロジックをテストする想定
        // ※メソッドは仮定です。実装時に合わせます。
        
        long t1 = parseTm("tm(1000)");
        assertEquals(1000, t1);

        long t2 = parseTm("tm(500)");
        assertEquals(500, t2);

        long t3 = parseTm("tm(0)");
        assertEquals(0, t3);
    }

    @Test
    void testParseInvalidTimeEvent() {
        assertEquals(-1, parseTm("tm()"));
        assertEquals(-1, parseTm("tm(abc)"));
        assertEquals(-1, parseTm("event"));
        assertEquals(-1, parseTm(""));
        assertEquals(-1, parseTm(null));
    }

    // 仮の実装（SimulationEngine内に実装予定のロジック）
    private long parseTm(String event) {
        if (event == null) return -1;
        if (event.startsWith("tm(") && event.endsWith(")")) {
            try {
                String val = event.substring(3, event.length() - 1);
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
    
    // 実際のタイマー動作（発火・キャンセル）のテストは、
    // Swing Timerやスレッドが絡むため、
    // TimerManagerインターフェースをMock化して行うか、
    // 統合テスト（UI操作）で確認することを推奨します。
    // ここではパースロジックの検証にとどめます。
}