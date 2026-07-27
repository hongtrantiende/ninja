public class AutoNhanDa implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        try {
            while (isAuto) {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(1000L);
                    continue;
                }

                if (TileMap.mapID != 23) {
                    TileMap.GoMap(23);
                    Thread.sleep(2000L);
                    continue;
                }

                myChar.cx = 481;
                myChar.cy = 168;
                Char.gameAC(481, 168);
                Thread.sleep(1000L);

                Service.gI().gameAH(33);
                Thread.sleep(500L);
                Service.gI().gameAC(33, 0, 0);
                Thread.sleep(2000L);
            }
        } catch (Exception e) {
            try {
                Thread.sleep(1000L);
            } catch (Exception ex) {}
        }
    }
}
