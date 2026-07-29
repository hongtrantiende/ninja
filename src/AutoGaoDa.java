public class AutoGaoDa implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        try {
            while (isAuto) {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(10L);
                    continue;
                }

                // 1. Nhận đá từ xa (NPC 62)
                Service.gI().gameAH(62);
                Thread.sleep(10L);
                Service.gI().gameAC(62, 0, 0);
                Service.gI().gameAC(62, 0);
                GameCanvas.endDlg();
                InfoDlg.gameAB();
                Thread.sleep(10L);
                if (!isAuto) break;

                // 2. Giao đá từ xa (NPC 63 - Nút 1 / Index 0)
                Service.gI().gameAH(63);
                Thread.sleep(10L);
                Service.gI().gameAC(63, 0, 0);
                Service.gI().gameAC(63, 0);
                GameCanvas.endDlg();
                InfoDlg.gameAB();
                Thread.sleep(10L);
            }
        } catch (Exception e) {
            try {
                Thread.sleep(10L);
            } catch (Exception ex) {}
        }
    }
}
