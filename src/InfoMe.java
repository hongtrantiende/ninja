public final class InfoMe {
    public static MyVector infoWaitToShow = new MyVector();
    public static InfoItem info;
    public static int p1 = 5;
    public static int p2;
    public static int x;
    public static int strWidth;
    public static int limLeft = 2;
    public static int hI = 20;

    public static void gameAA(mGraphics g) {
        int l = limLeft;
        int h = GameCanvas.h - 23;
        int w = GameCanvas.w;

        if (GameCanvas.isTouch) {
            if (GameCanvas.w >= 450) {
                l = 130;
                w = GameCanvas.w - 260;
            } else {
                l = 80;
                w = GameCanvas.w - 160 - 10;
            }
            h = GameCanvas.h - 60;
            limLeft = l + 2;
        }

        if (info != null && (GameCanvas.currentDialog == null || GameCanvas.currentDialog.center == null)) {
            g.gameAE(0, 0, GameCanvas.w, GameCanvas.h);
            if (GameCanvas.isTouch) {
                Paint.gameAA(l, h - 4, w + 10, hI + 8, g);
            } else {
                g.gameAA(0);
                g.gameAD(l - 2, h, w + 2, hI);
            }
            g.gameAE(l, h, w, hI);
            info.f.gameAA(g, info.s, x, h + 3, 0);
        }

        // Paint Boss Info Panel Overlay
        ThongTinBoss.paint(g);
    }

    public static void gameAA() {
        if (p1 == 0) {
            x -= 2;
            if (x < limLeft) {
                p1 = 1;
                p2 = 0;
            }
        } else if (p1 == 1) {
            p2++;
            if (p2 > 10) {
                p1 = 2;
                p2 = 0;
            }
        } else if (p1 == 2) {
            p2++;
            if (p2 > info.speed) {
                p1 = 3;
                p2 = 0;
            }
        } else if (p1 == 3) {
            if (x + strWidth < limLeft + GameCanvas.w - 20) {
                x -= 6;
            } else {
                x -= 2;
            }
            if (x + strWidth < limLeft) {
                p1 = 4;
                p2 = 0;
            }
        } else if (p1 == 4) {
            p2++;
            if (p2 > 10) {
                p1 = 5;
                p2 = 0;
            }
        } else if (p1 == 5) {
            if (infoWaitToShow.size() > 0) {
                InfoItem item = (InfoItem) infoWaitToShow.firstElement();
                infoWaitToShow.removeElementAt(0);
                if (info != null && item.s.equals(info.s)) {
                    return;
                }
                info = item;
                strWidth = info.f.gameAA(info.s);
                p2 = 0;
                p1 = 0;
                x = GameCanvas.w;
            } else {
                info = null;
            }
        }
    }

    public static void gameAA(String s) {
        if (!gameAB(s)) {
            if (GameCanvas.w == 128) {
                limLeft = 1;
            }
            if (infoWaitToShow.size() > 10) {
                infoWaitToShow.removeElementAt(0);
            }
            infoWaitToShow.addElement(new InfoItem(s));
        }
    }

    public static void gameAA(String s, int speed, mFont font) {
        if (!gameAB(s)) {
            if (GameCanvas.w == 128) {
                limLeft = 1;
            }
            if (infoWaitToShow.size() > 10) {
                infoWaitToShow.removeElementAt(0);
            }
            infoWaitToShow.addElement(new InfoItem(s, font, speed));
        }
    }

    public static boolean gameAB() {
        return p1 == 5 && infoWaitToShow.size() == 0;
    }

    private static boolean gameAB(String s) {
        for (int i = 0; i < infoWaitToShow.size(); i++) {
            InfoItem item = (InfoItem) infoWaitToShow.elementAt(i);
            if (item.s.equals(s)) {
                return true;
            }
        }
        return false;
    }
}
