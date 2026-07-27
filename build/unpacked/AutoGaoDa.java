public class AutoGaoDa implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        while (isAuto) {
            try {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(1000L);
                    continue;
                }

                // 1. Chat gm23 để chuyển qua Map 23
                Code.gameAF("gm23");
                Thread.sleep(2500L);
                if (!isAuto) break;

                // 2. Chat npc62 để mở đối thoại NPC 62
                Code.gameAF("npc62");
                Service.gI().gameAH(62);
                Thread.sleep(1200L);
                if (!isAuto) break;

                // 3. Chọn nút Nhận đá (Menu option 0)
                Service.gI().gameAC(62, 0, 0);
                Service.gI().gameAC(62, 0);
                Thread.sleep(1500L);
                if (!isAuto) break;

                // 4. Chat gm26 để chuyển qua Map 26
                Code.gameAF("gm26");
                Thread.sleep(2500L);
                if (!isAuto) break;

                // 5. Chat npc63 để mở đối thoại NPC 63
                Code.gameAF("npc63");
                Service.gI().gameAH(63);
                Thread.sleep(1200L);
                if (!isAuto) break;

                // 6. Chọn nút Giao đá (Menu option 0)
                Service.gI().gameAC(63, 0, 0);
                Service.gI().gameAC(63, 0);
                Thread.sleep(1500L);

            } catch (Exception e) {
                try {
                    Thread.sleep(1000L);
                } catch (Exception ex) {}
            }
        }
    }
}
