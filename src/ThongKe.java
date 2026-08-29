/**
 * ThongKe — Quan ly va hien thi bang thong ke Up trong Menu Pro (Dua Mod).
 * Hien thi truc tiep thong tin HUD tren man hinh game:
 * 1. Map ID, Khu, Toa do nhan vat, so quai song tren map.
 * 2. Thoi gian up, Yen, Xu, Luong thu hoach.
 * 3. Exp % va so quai tieu diet.
 * 4. Trang thai TS Boss Uu Tien & Dem nguoc san boss.
 * 5. Dem nguoc Tu Sat khi dung im qua lau tai 1 vi tri & Vi tri check.
 */
public class ThongKe {
    public static boolean isRunning = false;
    public static long startTime = 0L;
    public static long startExp = 0L;
    public static int startYen = 0;
    public static int startXu = 0;
    public static int startLuong = 0;
    public static int kills = 0;

    /** Toggle bat/tat thong ke up trong menu */
    public static void toggle() {
        isRunning = !isRunning;
        if (isRunning) {
            Char myChar = Char.getMyChar();
            if (myChar != null) {
                resetStats(myChar);
                GameScr.gameAC("B\u1eadt th\u1ed1ng k\u00ea Up!");
            }
        } else {
            GameScr.gameAC("T\u1eaft th\u1ed1ng k\u00ea Up!");
        }
    }

    public static void resetStats(Char myChar) {
        if (myChar != null) {
            startExp = myChar.cEXP;
            startYen = myChar.yen;
            startXu = myChar.xu;
            startLuong = myChar.luong;
            startTime = System.currentTimeMillis();
            kills = 0;
        }
    }

    /** Goi khi giet quai de tang count */
    public static void addKills(int count) {
        if (count > 0) {
            kills += count;
        }
    }

    /**
     * Ve cac dong thong ke HUD len man hinh (duoc goi tu GameScr.paint)
     */
    // === CACHE: chi tinh toan lai moi 1 giay, khong moi frame ===
    private static long lastCalcTime = 0;
    private static String cachedLine1 = "";
    private static String cachedLine2 = "";
    private static String cachedLine3 = "";
    private static String cachedLine4 = "";
    private static String cachedLine5 = "";

    public static void draaw(mGraphics g) {
        ModInit.initAll();

        // Kiem tra neu khong co auto nao bat -> reset thoi gian & stats
        boolean autoActive = (Code.gameAB != null || AutoSanBoss.isRunning || AutoBossEvent.inEvent || AutoBossEvent.isEnabled);
        if (!autoActive && !isRunning && !SetAuto.hienexp) {
            startTime = 0L;
            kills = 0;
            return;
        }

        if (EcoMode.isEnabled) {
            return;
        }

        if (!SetAuto.hienexp && !isRunning) return;

        int x = 2;
        int y = 150;

        // Chi tinh toan lai moi 1 giay (tranh tao string moi 60 lan/giay)
        long now = System.currentTimeMillis();
        if (now - lastCalcTime > 1000) {
            lastCalcTime = now;
            recalcStats();
        }

        try {
            int drawY = y;
            if (cachedLine1.length() > 0) {
                mFont.tahoma_7_yellow.gameAA(g, cachedLine1, x, drawY, 0, mFont.tahoma_7_grey);
                drawY += 11;
            }
            if (cachedLine2.length() > 0) {
                mFont.tahoma_7_yellow.gameAA(g, cachedLine2, x, drawY, 0, mFont.tahoma_7_grey);
                drawY += 11;
            }
            if (cachedLine3.length() > 0) {
                mFont.tahoma_7_yellow.gameAA(g, cachedLine3, x, drawY, 0, mFont.tahoma_7_grey);
                drawY += 11;
            }
            if (cachedLine4.length() > 0) {
                mFont.tahoma_7_yellow.gameAA(g, cachedLine4, x, drawY, 0, mFont.tahoma_7_grey);
                drawY += 11;
            }
            if (cachedLine5.length() > 0) {
                mFont.tahoma_7_yellow.gameAA(g, cachedLine5, x, drawY, 0, mFont.tahoma_7_grey);
                drawY += 11;
            }
        } catch (Exception e) {}
    }

    private static void recalcStats() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null) return;

            if (startTime == 0L) {
                resetStats(myChar);
            }
            if (startTime == 0L) return;

            int cx = myChar.cx;
            int cy = myChar.cy;

            long sec = (System.currentTimeMillis() - startTime) / 1000L;
            if (sec <= 0) sec = 1L;

            int gainYen = myChar.yen - startYen;
            if (gainYen < 0) gainYen = 0;
            int gainXu = myChar.xu - startXu;
            if (gainXu < 0) gainXu = 0;
            int gainLuong = myChar.luong - startLuong;
            if (gainLuong < 0) gainLuong = 0;

            long gainExp = myChar.cEXP - startExp;
            if (gainExp < 0) gainExp = 0;

            float expPercent = 0.0f;
            try {
                long maxExp = GameScr.exps[myChar.clevel];
                if (maxExp > 0) {
                    expPercent = (float)(gainExp * 10000L / maxExp) / 100.0f;
                }
            } catch (Exception e) {}

            String timeStr = NinjaUtil.gameAB((int)sec);

            // Dem quai song tren map hien tai
            int aliveMapMobs = 0;
            try {
                int size = GameScr.vMob.size();
                for (int i = 0; i < size; i++) {
                    Object o = GameScr.vMob.elementAt(i);
                    if (o instanceof Mob) {
                        Mob m = (Mob) o;
                        if (m.hp > 0 && m.status != 0 && m.status != 1) {
                            aliveMapMobs++;
                        }
                    }
                }
            } catch (Exception e) {}

            // Tu dong luu map goc neu dang chay TS ma chua co savedMap
            if (Code.gameAB != null && !(Code.gameAB instanceof PkBoss) && !(Code.gameAB instanceof SanBossHolder) && !AutoBossEvent.inEvent) {
                if (AutoBossEvent.getSavedMap() < 0) {
                    AutoBossEvent.saveLocalState();
                }
            }

            // Dong 1: Map ID, Khu, Toa do, So quai song & Map goc TS
            int sm = AutoBossEvent.getSavedMap();
            int sz = AutoBossEvent.getSavedZone();
            String originStr = (sm > 0) ? " [G\u1ed1c: M" + sm + " K" + sz + "]" : "";
            cachedLine1 = "Map: " + TileMap.mapID + " | Khu: " + TileMap.zoneID + " (" + cx + "," + cy + ")" + originStr + " | " + aliveMapMobs + " qu\u00e1i";

            // Dong 2: Thoi gian Up, Yen, Xu, Luong
            cachedLine2 = "T: " + timeStr + " | Y: +" + gainYen + " | X: +" + gainXu + " | L: +" + gainLuong;

            // Dong 3: Exp %, Diet quai
            cachedLine3 = "Exp: +" + expPercent + "% | Di\u1ec7t: " + kills;

            // Dong 4: TS Boss Uu Tien & Dem nguoc san boss & Boss da ha
            int totalBoss = BossLog.getTotalKills();
            String bossKillStr = totalBoss > 0 ? " | H\u1ea1: " + totalBoss : "";
            if (AutoBossEvent.isEnabled) {
                String pName = AutoBossEvent.priorityName();
                if (AutoBossEvent.inEvent) {
                    long huntSec = (AutoBossEvent.eventStartTime > 0) ? (System.currentTimeMillis() - AutoBossEvent.eventStartTime) / 1000L : 0L;
                    if (huntSec < 0) huntSec = 0L;
                    long remainHuntSec = 360L - huntSec;
                    if (remainHuntSec < 0) remainHuntSec = 0L;
                    cachedLine4 = "TS Boss: \u0110ang s\u0103n [" + pName + "] (M" + TileMap.mapID + " K" + TileMap.zoneID + " - c\u00f2n " + remainHuntSec + "s/6p" + bossKillStr + ")";
                } else {
                    int secLeft = AutoBossEvent.getSecondsTillNextForPriority();
                    if (secLeft > 0 && secLeft <= AutoBossEvent.PRE_SPAWN_SECONDS) {
                        cachedLine4 = "TS Boss: Chu\u1ea9n b\u1ecb [" + pName + "] (c\u00f2n " + secLeft + "s" + bossKillStr + ")";
                    } else if (secLeft > AutoBossEvent.PRE_SPAWN_SECONDS && secLeft < Integer.MAX_VALUE) {
                        int m = secLeft / 60;
                        int s = secLeft % 60;
                        cachedLine4 = "TS Boss: B\u1eadt [" + pName + "] (Boss t\u1edbi: " + (m > 0 ? m + "p" : "") + s + "s" + bossKillStr + ")";
                    } else {
                        cachedLine4 = "TS Boss: B\u1eadt [" + pName + "] (Ch\u1edd boss..." + bossKillStr + ")";
                    }
                }
            } else {
                cachedLine4 = "TS Boss: T\u1eaft" + (totalBoss > 0 ? " (\u0110\u00e3 h\u1ea1 " + totalBoss + " boss)" : "");
            }

            // Dong 5: Dem nguoc Tu Sat khi dung im qua lau & Vi tri check
            if (AutoSuicide.isEnabled) {
                int lastX = AutoSuicide.lastX;
                int lastY = AutoSuicide.lastY;
                long idleMs = System.currentTimeMillis() - AutoSuicide.lastMoveTime;
                long timeoutSec = AutoSuicide.IDLE_TIMEOUT_MS / 1000L;
                long idleSec = idleMs / 1000L;
                long remainSec = timeoutSec - idleSec;
                if (remainSec < 0) remainSec = 0;

                if (lastX != -1 && lastY != -1 && cx == lastX && cy == lastY) {
                    cachedLine5 = "T\u1ef1 s\u00e1t: c\u00f2n " + remainSec + "s (" + idleSec + "/" + timeoutSec + "s)";
                } else {
                    cachedLine5 = "T\u1ef1 s\u00e1t: " + timeoutSec + "s (\u0110ang di chuy\u1ec3n)";
                }
            } else {
                cachedLine5 = "";
            }
        } catch (Exception e) {}
    }
}
