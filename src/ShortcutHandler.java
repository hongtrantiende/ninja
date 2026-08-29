/** Xu ly softkey R Java ME va shortcut desktop. */
public final class ShortcutHandler {
    private ShortcutHandler() {
    }

    public static void handleKey(GameGraphics graphics, int keycode) {
        boolean inGame = GameCanvas.currentScreen == GameScr.instance;
        boolean chatClosed = !ChatTextField.gameAA().isShow;
        boolean rightSoftKey = keycode == -7 || keycode == -22;
        if (inGame && chatClosed && rightSoftKey && hasAnyAuto()) {
            ChatRouter.stopCurrentAuto();
            return;
        }
        if (inGame && chatClosed && keycode == 96) {
            checkKey(keycode);
            return;
        }
        graphics.gameAA(keycode);
    }

    private static boolean hasAnyAuto() {
        return Code.gameAB != null || AutoBossEvent.isEnabled || AutoSanBoss.isRunning
                || AutoPickup.isRunning || AutoLevel.isRunning;
    }

    public static void checkKey(int keycode) {
        if (GameCanvas.currentScreen != GameScr.instance || ChatTextField.gameAA().isShow)
            return;
        if (keycode == 96) {
            GameCanvas.gameBR = 0;
            if (Code.gameAB instanceof TanSat) {
                ChatRouter.stopCurrentAuto();
            } else {
                Code.gameAA(-1, (int) TileMap.mapID);
                AutoPickup.start();
                ChatRouter.onTsActivated();
                GameScr.gameAC("Bat Tan Sat + Hut VP");
            }
        }
    }
}