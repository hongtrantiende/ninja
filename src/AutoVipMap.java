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

    /** Toggle on/off Map VIP 1 (M195) */
    public static void toggle() {
        isEnabled = !isEnabled;
        if (isEnabled) {
            targetMapID = 195;
            menuOption = 4;
            saveConfigToRMS();
            GameScr.gameAC("AutoVIP: ON - T\u1ef1 v\u00e0o l\u1ea1i M195 khi ch\u1ebft/disconnect");
        } else {
            returning = false;
            saveConfigToRMS();
            GameScr.gameAC("AutoVIP: OFF");
        }
    }

    /** Toggle on/off Map VIP 2 (M196) */
    public static void toggle2() {
        if (isEnabled && targetMapID == 195) {
            isEnabled = false;
            returning = false;
        }
        isEnabled = !isEnabled;
        if (isEnabled) {
            targetMapID = 196;
            menuOption = 5;
            saveConfigToRMS();
            GameScr.gameAC("AutoVIP2: ON - T\u1ef1 v\u00e0o l\u1ea1i M196 khi ch\u1ebft/disconnect");
        } else {
            returning = false;
            saveConfigToRMS();
            GameScr.gameAC("AutoVIP2: OFF");
        }
    }

    /** Luu config vao RMS */
    public static void saveConfigToRMS() {
        try {
            RMS.gameAA("auto_vip_map_cfg", (isEnabled ? 1 : 0) + ";" + targetMapID + ";" + menuOption);
        } catch (Exception e) {}
    }

    /** Load config tu RMS */
    public static void loadConfigFromRMS() {
        try {
            String data = RMS.gameAC("auto_vip_map_cfg");
            if (data != null && data.length() > 0) {
                int[] v = new int[3];
                int idx = 0, start = 0;
                for (int i = 0; i <= data.length() && idx < 3; i++) {
                    if (i == data.length() || data.charAt(i) == ';') {
                        v[idx++] = Integer.parseInt(data.substring(start, i).trim());
                        start = i + 1;
                    }
                }
                if (idx >= 1) isEnabled = (v[0] == 1);
                if (idx >= 2 && v[1] > 0) targetMapID = v[1];
                if (idx >= 3 && v[2] > 0) menuOption = v[2];
            }
        } catch (Exception e) {}
    }

    static {
        loadConfigFromRMS();
    }

    /**
     * Goi tu Code.java game loop khi co Auto dang chay.
     * Check: dang o map khac targetMap + isEnabled + ko dang returning -> bat dau return.
     */
    public static void checkAndReturn() {
        if (!isEnabled || returning) return;
        // Tam ngung khi TSBoss dang san boss (tranh conflict keo ve map VIP)
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
        sleep(1000);

        // Neu da o map target roi thi khong can lam gi
        if (TileMap.mapID == targetMapID) return;

        GameScr.gameAC("AutoVIP: T\u00ecm NPC VIP \u0111\u1ec3 v\u00e0o M" + targetMapID + "...");

        // Retry toi da 5 lan (phong truong hop NPC chua load xong)
        for (int retry = 0; retry < 5 && isEnabled; retry++) {
            if (TileMap.mapID == targetMapID) break;

            // Dismiss dialog/NPC menu dang mo
            try { GameCanvas.endDlg(); } catch (Exception e2) {}
            try { InfoDlg.gameAB(); } catch (Exception e3) {}
            sleep(50);

            // Goi NPC bang Service packet (khong bi block boi UI)
            try {
                Service.gI().gameAH(npcType);
                sleep(50);
                Service.gI().gameAC(npcType, menuOption, 0);
            } catch (Exception e) {
                GameScr.gameAC("AutoVIP: Kh\u00f4ng g\u1ecdi \u0111\u01b0\u1ee3c NPC!");
                sleep(3000);
                continue;
            }

            // Cho vao map (toi da 15s)
            for (int w = 0; w < 150 && isEnabled; w++) {
                sleep(100);
                if (TileMap.mapID == targetMapID) {
                    break;
                }
            }

            if (TileMap.mapID == targetMapID) break;
            GameScr.gameAC("AutoVIP: Th\u1eed l\u1ea1i l\u1ea7n " + (retry + 2) + "...");
            sleep(2000);
        }

        if (TileMap.mapID == targetMapID) {
            GameScr.gameAC("AutoVIP: \u0110\u00e3 v\u00e0o M" + targetMapID + "!");
            // Doi khu cu
            int sz = AutoBossEvent.getSavedZone();
            if (sz >= 0 && TileMap.zoneID != sz) {
                Auto.gameAA(sz);
                for (int i = 0; i < 1000 && TileMap.zoneID != sz; i++) sleep(10L);
            }
            // Di chuyen ve toa do (x, y) cu
            int sx = AutoBossEvent.getSavedX();
            int sy = AutoBossEvent.getSavedY();
            if (sx > 0 && sy > 0) {
                try {
                    Char.gameAE(sx, sy);
                    Char.getMyChar().cx = sx;
                    Char.getMyChar().cy = sy;
                    Service.gI().gameAC(sx, sy);
                    sleep(200L);
                } catch (Exception ex) {}
            }
        } else {
            GameScr.gameAC("AutoVIP: Kh\u00f4ng v\u00e0o \u0111\u01b0\u1ee3c M" + targetMapID + " sau 5 l\u1ea7n!");
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}
