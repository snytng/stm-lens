package snytng.astah.plugin.stm.ui;

import java.awt.Color;

/**
 * Stm Lens の UI 状態を定義する Enum。
 * T1.1/T1.2 の設計合意に基づき、同期状態と閲覧状態を管理する。
 */
public enum UiMode {
    /** シミュレーション開始前の初期状態 */
    IDLE(new Color(245, 245, 245), false, " Ready"),

    /** 最新の状態に同期し、記録を行っているモード */
    LIVE(Color.WHITE, false, " 🔴 LIVE"),
    
    /** 過去の履歴や外部ファイルを閲覧し、保護されているモード */
    VIEWING(new Color(245, 245, 245), false, " 🔒 VIEWING"),

    /** 自動テスト実行中のモード */
    RUNNING(new Color(245, 245, 245), false, " ⏳ RUNNING");

    public final Color backgroundColor;
    public final boolean editable;
    public final String displayLabel;

    UiMode(Color backgroundColor, boolean editable, String displayLabel) {
        this.backgroundColor = backgroundColor;
        this.editable = editable;
        this.displayLabel = displayLabel;
    }
}