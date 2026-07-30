/**
 * ShortcutHandler - Xử lý shortcut phím backtick (`) toggle Auto Tàn Sát
 * Được gọi từ MotherCanvas.keyPressed() sau khi xử lý phím gốc.
 */
public class ShortcutHandler {

    public static void checkKey(int keycode) {
        if (keycode == 96) { // backtick `
            // Reset gameBR để ngăn chat mở
            GameCanvas.gameBR = 0;
            // Toggle tàn sát
            if (Code.gameAB instanceof TanSat) {
                GameScr.gameAC("T\u1eaft T\u00e0n S\u00e1t");
                Code.gameAF(); // stop auto
            } else {
                GameScr.gameAC("B\u1eadt T\u00e0n S\u00e1t All");
                Code.gameAA(-1, (int)TileMap.mapID);
            }
        }
    }
}
