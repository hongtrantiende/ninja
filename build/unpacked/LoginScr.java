/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.io.ConnectionNotFoundException
 *  javax.microedition.lcdui.Display
 *  javax.microedition.lcdui.Displayable
 *  javax.microedition.lcdui.Image
 *  javax.microedition.midlet.MIDlet
 */
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;

public final class LoginScr
extends mScreen
implements IActionListener {
    private TField tfUser;
    private TField tfPass;
    private TField tfRegPass;
    private TField tfEmail;
    private static LoginScr gI;
    private int focus;
    private int wC;
    private int yL;
    private int defYL;
    private boolean isCheck = false;
    private boolean isRes = false;
    private Command cmdLogin;
    private Command cmdCheck;
    private Command cmdRes;
    private Command cmdMenu;
    private static Image imgTitle;
    private int yt;
    private String[] currentTip;
    private int tipid = -1;
    private int v = 2;
    private int g = 0;
    private int ylogo = -40;
    private int dir = 1;
    public static boolean isLoggingIn;
    private String strNick = "";
    public static Account ACC;

    public static Image gameAA() {
        return imgTitle;
    }

    public final void gameAB() {
        if (RMS.gameAC("random") == null) {
            RMS.gameAA("random", SelectServerScr.gameAE());
        }
        this.yL = -50;
        this.isRes = false;
        GameScr.gH = GameCanvas.h;
        if (GameCanvas.typeBg == 2) {
            GameCanvas.gameAC(0);
        } else {
            GameCanvas.gameAC(TileMap.bgID);
        }
        super.gameAB();
        if (GameScr.instance != null) {
            GameScr.instance = null;
        }
        if (GameCanvas.menu != null) {
            GameCanvas.menu = new Menu();
        }
        GameCanvas.isGPRS = false;
        int var1 = RMS.gameAD("isSoftKey");
        if (var1 <= 0) {
            RMS.gameAA("isSoftKey", 1);
            GameScr.isTouchKey = true;
        } else if (var1 == 1) {
            GameScr.isTouchKey = true;
        } else if (var1 == 2) {
            GameScr.isTouchKey = false;
        }
        var1 = RMS.gameAD("isSound");
        if (var1 < 0) {
            RMS.gameAA("isSound", 1);
            Sound.isSound = true;
        } else if (var1 == 1) {
            Sound.isSound = true;
        } else if (var1 == 2) {
            Sound.isSound = false;
        }
        this.left = this.cmdMenu = new Command("Q.M\u1eadt Kh\u1ea9u", this, 2005, null);
    }

    public final void gameAE() {
        this.isRes = true;
        this.yL = -50;
        GameScr.gH = GameCanvas.h;
        if (GameCanvas.typeBg == 2) {
            GameCanvas.gameAC(0);
        } else {
            GameCanvas.gameAC(TileMap.bgID);
        }
        super.gameAB();
        if (GameScr.instance != null) {
            GameScr.instance = null;
        }
        if (GameCanvas.menu != null) {
            GameCanvas.menu = new Menu();
        }
        GameCanvas.isGPRS = false;
        this.left = this.cmdMenu = new Command("H\u1ee7y", this, 20051, null);
    }

    public LoginScr() {
        gI = this;
        this.isRes = false;
        TileMap.bgID = (byte)(System.currentTimeMillis() % 9L);
        if (TileMap.bgID == 5 || TileMap.bgID == 6) {
            TileMap.bgID = (byte)4;
        }
        GameScr.gameAA(true);
        GameScr.cmx = 100;
        this.defYL = GameCanvas.h > 200 ? GameCanvas.hh - 80 : GameCanvas.hh - 65;
        this.yL = -50;
        this.wC = GameCanvas.w - 30;
        if (this.wC < 135) {
            this.wC = 135;
        }
        if (this.wC > 155) {
            this.wC = 155;
        }
        this.yt = GameCanvas.hh - mScreen.ITEM_HEIGHT - 5;
        if (GameCanvas.h <= 160) {
            this.yt = 20;
        }
        this.tfUser = new TField();
        this.tfUser.name = mResources.USERNAME;
        this.tfUser.x = GameCanvas.hw - 20 - 57;
        this.tfUser.y = this.yt;
        this.tfUser.width = this.wC;
        this.tfUser.height = mScreen.ITEM_HEIGHT + 2;
        this.tfUser.isFocus = true;
        this.tfUser.gameAC(3);
        this.tfPass = new TField();
        this.tfPass.name = mResources.PASSWORD;
        this.tfPass.x = GameCanvas.hw - 20 - 57;
        this.tfPass.y = this.yt += 35;
        this.tfPass.width = this.wC;
        this.tfPass.height = mScreen.ITEM_HEIGHT + 2;
        this.tfPass.isFocus = false;
        this.tfPass.gameAC(2);
        this.tfRegPass = new TField();
        this.tfRegPass.name = mResources.REPASSWORD;
        this.tfRegPass.x = GameCanvas.hw - 20 - 57;
        this.tfRegPass.y = this.yt += 35;
        this.tfRegPass.width = this.wC;
        this.tfRegPass.height = mScreen.ITEM_HEIGHT + 2;
        this.tfRegPass.isFocus = false;
        this.tfRegPass.gameAC(2);
        this.tfEmail = new TField();
        this.tfEmail.name = "Email/S\u1ed1 di \u0111\u1ed9ng";
        this.tfEmail.x = GameCanvas.hw - 20 - 57;
        this.tfEmail.y = this.yt += 35;
        this.tfEmail.width = this.wC;
        this.tfEmail.height = mScreen.ITEM_HEIGHT + 2;
        this.tfEmail.isFocus = false;
        this.tfEmail.gameAC(3);
        this.isCheck = true;
        if (SelectServerScr.uname != null) {
            if (SelectServerScr.uname.startsWith("tmpusr")) {
                this.tfUser.gameAA("");
                this.tfPass.gameAA("");
            } else {
                this.tfUser.gameAA(SelectServerScr.uname);
                this.tfPass.gameAA(SelectServerScr.pass);
            }
        }
        this.focus = 0;
        this.cmdLogin = new Command(mResources.OK, this, 2000, null);
        this.cmdCheck = new Command(mResources.REMEMBER, this, 2001, null);
        this.cmdRes = new Command(mResources.OK, this, 2002, null);
        this.left = !this.isRes ? (this.cmdMenu = new Command("Q.M\u1eadt Kh\u1ea9u", this, 2005, null)) : (this.cmdMenu = new Command("H\u1ee7y", this, 20051, null));
        if (GameCanvas.isTouch && GameCanvas.w >= 320) {
            this.center = null;
            this.right = this.cmdLogin;
        } else {
            this.center = this.cmdLogin;
            this.right = this.tfUser.cmdClear;
        }
    }

    public static LoginScr gameAF() {
        return gI;
    }

    private static void gameAA(boolean var0) {
        GameCanvas.isGPRS = var0;
        RMS.gameAA("isGPRS", var0 ? 1 : 2);
    }

    private void gameAA(String var1) {
        GameMidlet.IP = mResources.listIP[0];
        GameCanvas.gameAB(mResources.CONNECTING);
        GameCanvas.gameAC();
        GameCanvas.gameAB(mResources.REGISTERING);
        Service.gI().gameAB();
        Service.gI().gameAB(var1, this.tfPass.gameAD(), this.tfEmail.gameAD());
    }

    private void gameAG() {
        this.tipid = GameCanvas.gameTick % mResources.tips.length;
        this.currentTip = mFont.tahoma_7_white.gameAB(mResources.tips[this.tipid], GameCanvas.w - 40);
        String var1 = this.tfUser.gameAD().toLowerCase().trim();
        String var2 = this.tfPass.gameAD().toLowerCase().trim();
        if (!(var1.equals("a") && var2.equals("a") || !var1.equals("b"))) {
            var2.equals("b");
        }
        if (var1 != null && var2 != null && !var1.equals("")) {
            if (var2.equals("")) {
                this.focus = 1;
                this.tfUser.isFocus = false;
                this.tfPass.isFocus = true;
                this.right = this.tfPass.cmdClear;
                return;
            }
            GameCanvas.gameAB(mResources.CONNECTING);
            GameCanvas.gameAC();
            GameCanvas.gameAB(mResources.LOGGING);
            Service.gI().gameAA(var1, var2, "1.8.8");
            isLoggingIn = true;
            if (this.isCheck) {
                RMS.gameAA("check", 1);
                RMS.gameAA("acc", var1);
                RMS.gameAA("pass", var2);
            } else {
                RMS.gameAA("check", 2);
                RMS.gameAA("acc", "");
                RMS.gameAA("pass", "");
            }
            this.focus = 0;
        }
    }

    public final void gameAC() {
        if (++GameScr.cmx > GameCanvas.w * 3 + 100) {
            GameScr.cmx = 100;
        }
        this.tfUser.gameAC();
        this.tfPass.gameAC();
        if (this.isRes) {
            this.tfRegPass.gameAC();
            this.tfEmail.gameAC();
        }
        if (this.defYL != this.yL) {
            this.yL += this.defYL - this.yL >> 1;
        }
        if (GameCanvas.isTouch) {
            this.center = null;
            this.right = this.isRes ? this.cmdRes : this.cmdLogin;
        } else if (this.isRes) {
            this.center = this.cmdRes;
        } else if (this.focus == 2) {
            this.center = this.cmdCheck;
            this.center.caption = this.isCheck ? mResources.UNCHECK : mResources.REMEMBER;
        } else {
            this.center = this.cmdLogin;
        }
        if (this.g >= 0) {
            this.ylogo += this.dir * this.g;
            this.g += this.dir * this.v;
            if (this.g <= 0) {
                this.dir = -this.dir;
            }
            if (this.ylogo > 0) {
                this.dir = -this.dir;
                this.g -= 2 * this.v;
            }
        }
        if (this.tipid >= 0 && GameCanvas.gameTick % 100 == 0) {
            ++this.tipid;
            if (this.tipid >= mResources.tips.length) {
                this.tipid = 0;
            }
            this.currentTip = mFont.tahoma_7_white.gameAB(mResources.tips[this.tipid], GameCanvas.w - 40);
        }
    }

    public final void gameAA(int var1) {
        if (this.tfUser.isFocus) {
            this.tfUser.gameAA(var1);
        } else if (this.tfPass.isFocus) {
            this.tfPass.gameAA(var1);
        } else if (this.isRes && this.tfRegPass.isFocus) {
            this.tfRegPass.gameAA(var1);
        } else if (this.isRes && this.tfEmail.isFocus) {
            this.tfEmail.gameAA(var1);
        }
        super.gameAA(var1);
    }

    public final void gameBM() {
        super.gameBM();
    }

    public final void gameAA(mGraphics var1) {
        block23: {
            int var2;
            block21: {
                block22: {
                    var1.gameAA(0);
                    var1.gameAD(0, 0, GameCanvas.w, GameCanvas.h);
                    GameCanvas.gameAA(var1);
                    var2 = this.tfUser.y - 45;
                    if (GameCanvas.h <= 220) {
                        var2 += 5;
                    }
                    if (GameCanvas.currentDialog != null) break block21;
                    if (this.isRes) {
                        Paint.gameAA(GameCanvas.hw - 85, this.tfUser.y - 15, 170, 150, var1);
                    } else {
                        Paint.gameAA(GameCanvas.hw - 85, this.tfUser.y - 15, 170, 90, var1);
                    }
                    Image var3 = imgTitle;
                    if (GameCanvas.h > 160 && var3 != null) {
                        var1.gameAA(var3, GameCanvas.hw, var2 - 2, 3);
                    }
                    this.tfUser.gameAA(var1);
                    this.tfPass.gameAA(var1);
                    if (this.isRes) {
                        this.tfRegPass.gameAA(var1);
                        this.tfEmail.gameAA(var1);
                    }
                    var1.gameAE(0, 0, GameCanvas.w, GameCanvas.h);
                    if (GameCanvas.w <= 200) break block22;
                    if (this.tfUser.gameAD().equals("")) {
                        if (!this.tfUser.isFocus) {
                            mFont.tahoma_7b_white.gameAA(var1, mResources.USERNAME, this.tfUser.x + 5, this.tfUser.y + 7, 0);
                        } else {
                            mFont.tahoma_7_grey.gameAA(var1, mResources.USERNAME, this.tfUser.x + 5, this.tfUser.y + 7, 0);
                        }
                    }
                    if (this.tfPass.gameAD().equals("")) {
                        if (!this.tfPass.isFocus) {
                            mFont.tahoma_7b_white.gameAA(var1, mResources.PASSWORD, this.tfPass.x + 5, this.tfPass.y + 7, 0);
                        } else {
                            mFont.tahoma_7_grey.gameAA(var1, mResources.PASSWORD, this.tfPass.x + 5, this.tfPass.y + 7, 0);
                        }
                    }
                    if (!this.isRes) break block23;
                    if (this.tfRegPass.gameAD().equals("")) {
                        if (!this.tfRegPass.isFocus) {
                            mFont.tahoma_7b_white.gameAA(var1, mResources.REPASS, this.tfRegPass.x + 5, this.tfRegPass.y + 7, 0);
                            mFont.tahoma_7b_white.gameAA(var1, mResources.PASSWORD, this.tfRegPass.x + 50, this.tfRegPass.y + 7, 0);
                        } else {
                            mFont.tahoma_7_grey.gameAA(var1, mResources.REPASS, this.tfRegPass.x + 5, this.tfRegPass.y + 7, 0);
                            mFont.tahoma_7_grey.gameAA(var1, mResources.PASSWORD, this.tfRegPass.x + 50, this.tfRegPass.y + 7, 0);
                        }
                    }
                    if (!this.tfEmail.gameAD().equals("")) break block23;
                    if (!this.tfEmail.isFocus) {
                        mFont.tahoma_7b_white.gameAA(var1, "Email/s\u1ed1 di \u0111\u1ed9ng", this.tfEmail.x + 5, this.tfEmail.y + 5, 0);
                    } else {
                        mFont.tahoma_7_grey.gameAA(var1, "Email/s\u1ed1 di \u0111\u1ed9ng", this.tfEmail.x + 5, this.tfEmail.y + 5, 0);
                    }
                    break block23;
                }
                if (this.tfUser.gameAD().equals("")) {
                    mFont.tahoma_7b_white.gameAA(var1, mResources.gameDJ, this.tfUser.x - 35, this.tfUser.y + 7, 0);
                }
                if (this.tfPass.gameAD().equals("")) {
                    mFont.tahoma_7b_white.gameAA(var1, mResources.gameDK, this.tfPass.x - 35, this.tfPass.y + 7, 0);
                }
                if (!this.isRes) break block23;
                mFont.tahoma_7b_white.gameAA(var1, mResources.gameDL, this.tfRegPass.x - 35, this.tfRegPass.y - 1, 0);
                mFont.tahoma_7b_white.gameAA(var1, mResources.gameDK, this.tfRegPass.x - 35, this.tfRegPass.y + 13, 0);
                mFont.tahoma_7b_white.gameAA(var1, "Email/s\u1ed1 di \u0111\u1ed9ng", this.tfEmail.x - 35, this.tfEmail.y + 5, 0);
                break block23;
            }
            if (this.currentTip != null) {
                for (var2 = 0; var2 < this.currentTip.length; ++var2) {
                    mFont.tahoma_7_white.gameAA(var1, this.currentTip[var2], GameCanvas.w / 2, this.tfUser.y - 15 + var2 * 10, 2, mFont.tahoma_7_grey);
                }
            }
        }
        String var4 = "1.8.8";
        if (isLoggingIn) {
            var4 = Session_ME.gameAA().gameAL;
        }
        mFont.tahoma_7_grey.gameAA(var1, var4, GameCanvas.w - 5, 5, 1);
        super.gameAA(var1);
    }

    public final void gameAD() {
        if (GameCanvas.keyPressedz[2]) {
            --this.focus;
            if (this.focus < 0) {
                this.focus = 3;
            }
        } else if (GameCanvas.keyPressedz[8]) {
            ++this.focus;
            if (this.focus > 3) {
                this.focus = 0;
            }
        }
        if (GameCanvas.keyPressedz[2] || GameCanvas.keyPressedz[8]) {
            GameCanvas.gameAJ();
            if (this.focus == 1) {
                this.tfUser.isFocus = false;
                this.tfPass.isFocus = true;
                this.tfRegPass.isFocus = false;
                this.tfEmail.isFocus = false;
                this.right = this.tfPass.cmdClear;
            } else if (this.focus == 0) {
                this.tfUser.isFocus = true;
                this.tfPass.isFocus = false;
                this.tfRegPass.isFocus = false;
                this.tfEmail.isFocus = false;
                this.right = this.tfUser.cmdClear;
            } else {
                this.tfUser.isFocus = false;
                this.tfPass.isFocus = false;
                if (this.isRes) {
                    if (this.focus == 2) {
                        this.tfRegPass.isFocus = true;
                        this.tfEmail.isFocus = false;
                        this.right = this.tfRegPass.cmdClear;
                    } else if (this.focus == 3) {
                        this.tfEmail.isFocus = true;
                        this.tfRegPass.isFocus = false;
                        this.right = this.tfEmail.cmdClear;
                    }
                }
            }
        }
        if (GameCanvas.isPointerJustRelease) {
            if (GameCanvas.gameAB(this.tfUser.x, this.tfUser.y, this.tfUser.width, this.tfUser.height)) {
                this.focus = 0;
            } else if (GameCanvas.gameAB(this.tfPass.x, this.tfPass.y, this.tfPass.width, this.tfPass.height)) {
                this.focus = 1;
            } else {
                if (this.isRes) {
                    if (GameCanvas.gameAB(this.tfRegPass.x, this.tfRegPass.y, this.tfRegPass.width, this.tfRegPass.height)) {
                        this.focus = 2;
                    } else if (GameCanvas.gameAB(this.tfEmail.x, this.tfEmail.y, this.tfEmail.width, this.tfEmail.height)) {
                        this.focus = 3;
                    }
                } else if (GameCanvas.gameAB(this.tfUser.x - 20, GameCanvas.hh + 40, 80, 20)) {
                    this.isCheck = !this.isCheck;
                }
                this.focus = 2;
            }
        }
        super.gameAD();
        GameCanvas.gameAJ();
    }

    public final void perform(int var1, Object var2) {
        try {
            switch (var1) {
                case 1002: {
                    this.isRes = true;
                    this.tfRegPass.isFocus = false;
                    this.tfEmail.isFocus = false;
                    this.tfPass.isFocus = false;
                    this.tfUser.isFocus = true;
                    this.right = this.tfUser.cmdClear;
                    this.left = new Command(mResources.CANCEL, this, 10021, null);
                    return;
                }
                case 1003: {
                    GameMidlet.instance.platformRequest("http://ninjaschool.vn");
                    return;
                }
                case 1004: {
                    MyVector var6 = new MyVector();
                    var1 = RMS.gameAD("lowGraphic");
                    if (!GameCanvas.isTouch) {
                        if (var1 == 1) {
                            var6.addElement(new Command(mResources.gameDR, this, 10041, null));
                        } else {
                            var6.addElement(new Command(mResources.gameDS, this, 10042, null));
                        }
                    }
                    var6.addElement(new Command(mResources.LANGUAGE, this, 1006, null));
                    if (GameCanvas.currentScreen == this) {
                        var6.addElement(new Command(mResources.gameBM, this, 1009, null));
                    }
                    GameCanvas.menu.gameAA(var6);
                    return;
                }
                case 1005: {
                    GameCanvas.gameAA(mResources.gameEA, new Command("3G/Wifi", this, 3000, null), new Command("GPRS", this, 3001, null));
                    return;
                }
                case 1006: {
                    GameCanvas.gameAA(mResources.gameBO, new Command(mResources.ACCEPT, this, 10061, null), new Command(mResources.NO, GameCanvas.gameAB(), 8882, null));
                    return;
                }
                case 1007: {
                    if (Sound.isSound = !Sound.isSound) {
                        RMS.gameAA("isSound", 1);
                        return;
                    }
                    RMS.gameAA("isSound", 2);
                    System.out.println("tat am thanh");
                    return;
                }
                case 1009: {
                    RMS.gameAE();
                    return;
                }
                case 2000: {
                    if (!this.tfUser.gameAD().equals("") && !this.tfPass.gameAD().equals("")) {
                        SelectServerScr.unameChange = this.tfUser.gameAD();
                        SelectServerScr.passChange = this.tfPass.gameAD();
                    }
                    GameCanvas.selectsvScr.gameAB();
                    return;
                }
                case 2001: {
                    if (this.isCheck) {
                        this.isCheck = false;
                        return;
                    }
                    this.isCheck = true;
                    return;
                }
                case 2002: {
                    if (this.tfUser.gameAD().equals("")) {
                        GameCanvas.gameAA(mResources.NOT_INPUT_USERNAME);
                        return;
                    }
                    char[] var5 = this.tfUser.gameAD().toCharArray();
                    for (var1 = 0; var1 < var5.length; ++var1) {
                        if (TField.gameAA(var5[var1])) continue;
                        GameCanvas.gameAA(mResources.NOT_SPEC_CHARACTER);
                        return;
                    }
                    if (this.tfPass.gameAD().equals("")) {
                        GameCanvas.gameAA(mResources.NOT_INPUT_PASS1);
                        return;
                    }
                    if (this.tfRegPass.gameAD().equals("")) {
                        GameCanvas.gameAA(mResources.NOT_INPUT_PASS2);
                        return;
                    }
                    if (this.tfUser.gameAD().length() < 5) {
                        GameCanvas.gameAA(mResources.USERNAME_LENGHT);
                        return;
                    }
                    if (!this.tfPass.gameAD().equals(this.tfRegPass.gameAD())) {
                        GameCanvas.gameAA(mResources.WRONG_PASSWORD);
                        return;
                    }
                    if (!this.tfEmail.gameAD().equals("")) {
                        GameCanvas.msgdlg.gameAA(mResources.REGISTER_TEXT[0] + " " + this.tfUser.gameAD() + ", " + mResources.REGISTER_TEXT[1], new Command(mResources.ACCEPT, this, 4000, null), null, new Command(mResources.NO, GameCanvas.instance, 8882, null));
                        GameCanvas.currentDialog = GameCanvas.msgdlg;
                        return;
                    }
                    GameCanvas.gameAA("B\u1ea1n ch\u01b0a nh\u1eadp Email/s\u1ed1 di \u0111\u1ed9ng, Email/s\u1ed1 di \u0111\u1ed9ng gi\u00fap b\u1ea1n l\u1ea5y l\u1ea1i m\u1eadt kh\u1ea9u khi m\u1ea5t m\u1eadt kh\u1ea9u", new Command("Ti\u1ebfp T\u1ee5c", this, 4001, null), new Command(mResources.NO, GameCanvas.instance, 8882, null));
                    return;
                }
                case 2003: {
                    GameMidlet.gameAA("http://dd.ninjaschool.vn/app/index.php?for=event&do=resetpass");
                    return;
                }
                case 2004: {
                    GameCanvas.inputDlg.gameAA(mResources.INPUT_NICK, new Command(mResources.OK, this, 20041, null), 0);
                    return;
                }
                case 2005: {
                    Account var5 = ACC;
                    Display.getDisplay((MIDlet)GameMidlet.instance).setCurrent((Displayable)var5.gameAA);
                    return;
                }
                case 3000: {
                    LoginScr.gameAA(false);
                    GameCanvas.endDlg();
                    return;
                }
                case 3001: {
                    LoginScr.gameAA(true);
                    GameCanvas.endDlg();
                    return;
                }
                case 4000: {
                    this.gameAA(this.tfUser.gameAD());
                    return;
                }
                case 4001: {
                    this.gameAA(this.tfUser.gameAD());
                    return;
                }
                case 10021: {
                    this.isRes = false;
                    this.tfRegPass.isFocus = false;
                    this.tfPass.isFocus = false;
                    this.tfUser.isFocus = true;
                    this.right = this.tfUser.cmdClear;
                    this.left = this.cmdMenu;
                    return;
                }
                case 10041: {
                    RMS.gameAA("lowGraphic", 0);
                    GameCanvas.gameAA(mResources.RESTART, 8885);
                    return;
                }
                case 10042: {
                    RMS.gameAA("lowGraphic", 1);
                    GameCanvas.gameAA(mResources.RESTART, 8885);
                    return;
                }
                case 10051: {
                    RMS.gameAA("isSoftKey", 1);
                    GameScr.isTouchKey = true;
                    return;
                }
                case 10052: {
                    RMS.gameAA("isSoftKey", 2);
                    GameScr.isTouchKey = false;
                    return;
                }
                case 10061: {
                    GameCanvas.endDlg();
                    RMS.gameAA("indLanguage", -1);
                    GameMidlet.instance.notifyDestroyed();
                    return;
                }
                case 20041: {
                    this.strNick = GameCanvas.inputDlg.tfInput.gameAD().toString();
                    GameCanvas.endDlg();
                    if (this.strNick.equals("")) {
                        GameCanvas.gameAA(mResources.NOT_INPUT_USERNAME);
                        return;
                    }
                    GameCanvas.gameAA(mResources.ASK_REG_NUM, new Command(mResources.YES, this, 200421, null), new Command(mResources.NO, this, 200422, null));
                    return;
                }
                case 20051: {
                    GameScr.gI().gameAB();
                    return;
                }
                case 20052: {
                    GameMidlet.gameAA("http://dd.ninjaschool.vn/app/index.php?for=event&do=resetpass");
                    return;
                }
                case 200041: {
                    GameCanvas.menu.showMenu = false;
                    GameMidlet.IP = "localhost";
                    GameMidlet.PORT = 14444;
                    RMS.gameAA("indServer", 2);
                    this.gameAG();
                    return;
                }
                case 200042: {
                    GameCanvas.menu.showMenu = false;
                    GameMidlet.IP = "localhost";
                    GameMidlet.PORT = 14445;
                    RMS.gameAA("indServer", 3);
                    this.gameAG();
                    return;
                }
                case 200421: {
                    GameCanvas.endDlg();
                    String var4 = this.strNick;
                    GameMidlet.IP = mResources.listIP[0];
                    GameCanvas.gameAB(mResources.CONNECTING);
                    GameCanvas.gameAC();
                    GameCanvas.gameAB(mResources.gameBG);
                    Service.gI().gameAA(var4);
                    return;
                }
                case 200422: {
                    GameCanvas.gameAA(mResources.gameAA(mResources.gameUF, this.strNick));
                }
            }
            if (var1 >= 20000 && var1 < 20000 + mResources.serverST.length) {
                var1 = mResources.serverST[var1 - 20000];
                GameCanvas.menu.showMenu = false;
                GameMidlet.IP = mResources.listIP[var1];
                GameMidlet.PORT = mResources.listPort[var1];
                GameMidlet.serverLogin = mResources.serverLoginList[var1];
                RMS.gameAA("indServer", mResources.serverST[var1]);
                this.gameAG();
            }
        }
        catch (ConnectionNotFoundException var3) {
            var3.printStackTrace();
        }
    }

    static {
        imgTitle = GameCanvas.gameAC("/tt.png");
        ACC = new Account();
        mResources.nameHienThi = "NSO Aeharuna";
        mResources.listName = new String[]{"AEharuna "};
        mResources.listIP = new String[]{"103.77.214.194"};
        mResources.listPort = new int[]{14444};
        mResources.serverLoginList = new byte[]{0, 1, 2};
        mResources.serverST = new int[]{0, 1, 2};
    }
}
