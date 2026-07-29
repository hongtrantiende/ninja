public class AutoNhanDa implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        try {
            while (isAuto) {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(10L);
                    continue;
                }

                GameCanvas.endDlg();
                InfoDlg.gameAB();

                if (TileMap.mapID != 23) {
                    TileMap.GoMap(23);
                    Thread.sleep(200L);
                    continue;
                }

                myChar.cx = 481;
                myChar.cy = 168;
                Char.gameAC(481, 168);
                Thread.sleep(10L);

                Service.gI().gameAH(33);
                Thread.sleep(10L);
                Service.gI().gameAC(33, 0, 0);
                Service.gI().gameAC(33, 0);
                GameCanvas.endDlg();
                InfoDlg.gameAB();
                Thread.sleep(50L);
            }
        } catch (Exception e) {
            try {
                Thread.sleep(10L);
            } catch (Exception ex) {}
        }
    }
}
