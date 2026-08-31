public class AutoTAQ implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        try {
            while (isAuto) {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(10L);
                    continue;
                }

                // 1. Tự động đóng các Dialog / popup nếu có
                GameCanvas.endDlg();
                InfoDlg.gameAB();

                // 2. Tự động sắp xếp Hành trang và Rương đồ để tránh đầy rương
                Service.gI().gameAG(); // Sắp xếp hành trang (Packet -106)
                Service.gI().gameAF(); // Sắp xếp rương (Packet -107)

                // 3. Tương tác spam NPC 51 ô 1 (index 0)
                Service.gI().gameAH(51);
                Service.gI().gameAC(51, 0, 0);
                Service.gI().gameAC(51, 0);

                // 4. Đóng thông báo kết quả
                GameCanvas.endDlg();
                InfoDlg.gameAB();

                // 5. Delay spam 10ms
                Thread.sleep(10L);
            }
        } catch (Exception e) {
            try {
                Thread.sleep(10L);
            } catch (Exception ex) {}
        }
    }
}
