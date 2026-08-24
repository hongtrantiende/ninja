/**
 * Auto re-enter VIP "Map Up Luong" (M195) khi chet hoac mat ket noi.
 * Flow: Hoi sinh tai thon -> talk NPC 47 option 5 -> vao M195.
 * NPC VIP nam ngay map thon (cho hoi sinh), KHONG can GoMap.
 *
 * Su dung: go "tsvip" de bat/tat.
 */
public final class AutoVipMap {

    /** Flag bat/tat auto quay lai map VIP */
    public static boolean isEnabled;

    /** Map dich (Map Up Luong) */
    public static int targetMapID = 195;

    /** NPC type ID (VIP [47]) */
    public static int npcType = 47;

    /** Menu option index cho "Map Up Luong" (0-based, o thu 5 = index 4) */
    public static int menuOption = 4;

    /** Dang trong qua trinh quay lai */
    private static boolean returning;

    private AutoVipMap() {}

    /** Toggle on/off */
    public static void toggle() {
        isEnabled = !isEnabled;
        if (isEnabled) {
            GameScr.gameAC("AutoVIP: ON - T\u1ef1 v\u00e0o l\u1ea1i M" + targetMapID + " khi ch\u1ebft/disconnect");
        } else {
            returning = false;
            GameScr.gameAC("AutoVIP: OFF");
        }
    }

    /**
     * Goi tu Code.java game loop khi co Auto dang chay.
     * Check: dang o map khac targetMap + isEnabled + ko dang returning -> bat dau return.
     */
    public static void checkAndReturn() {
        if (!isEnabled || returning) return;
        if (TileMap.mapID == targetMapID) return;
        if (Code.gameAB == null) return;

        // Chi return khi nhan vat con song (da respawn xong)
        try {
            Char me = Char.getMyChar();
            if (me == null || me.statusMe == 14 || me.cHP <= 0) return;
        } catch (Exception e) { return; }

        // Bat dau return trong thread rieng
        returning = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    doReturn();
                } catch (Exception e) {
                    GameScr.gameAC("AutoVIP: L\u1ed7i - " + e.getMessage());
                } finally {
                    returning = false;
                }
            }
        }).start();
    }

    /** Quy trinh quay lai map VIP: NPC VIP o ngay map thon (cho hoi sinh) */
    private static void doReturn() {
        if (!isEnabled) return;

        // Cho nhan vat on dinh (vua respawn xong)
        sleep(2000);

        // Neu da o map target roi thi khong can lam gi
        if (TileMap.mapID == targetMapID) return;

        GameScr.gameAC("AutoVIP: T\u00ecm NPC VIP \u0111\u1ec3 v\u00e0o M" + targetMapID + "...");

        // Retry toi da 3 lan (phong truong hop NPC chua load xong)
        for (int retry = 0; retry < 3 && isEnabled; retry++) {
            if (TileMap.mapID == targetMapID) return;

            // Talk NPC VIP voi option "Map Up Luong" (o thu 5, index 4)
            // GameScr.gameAB(npcType, optionIndex, 0) — param2 = lua chon menu
            try {
                GameScr.gameAB(npcType, menuOption, 0);
            } catch (Exception e) {
                GameScr.gameAC("AutoVIP: Kh\u00f4ng t\u00ecm th\u1ea5y NPC VIP!");
                sleep(3000);
                continue;
            }

            // Cho vao map (toi da 15s)
            for (int w = 0; w < 150 && isEnabled; w++) {
                sleep(100);
                if (TileMap.mapID == targetMapID) {
                    GameScr.gameAC("AutoVIP: \u0110\u00e3 v\u00e0o M" + targetMapID + "!");
                    return;
                }
            }

            GameScr.gameAC("AutoVIP: Th\u1eed l\u1ea1i l\u1ea7n " + (retry + 2) + "...");
            sleep(2000);
        }

        if (TileMap.mapID != targetMapID) {
            GameScr.gameAC("AutoVIP: Kh\u00f4ng v\u00e0o \u0111\u01b0\u1ee3c M" + targetMapID + " sau 3 l\u1ea7n!");
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}
