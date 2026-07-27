public class AutoGaoDa implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        try {
            while (isAuto) {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(1000L);
                    continue;
                }

                // 1. Di chuyển sang Map 23 (Kenshin / Nhận đá)
                Code.gameAF("gm23");
                Thread.sleep(2500L);
                if (!isAuto) break;

                // 2. Tương tác NPC 62 nhận đá vào Hành trang
                Code.gameAF("npc62");
                Thread.sleep(500L);
                Service.gI().gameAH(62);
                Thread.sleep(1200L);
                if (!isAuto) break;

                Service.gI().gameAC(62, 0, 0);
                Service.gI().gameAC(62, 0);
                Thread.sleep(1500L);
                if (!isAuto) break;

                // 3. Di chuyển sang Map 26 (Giao đá)
                Code.gameAF("gm26");
                Thread.sleep(2500L);
                if (!isAuto) break;

                // 4. Tương tác NPC 63 giao đá từ Hành trang
                Code.gameAF("npc63");
                Thread.sleep(500L);
                Service.gI().gameAH(63);
                Thread.sleep(1200L);
                if (!isAuto) break;

                Service.gI().gameAC(63, 0, 0);
                Service.gI().gameAC(63, 0);
                Thread.sleep(1500L);
            }
        } catch (Exception e) {
            try {
                Thread.sleep(1000L);
            } catch (Exception ex) {}
        }
    }
}
