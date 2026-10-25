package skypass.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import javax.swing.UIManager;

/**
 * Design tokens for the whole app. Every colour, font and spacing value
 * used by the screens comes from here.
 *
 * The look: a light, flat interface. Colour is spent on data and on the
 * one primary action (international orange, the colour of windsocks and
 * flight recorders); everything else is ink on white.
 */
public final class Theme {

    // ---- Surfaces ----
    public static final Color GROUND = new Color(0xF2F4F7);   // window frame + sidebar
    public static final Color SHEET = Color.WHITE;            // the page
    public static final Color LINE = new Color(0xE4E7EC);     // 1px rules
    public static final Color PILL = new Color(0xE6E9EF);     // active nav item, selected row
    public static final Color HOVER = new Color(0xEEF1F5);

    // ---- Ink ----
    public static final Color INK = new Color(0x101828);
    public static final Color INK_2 = new Color(0x475467);
    public static final Color INK_3 = new Color(0x667085);

    // ---- Accent and status ----
    public static final Color ACCENT = new Color(0xC9440F);
    public static final Color ACCENT_SOFT = new Color(0xFBE9E1);
    public static final Color OK = new Color(0x157F4A);
    public static final Color WARN = new Color(0xB54708);
    public static final Color DANGER = new Color(0xC0262D);

    // ---- The sky (splash, route hero) and the departures board ----
    public static final Color SKY = new Color(0x0E2440);
    public static final Color SKY_INK_2 = new Color(0x98A2B3);
    public static final Color BOARD = new Color(0x0B1220);
    public static final Color BOARD_TILE = new Color(0x18233A);
    public static final Color BOARD_TEXT = new Color(0xF4EFE2);
    public static final Color BOARD_AMBER = new Color(0xF5B83D);

    // ---- Spacing scale ----
    public static final int S1 = 4, S2 = 8, S3 = 12, S4 = 16, S5 = 24, S6 = 32;
    public static final int RADIUS = 8;
    public static final int CARD_RADIUS = 14;

    private Theme() {
    }

    /** Installs FlatLaf, the bundled fonts and our colours. Call once at startup. */
    public static void install() {
        FlatInterFont.install();
        FlatJetBrainsMonoFont.install();
        FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#C9440F"));
        FlatLightLaf.setup();

        UIManager.put("Panel.background", SHEET);
        UIManager.put("Component.arc", RADIUS);
        UIManager.put("Button.arc", RADIUS);
        UIManager.put("TextComponent.arc", RADIUS);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.borderColor", new Color(0xD0D5DD));
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.focusColor", new Color(0x33C9440F, true));
        UIManager.put("TextComponent.selectionBackground", ACCENT_SOFT);
        UIManager.put("TextComponent.selectionForeground", INK);
        UIManager.put("TextField.caretForeground", ACCENT);
        UIManager.put("TextArea.caretForeground", ACCENT);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.thumb", new Color(0xD0D5DD));
        UIManager.put("ScrollBar.track", SHEET);
        UIManager.put("ComboBox.padding", new java.awt.Insets(6, 8, 6, 8));
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.selectionBackground", PILL);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("Table.selectionInactiveBackground", PILL);
        UIManager.put("Table.selectionInactiveForeground", INK);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("Table.cellFocusColor", new Color(0, 0, 0, 0));
        UIManager.put("TableHeader.background", SHEET);
        UIManager.put("TableHeader.foreground", INK_3);
        UIManager.put("TableHeader.separatorColor", SHEET);
        UIManager.put("TableHeader.bottomSeparatorColor", LINE);
        UIManager.put("TableHeader.height", 34);
        UIManager.put("ComboBox.selectionBackground", PILL);
        UIManager.put("ComboBox.selectionForeground", INK);
        UIManager.put("List.selectionBackground", PILL);
        UIManager.put("List.selectionForeground", INK);
        UIManager.put("CheckBox.icon.selectedBackground", ACCENT);
        UIManager.put("CheckBox.icon.selectedBorderColor", ACCENT);
        UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
        UIManager.put("ToolTip.background", INK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
    }

    // ---- Type ----
    public static Font font(int style, float size) {
        return new Font(FlatInterFont.FAMILY, style, Math.round(size));
    }

    public static Font body() {
        return font(Font.PLAIN, 13);
    }

    public static Font small() {
        return font(Font.PLAIN, 12);
    }

    public static Font strong() {
        return font(Font.BOLD, 13);
    }

    public static Font heading() {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 15);
    }

    public static Font title() {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, 24);
    }

    public static Font figure(float size) {
        return new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, Math.round(size));
    }

    /** The departures board is the one place a fixed-width face belongs. */
    public static Font board(float size) {
        return new Font(FlatJetBrainsMonoFont.FAMILY, Font.BOLD, Math.round(size));
    }
}
