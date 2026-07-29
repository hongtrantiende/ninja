public class AutoDoiDiem implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        try {
            while (isAuto) {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(10L);
                    continue;
                }

                // 1. Set vị trí XY: 3356-240 nếu chưa đúng
                if (myChar.cx != 3356 || myChar.cy != 240) {
                    myChar.cx = 3356;
                    myChar.cy = 240;
                    Char.gameAC(3356, 240);
                }

                // 2. Tự động đóng các Dialog/Thông báo OK hiện tại
                GameCanvas.endDlg();
                InfoDlg.gameAB();

                // 3. Mở menu NPC 63
                Service.gI().gameAH(63);
                Thread.sleep(10L);
                if (!isAuto) break;

                // 4. Chọn menu Nút số 2 (Đổi điểm / Đổi quà)
                Service.gI().gameAC(63, 1, 0);
                Service.gI().gameAC(63, 1);
                Thread.sleep(10L);
                if (!isAuto) break;

                // 5. Tự động đóng Dialog OK thông báo đổi xong
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
