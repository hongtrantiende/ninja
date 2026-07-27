public class AutoNhanDa implements Runnable {
    public static boolean isAuto = false;

    public void run() {
        while (isAuto) {
            try {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cHP <= 0) {
                    Thread.sleep(1000L);
                    continue;
                }

                // 1. Chuyển sang Map 23 (Trường Hirosaki) nếu chưa ở Map 23
                if (TileMap.mapID != 23) {
                    TileMap.GoMap(23);
                    Thread.sleep(2000L);
                    continue;
                }

                // 2. Di chuyển đến vị trí nhận đá (X: 481, Y: 168)
                myChar.cx = 481;
                myChar.cy = 168;
                Char.gameAC(481, 168);
                Thread.sleep(1000L);

                // 3. Tương tác NPC 33 (đối thoại nhận đá 62) - Giữ nguyên trong hành trang
                Service.gI().gameAH(33);
                Thread.sleep(500L);
                Service.gI().gameAC(33, 0, 0);

                Thread.sleep(2000L);
            } catch (Exception e) {
                try {
                    Thread.sleep(1000L);
                } catch (Exception ex) {}
            }
        }
    }
}
