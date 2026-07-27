/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Canvas
 *  javax.microedition.lcdui.Graphics
 */
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public final class MotherCanvas
extends Canvas
implements Runnable {
    public static MotherCanvas instance;
    public GameGraphics gameAB;
    private int gameAD = 1;
    public static boolean gameAC;

    public MotherCanvas() {
        this.setFullScreenMode(true);
        this.gameAD = 1;
        mGraphics.zoomLevel = 1;
    }

    public static MotherCanvas gI() {
        if (instance == null) {
            instance = new MotherCanvas();
        }
        return instance;
    }

    protected final void paint(Graphics var1) {
        this.gameAB.gameAA(var1);
    }

    protected final void keyPressed(int var1) {
        this.gameAB.gameAA(var1);
    }

    protected final void keyReleased(int var1) {
        this.gameAB.gameAB(var1);
    }

    protected final void pointerDragged(int var1, int var2) {
        this.gameAB.setSize(var1 /= this.gameAD, var2 /= this.gameAD);
    }

    protected final void pointerPressed(int var1, int var2) {
        this.gameAB.gameAA(var1 /= this.gameAD, var2 /= this.gameAD);
    }

    protected final void pointerReleased(int var1, int var2) {
        this.gameAB.gameAC(var1 /= this.gameAD, var2 /= this.gameAD);
    }

    public final int gameAB() {
        return this.gameAD == 1 ? this.getHeight() : 0;
    }

    public final int gameAC() {
        return this.gameAD == 1 ? this.getWidth() : 0;
    }

    public final void run() {
        try {
            Thread.sleep(10L);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
        gameAC = true;
        while (gameAC) {
            try {
                long var1 = System.currentTimeMillis();
                this.gameAB.gameAA();
                long var3 = System.currentTimeMillis() - var1;
                try {
                    long sleepTime = Char.speedGame > 0 ? (long)Char.speedGame : 30L;
                    if (GameCanvas.currentScreen instanceof LoginScr || GameCanvas.currentScreen instanceof SelectServerScr) {
                        if (sleepTime < 30L) sleepTime = 30L;
                    } else if (sleepTime < 10L) {
                        sleepTime = 10L;
                    }
                    long delay = var3 < sleepTime ? sleepTime - var3 : 5L;
                    Thread.sleep(delay < 5L ? 5L : delay);
                }
                catch (InterruptedException interruptedException) {
                }
            }
            catch (Exception var7) {
                try {
                    Thread.sleep(30L);
                }
                catch (InterruptedException var5) {
                    var5.printStackTrace();
                }
                var7.printStackTrace();
            }
        }
    }
}
