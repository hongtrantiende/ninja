/**
 * EcoMode — Che Do Tiet Kiem Pin & CPU cho Treo May Dem.
 * 
 * Co che:
 * - Chan ngay tai MotherCanvas.paint(): Giam 100% render do hoa, quai, hieu ung, nut ao, D-pad
 * - 0% giat lag / nhap nhay khi chuyen map / chuyen khu
 * - Hien thi Minimal HUD can giua man hinh dep mat
 * - Hien thi day du: Map, Khu, Trang thai TS Boss Uu Tien + dem nguoc, Dem nguoc Tu Sat
 * - Logic Auto van chay ngam 100% (Tan Sat, San Boss, Nhat do, Hoi sinh)
 * - Cham man hinh hoac go lenh chat "eco", "sleep", "tkp", "pin" de thoat
 */
public class EcoMode {
    public static volatile boolean isEnabled = false;
    public static long enableTime = 0L;
    public static long lastUserActionTime = System.currentTimeMillis();
    public static mGraphics ecoGraphics = null;

    /**
     * Bat / Tat Eco Mode
     */
    public static void toggle() {
        if (isEnabled) {
            stop();
        } else {
            start();
        }
    }

    public static void start() {
        isEnabled = true;
        enableTime = System.currentTimeMillis();
        GameScr.gameAC("B\u1eadt Ti\u1ebft Ki\u1ec7m Pin (Eco Mode)!");
    }

    public static void stop() {
        isEnabled = false;
        enableTime = 0L;
        GameScr.gameAC("T\u1eaft Ti\u1ebft Ki\u1ec7m Pin!");
    }

    public static void onUserAction() {
        lastUserActionTime = System.currentTimeMillis();
        if (isEnabled) {
            stop();
        }
    }

    /**
     * Hook truc tiep tu MotherCanvas.paint()
     * Neu EcoMode bat: ve man hinh den va return (KHONG ve GameCanvas, KHONG ve nut, D-pad)
     */
    public static void renderPaint(GameGraphics graphics, javax.microedition.lcdui.Graphics g) {
        ModInit.initAll();
        if (paintEco(g)) {
            return;
        }
        graphics.gameAA(g);
    }

    /**
     * Hook truc tiep tu MotherCanvas.pointerPressed()
     * Neu EcoMode bat: cham vao man hinh se thoat EcoMode ngay lap tuc
     */
    public static void handlePointer(GameGraphics graphics, int x, int y) {
        if (isEnabled) {
            stop();
            return;
        }
        graphics.gameAA(x, y);
    }

    /**
     * Ve man hinh den True Black & Minimal HUD can giua
     */
    public static boolean paintEco(javax.microedition.lcdui.Graphics g) {
        if (!isEnabled) return false;
        try {
            if (ecoGraphics == null) {
                ecoGraphics = new mGraphics(g);
            } else {
                ecoGraphics.gameAA = g;
            }

            int sw = GameCanvas.w * mGraphics.zoomLevel;
            int sh = GameCanvas.h * mGraphics.zoomLevel;
            if (sw <= 0) sw = 2000;
            if (sh <= 0) sh = 2000;

            // 1. Reset clip & Phu den 100% man hinh goc (True Black #000000)
            g.setClip(0, 0, sw, sh);
            g.setColor(0x000000);
            g.fillRect(0, 0, sw, sh);

            Char myChar = Char.getMyChar();
            if (myChar == null) return true;

            int cx = myChar.cx;
            int cy = myChar.cy;

            long elapsedSec = (System.currentTimeMillis() - enableTime) / 1000L;
            if (elapsedSec < 0) elapsedSec = 0;
            String timeStr = NinjaUtil.gameAB((int) elapsedSec);

            int hpPercent = 100;
            int mpPercent = 100;
            if (myChar.cMaxHP > 0) hpPercent = myChar.cHP * 100 / myChar.cMaxHP;
            if (myChar.cMaxMP > 0) mpPercent = myChar.cMP * 100 / myChar.cMaxMP;

            int gainYen = myChar.yen - ThongKe.startYen;
            if (gainYen < 0) gainYen = 0;
            int gainXu = myChar.xu - ThongKe.startXu;
            if (gainXu < 0) gainXu = 0;

            int totalBoss = BossLog.getTotalKills();

            // Tinh thoi gian san boss (neu dang san)
            long huntSec = 0L;
            if (AutoBossEvent.inEvent && AutoBossEvent.eventStartTime > 0) {
                huntSec = (System.currentTimeMillis() - AutoBossEvent.eventStartTime) / 1000L;
            } else if (AutoSanBoss.isRunning && AutoSanBoss.huntStartTime > 0) {
                huntSec = (System.currentTimeMillis() - AutoSanBoss.huntStartTime) / 1000L;
            }
            if (huntSec < 0) huntSec = 0;
            long remainHuntSec = 360L - huntSec;
            if (remainHuntSec < 0) remainHuntSec = 0;
            int hm = (int)(huntSec / 60);
            int hs = (int)(huntSec % 60);
            String huntTimeStr = (hm > 0 ? hm + "p" : "") + hs + "s/6p";

            // Xac dinh trang thai Auto hien tai
            String autoState = "T\u1ef1 \u0111\u1ed9ng";
            if (AutoBossEvent.inEvent || AutoSanBoss.isRunning) {
                autoState = "\u0110ang S\u0103n Boss (" + huntTimeStr + ")";
            } else if (Code.gameAB instanceof TanSat) {
                autoState = "\u0110ang T\u00e0n S\u00e1t";
            } else if (AutoLevel.isRunning) {
                autoState = "Auto Level";
            } else if (AutoBossEvent.isEnabled) {
                autoState = "Ch\u1edd TS Boss";
            }

            // Thong tin TS Boss Uu Tien
            String tsBossLine = "TS Boss: T\u1eaft";
            if (AutoBossEvent.isEnabled) {
                String pName = AutoBossEvent.priorityName();
                if (AutoBossEvent.inEvent) {
                    tsBossLine = "TS Boss: \u0110ang s\u0103n [" + pName + "] (c\u00f2n " + remainHuntSec + "s/6p)";
                } else {
                    int secLeft = AutoBossEvent.getSecondsTillNextForPriority();
                    if (secLeft > 0 && secLeft <= AutoBossEvent.PRE_SPAWN_SECONDS) {
                        tsBossLine = "TS Boss: Chu\u1ea9n b\u1ecb [" + pName + "] (c\u00f2n " + secLeft + "s)";
                    } else if (secLeft > AutoBossEvent.PRE_SPAWN_SECONDS && secLeft < Integer.MAX_VALUE) {
                        int m = secLeft / 60;
                        int s = secLeft % 60;
                        tsBossLine = "TS Boss: B\u1eadt [" + pName + "] (Boss t\u1edbi: " + (m > 0 ? m + "p" : "") + s + "s)";
                    } else {
                        tsBossLine = "TS Boss: B\u1eadt [" + pName + "] (Ch\u1edd boss...)";
                    }
                }
            }

            // Thong tin Tu Sat Dem Nguoc
            String tuSatLine = "";
            if (AutoSuicide.isEnabled) {
                int lastX = AutoSuicide.lastX;
                int lastY = AutoSuicide.lastY;
                long idleMs = System.currentTimeMillis() - AutoSuicide.lastMoveTime;
                long timeoutSec = AutoSuicide.IDLE_TIMEOUT_MS / 1000L;
                long idleSec = idleMs / 1000L;
                long remainSec = timeoutSec - idleSec;
                if (remainSec < 0) remainSec = 0;

                if (lastX != -1 && lastY != -1 && cx == lastX && cy == lastY) {
                    tuSatLine = "T\u1ef1 s\u00e1t: c\u00f2n " + remainSec + "s (" + idleSec + "/" + timeoutSec + "s)";
                } else {
                    tuSatLine = "T\u1ef1 s\u00e1t: " + timeoutSec + "s (\u0110ang di chuy\u1ec3n)";
                }
            }

            // Tinh toan can giua man hinh (Game Coordinates)
            int gameW = GameCanvas.w;
            int gameH = GameCanvas.h;
            if (gameW <= 0) gameW = sw / mGraphics.zoomLevel;
            if (gameH <= 0) gameH = sh / mGraphics.zoomLevel;

            int lineH = 13;
            int lineCount = 6;
            if (tuSatLine.length() > 0) lineCount++;
            int totalH = lineCount * lineH + 14;
            int startY = (gameH - totalH) / 2;
            if (startY < 10) startY = 10;

            int centerX = gameW / 2;

            // Reset ecoGraphics translate & clip
            int tx = ecoGraphics.gameAA();
            int ty = ecoGraphics.gameAB();
            ecoGraphics.gameAA(-tx, -ty);
            ecoGraphics.gameAE(0, 0, gameW, gameH);

            // Header (Can giua, xanh la)
            mFont.tahoma_7b_green.gameAA(ecoGraphics, "=== TI\u1ebeT KI\u1ec6M PIN & CPU (ECO MODE) ===", centerX, startY, 2);
            startY += lineH + 3;

            int sm = AutoBossEvent.getSavedMap();
            int sz = AutoBossEvent.getSavedZone();
            String originStr = (sm > 0 && (sm != TileMap.mapID || sz != TileMap.zoneID)) ? " [G\u1ed1c: M" + sm + " K" + sz + "]" : "";

            // Dong 1: Thoi gian & Vi tri (Can giua, vang)
            mFont.tahoma_7_yellow.gameAA(ecoGraphics, "Treo: " + timeStr + " | Map: " + TileMap.mapID + " - Khu: " + TileMap.zoneID + " (" + cx + "," + cy + ")" + originStr, centerX, startY, 2);
            startY += lineH;

            // Dong 2: Thong tin nhan vat (Can giua, trang)
            mFont.tahoma_7_white.gameAA(ecoGraphics, myChar.cName + " (Lv." + myChar.clevel + ") | HP: " + hpPercent + "% | MP: " + mpPercent + "%", centerX, startY, 2);
            startY += lineH;

            // Dong 3: Yen, Xu & Quai diet (Can giua, vang)
            mFont.tahoma_7_yellow.gameAA(ecoGraphics, "Y\u00ean: +" + gainYen + " | Xu: +" + gainXu + " | Di\u1ec7t: " + ThongKe.kills + " qu\u00e1i", centerX, startY, 2);
            startY += lineH;

            // Dong 4: Boss da ha & Trang thai (Can giua, vang)
            mFont.tahoma_7_yellow.gameAA(ecoGraphics, "Boss \u0111\u00e3 h\u1ea1: " + totalBoss + " con | Tr\u1ea1ng th\u00e1i: [" + autoState + "]", centerX, startY, 2);
            startY += lineH;

            // Dong 5: TS Boss Uu Tien & Dem nguoc (Can giua, vang)
            mFont.tahoma_7_yellow.gameAA(ecoGraphics, tsBossLine, centerX, startY, 2);
            startY += lineH;

            // Dong 6: Dem nguoc Tu Sat (Neu bat) (Can giua, vang)
            if (tuSatLine.length() > 0) {
                mFont.tahoma_7_yellow.gameAA(ecoGraphics, tuSatLine, centerX, startY, 2);
                startY += lineH;
            }

            startY += 4;
            // Footer (Can giua, xam)
            mFont.tahoma_7_grey.gameAA(ecoGraphics, "* Ch\u1ea1m m\u00e0n h\u00ecnh ho\u1eb7c g\u00f5 'eco' \u0111\u1ec3 t\u1eaft *", centerX, startY, 2);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
