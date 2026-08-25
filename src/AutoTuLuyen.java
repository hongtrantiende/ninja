/**
 * Auto re-enter "Map Tu Luyen" khi chet hoac mat ket noi.
 * Flow: Hoi sinh tai thon -> talk NPC 47 option 4 -> vao Map Tu Luyen.
 * NPC VIP nam ngay map thon (cho hoi sinh), KHONG can GoMap.
 *
 * Su dung: go "tstl" de bat/tat.
 */
public final class AutoTuLuyen {

    /** Flag bat/tat auto quay lai map Tu Luyen */
    public static boolean isEnabled;

    /** Map dich (Map Tu Luyen) */
    public static int targetMapID = 192;

    /** NPC type ID (VIP [47]) */
    public static int npcType = 47;

    /** Menu option index cho "Map Tu Luyen" (0-based, o thu 4 = index 3) */
    public static int menuOption = 3;

    /** Dang trong qua trinh quay lai */
    private static boolean returning;

    private AutoTuLuyen() {}

    /** Toggle on/off */
    public static void toggle() {
        isEnabled = !isEnabled;
        if (isEnabled) {
            GameScr.gameAC("AutoTuLuyen: ON - T\u1ef1 v\u00e0o l\u1ea1i M" + targetMapID + " khi ch\u1ebft/disconnect");
        } else {
            returning = false;
            GameScr.gameAC("AutoTuLuyen: OFF");
        }
    }

    /**
     * Goi tu Code.java game loop khi co Auto dang chay.
     * Check: nhan vat dang o map thon (da hoi sinh) + isEnabled + ko dang returning -> bat dau return.
     */
    public static void checkAndReturn() {
        if (!isEnabled || returning) return;
        // Tam ngung khi TSBoss dang san boss (tranh conflict keo ve map Tu Luyen)
        if (AutoBossEvent.inEvent || AutoSanBoss.isRunning) return;
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
                    GameScr.gameAC("AutoTuLuyen: L\u1ed7i - " + e.getMessage());
                } finally {
                    returning = false;
                }
            }
        }).start();
    }

    /** Quy trinh quay lai Map Tu Luyen: NPC VIP o ngay map thon (cho hoi sinh) */
    private static void doReturn() {
        if (!isEnabled) return;

        // Cho nhan vat on dinh (vua respawn xong)
        sleep(2000);

        // Neu da o map target roi thi khong can lam gi
        if (TileMap.mapID == targetMapID) return;

        GameScr.gameAC("AutoTuLuyen: T\u00ecm NPC VIP \u0111\u1ec3 v\u00e0o M" + targetMapID + "...");

        // Retry toi da 3 lan (phong truong hop NPC chua load xong)
        for (int retry = 0; retry < 3 && isEnabled; retry++) {
            if (TileMap.mapID == targetMapID) return;

            // Talk NPC VIP voi option "Map Tu Luyen" (o thu 4, index 3)
            try {
                GameScr.gameAB(npcType, menuOption, 0);
            } catch (Exception e) {
                GameScr.gameAC("AutoTuLuyen: Kh\u00f4ng t\u00ecm th\u1ea5y NPC VIP!");
                sleep(3000);
                continue;
            }

            // Cho vao map (toi da 15s)
            for (int w = 0; w < 150 && isEnabled; w++) {
                sleep(100);
                if (TileMap.mapID == targetMapID) {
                    GameScr.gameAC("AutoTuLuyen: \u0110\u00e3 v\u00e0o M" + targetMapID + "!");
                    return;
                }
            }

            GameScr.gameAC("AutoTuLuyen: Th\u1eed l\u1ea1i l\u1ea7n " + (retry + 2) + "...");
            sleep(2000);
        }

        if (TileMap.mapID != targetMapID) {
            GameScr.gameAC("AutoTuLuyen: Kh\u00f4ng v\u00e0o \u0111\u01b0\u1ee3c M" + targetMapID + " sau 3 l\u1ea7n!");
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}
