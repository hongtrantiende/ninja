/*
 * Decompiled with CFR 0.152.
 */
public abstract class Auto {
    public boolean gameAA;
    public int mapID;
    public int zoneID;
    public boolean gameAD;
    public int gameAE;
    public int gameAF;
    private int gameAV;
    private int gameAW;
    public int gameAG;
    public long gameAH;
    public long gameAI;
    public Auto reAB;
    public static boolean gameAK;
    public static Skill skill1;
    public static boolean gameAM;
    public static MyVector gameAN;
    private static MyVector gameAX;
    public static int gameAO;
    private static boolean gameAY;
    private static long gameAZ;
    public static MyVector reAE;
    public static MyVector reAF;
    protected long gameAR = 0L;
    protected long gameAS = 0L;
    protected long gameAT = 0L;
    protected boolean gameAU = false;
    private static MyVector gameBA;
    private static long gameBB;
    public static Skill skill5;
    public static Skill skill2;
    public static Skill skill3;
    public static Skill skill4;

    public static void gameAA(Mob var0) {
        if (var0.isBoss || var0.status != 0 && var0.levelBoss != 3 && var0.maxHp != var0.getTemplate().hp) {
            if (!var0.isBoss && var0.levelBoss == 0) {
                if (var0.maxHp == 10 * var0.getTemplate().hp) {
                    var0.levelBoss = 1;
                } else {
                    if (var0.maxHp != 100 * var0.getTemplate().hp && var0.templateId != 89) {
                        return;
                    }
                    var0.levelBoss = (short)2;
                }
            }
            if (!gameAN.contains(var0)) {
                gameAN.addElement(var0);
            }
        }
    }

    public static void gameAB(Mob var0) {
        gameAN.removeElement(var0);
    }

    public static void gameAA() {
        gameAN.removeAllElements();
    }

    public static void gameAA(Char var0) {
        if (var0 != Char.getMyChar()) {
            if (gameAX.contains(var0)) {
                if (var0.cTypePk != 3 && var0.killCharId != Char.getMyChar().charID) {
                    gameAX.removeElement(var0);
                    return;
                }
            } else if (var0.cTypePk == 3 || var0.killCharId == Char.getMyChar().charID) {
                gameAX.addElement(var0);
                if (LockGame.gameAB && Res.abs(Char.getMyChar().cx - var0.cx) <= 300 && Res.abs(Char.getMyChar().cy - var0.cy) <= 300) {
                    LockGame.gameAD();
                }
            }
        }
    }

    public static void gameAB() {
        gameAX.removeAllElements();
    }

    public void gameAC() {
        this.mapID = -1;
        this.zoneID = -1;
        this.gameAD = false;
        this.reAB = null;
        this.gameAG = Char.getMyChar().yen;
        this.gameAH = Char.getMyChar().cEXP;
        this.gameAI = System.currentTimeMillis();
        this.gameAA = false;
        Code.gameAS = -1;
        Code.gameAW = 0;
        gameAK = Char.getMyChar().isHuman;
        skill1 = Char.getMyChar().myskill;
        this.gameAD();
    }

    public void gameAD() {
        gameAM = false;
        Code.gameBC = System.currentTimeMillis();
    }

    protected static boolean gameAB(Char var0) {
        return var0.cHP <= 0 || var0.statusMe == 14 || var0.statusMe == 5;
    }

    protected static boolean reAC() {
        return Auto.gameAB(Char.getMyChar());
    }

    public static void Sleep(long var0) {
        try {
            Thread.sleep(var0);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
    }

    protected final void gameAA(int var1, int var2, int var3, int var4) {
        if ((var1 < 139 || var1 > 148) && TileMap.mapID >= 139 && TileMap.mapID <= 148) {
            Auto.reAD();
        } else {
            if (TileMap.mapID != var1) {
                if (!TileMap.GoMap(var1)) {
                    if (TileMap.isLangCo(var1)) {
                        try {
                            Thread.sleep(100L);
                            return;
                        }
                        catch (InterruptedException interruptedException) {
                            // empty catch block
                        }
                    }
                    return;
                }
                if (var2 >= -1 && TileMap.zoneID != var2) {
                    try {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException interruptedException) {}
                } else {
                    try {
                        Thread.sleep(100L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                }
            }
            if (var2 == -1) {
                if (Code.gameAV) {
                    int[] var8 = Code.gameAX;
                    Code.gameAW = 0;
                    this.zoneID = var8[0];
                    Auto.gameAA(this.zoneID);
                } else {
                    this.gameAB(var2);
                }
            } else if (var2 >= 0) {
                Auto.gameAA(var2);
            }
            if (TileMap.zoneID == this.zoneID && var3 > 0 && var4 > 0 && Char.getMyChar().cx != var3 && Char.getMyChar().cy != var4) {
                if (this instanceof TuDanh || this instanceof PkBoss) {
                    Char.gameAC(var3, var4);
                    return;
                }
                this.gameAC(Auto.gameAA(var3, var4));
            }
        }
    }

    public static void reAD() {
        Char var0 = Char.getMyChar();
        if (!Char.gameAJ(37) && !Char.gameAJ(35)) {
            Npc var1 = GameScr.gameAI(13);
            if (var1 != null && Math.abs(var1.cx - var0.cx) <= 400 && Math.abs(var1.cy - var0.cy) <= 400) {
                Char.gameAC(var1.cx > 400 ? var1.cx - 400 : var1.cx + 400, var1.cy);
            }
            Service.gI().gameAE();
        } else {
            Char.gameAC(var0.cx, TileMap.pxh);
        }
        long var4 = System.currentTimeMillis();
        while (var0.cHP > 0 && System.currentTimeMillis() - var4 < 0L) {
            try {
                Thread.sleep(0L);
            }
            catch (InterruptedException interruptedException) {}
        }
    }

    protected static void gameAA(boolean var0) {
        Char var1 = Char.getMyChar();
        if (var0) {
            if (gameAY) {
                if (System.currentTimeMillis() - gameAZ < 200L) {
                    return;
                }
                gameAY = false;
            } else if (Char.DanhNhom && GameScr.vParty.size() > 0) {
                for (int var5 = 0; var5 < GameScr.vParty.size(); ++var5) {
                    Party var2 = (Party)GameScr.vParty.elementAt(var5);
                    if (var2.charId == var1.charID || var2.c == null || var2.c.cHP <= 0 || var2.c.nClass.classId != 6) continue;
                    GameScr.gameAC("Ch\u1edd h\u1ed3i sinh!");
                    gameAZ = System.currentTimeMillis();
                    gameAY = true;
                    return;
                }
            }
        }
        try {
            Thread.sleep(50L);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
        gameAN.removeAllElements();
        gameAM = false;
        LockGame.gameAA = true;
        if (Code.HoiSinhLuong && Char.getMyChar().luong > 0) {
            Service.gI().gameAL();
        } else {
            Service.gI().gameAK();
            TileMap.gameAF();
        }
        LockGame.gameAA = false;
        try {
            Thread.sleep(50L);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
    }

    protected static void gameAA(int var0) {
        if (TileMap.zoneID != var0) {
            Npc var1 = GameScr.gameAI(13);
            int var2 = -1;
            if (var1 != null && var1.statusMe != 15) {
                if (Math.abs(var1.cx - Char.getMyChar().cx) > 22 || Math.abs(var1.cy - Char.getMyChar().cy) > 22) {
                    Char.gameAC(var1.cx, var1.cy);
                    try {
                        Thread.sleep(0L);
                    }
                    catch (InterruptedException interruptedException) {}
                }
            } else {
                if (TileMap.mapID != 99 && TileMap.mapID != 103 && TileMap.mapID != 134 && TileMap.mapID != 135 && TileMap.mapID != 136 && TileMap.mapID != 137) {
                    return;
                }
                var2 = Char.gameAI(490);
                if (var2 < 0) var2 = Char.gameAI(37);
                if (var2 < 0 && (var2 = Char.gameAI(35)) < 0) {
                    return;
                }
            }
            if (System.currentTimeMillis() - gameBB < 0L) {
                return;
            }
            Service.gI().gameAA(var0, var2);
            TileMap.gameAF();
            gameBB = System.currentTimeMillis();
            try {
                Thread.sleep(0L);
                return;
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
    }

    protected final void gameAB(int var1) {
        if (!this.gameAA || Code.gameAH == null || Char.getMyChar().cName.equals(Code.gameAH)) {
            GameScr var2 = GameScr.gI();
            Npc var3 = GameScr.gameAI(13);
            int var4 = -1;
            if (var3 != null && var3.statusMe != 15) {
                if (Math.abs(var3.cx - Char.getMyChar().cx) > 22 || Math.abs(var3.cy - Char.getMyChar().cy) > 22) {
                    Char.gameAC(var3.cx, var3.cy);
                    try {
                        Thread.sleep(0L);
                    }
                    catch (InterruptedException interruptedException) {}
                }
            } else {
                if (TileMap.mapID != 99 && TileMap.mapID != 103 && TileMap.mapID != 134 && TileMap.mapID != 135 && TileMap.mapID != 136 && TileMap.mapID != 137) {
                    this.zoneID = TileMap.zoneID;
                    gameBB = System.currentTimeMillis();
                    return;
                }
                var4 = Char.gameAI(490);
                if (var4 < 0) var4 = Char.gameAI(37);
                if (var4 < 0 && (var4 = Char.gameAI(35)) < 0) {
                    this.zoneID = TileMap.zoneID;
                    gameBB = System.currentTimeMillis();
                    return;
                }
            }
            if (System.currentTimeMillis() - gameBB < 0L) {
                return;
            }
            Service.gI().gameAE();
            LockGame.gameAE();
            int var9 = -1;
            if (var1 < 0) {
                var1 = var2.zones.length - 1;
            } else if (var1 >= var2.zones.length) {
                var1 = 0;
            }
            if (this instanceof TaThu) {
                var9 = (var1 / 5 + 1) * 5 % var2.zones.length;
            } else if (!Char.DanhQuai) {
                var9 = (var1 + 1) % var2.zones.length;
            } else {
                int var5 = -1;
                int var6 = (var1 + 1) % var2.zones.length;
                while (var6 != var1) {
                    if (var5 == -1 || var2.zones[var6] < var5) {
                        var9 = var6;
                        var5 = var2.zones[var6];
                    }
                    var6 = (var6 + 1) % var2.zones.length;
                }
            }
            Service.gI().gameAA(var9, var4);
            this.zoneID = var9;
            TileMap.gameAF();
            if (this.gameAL()) {
                Service.gI().gameAK("khu" + var9);
            }
            gameBB = System.currentTimeMillis();
            try {
                Thread.sleep(0L);
                return;
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
    }

    private static boolean gameAA(Mob var0, int var1) {
        return !(var0.templateId == 202 && var0.status == 8 || var1 >= 0 && var0.templateId != var1);
    }

    private static boolean gameAC(int var0, int var1) {
        return var1 < 0 || var0 == 0 && (var1 & 1) > 0 || var0 == 1 && (var1 & 2) > 0 || var0 == 2 && (var1 & 4) > 0 || var0 == 3 && (var1 & 8) > 0;
    }

    public final int gameAA(boolean var1, boolean var2, boolean var3) {
        if (this.gameAD) {
            return -1;
        }
        int var4 = 0;
        if (var1) {
            var4 = 1;
        }
        if (var2) {
            var4 |= 2;
        }
        if (var3) {
            var4 |= 4;
        }
        return var4;
    }

    protected static boolean gameAA(Char var0, Char var1) {
        return var1.statusMe != 14 && var1.statusMe != 5 && var1.statusMe != 15 && (var1.cTypePk == 3 || var0.cTypePk == 3 || var1.cTypePk == 1 && var0.cTypePk == 1 || var1.cTypePk == 5 && var0.cTypePk == 4 || var1.cTypePk == 4 && var0.cTypePk == 5 || var0.killCharId >= 0 && var0.killCharId == var1.charID || var0.testCharId >= 0 && var0.testCharId == var1.charID || var1.killCharId >= 0 && var1.killCharId == var0.charID);
    }

    protected final void gameAC(Mob var1) {
        if (var1 != null) {
            Char var4 = Char.getMyChar();
            if (var4 == null || var4.statusMe == 14 || var4.cHP <= 0) return;

            int var2 = var1.xFirst;
            int var3 = var1.yFirst;
            if (TileMap.mapID == 35) {
                if (var1.xFirst == 1428 && var1.yFirst == 528) {
                    var2 = 1452;
                    var3 = 552;
                } else if (var1.xFirst == 1284 && var1.yFirst == 528) {
                    var2 = 1308;
                    var3 = 552;
                } else if (var1.xFirst == 1836 && var1.yFirst == 648) {
                    var2 = 1812;
                    var3 = 672;
                }
            } else if (TileMap.mapID == 37) {
                if ((var1.xFirst == 876 || var1.xFirst == 900) && var1.yFirst == 408) {
                    var2 = 900;
                    var3 = 432;
                } else if ((var1.xFirst == 828 || var1.xFirst == 852) && var1.yFirst == 360) {
                    var2 = 852;
                    var3 = 384;
                } else if ((var1.xFirst == 924 || var1.xFirst == 876) && var1.yFirst == 624) {
                    var2 = 924;
                    var3 = 648;
                } else if (var1.xFirst == 732 && var1.yFirst == 600 || var1.xFirst == 756 && var1.yFirst == 576) {
                    var2 = 756;
                    var3 = 600;
                }
            }

            int dist = Res.abs(var4.cx - var2) + Res.abs(var4.cy - var3);
            boolean moved = false;
            // Neu o gan (< 100px), thu di chuyen binh thuong truoc
            if (dist <= 100) {
                try {
                    moved = Char.gameAD(var2, var3);
                } catch (Exception e) {}
            }
            // Neu quai o xa hoac di chuyen binh thuong that bai (quai bay, dia hinh chan) -> Teleport ngay!
            if (!moved) {
                int groundY = TileMap.gameAD(var2, var3);
                int targetY = (groundY > 0 && Math.abs(groundY - var3) <= 150) ? groundY : var3;
                try {
                    Char.gameAC(var2, targetY);
                    var4.cx = var2;
                    var4.cy = targetY;
                    Service.gI().gameAC(var2, targetY);
                    moved = true;
                } catch (Exception e) {}
            }

            if (moved) {
                this.gameAV = this.gameAE;
                this.gameAW = this.gameAF;
                this.gameAE = var4.cx;
                this.gameAF = var4.cy;
                var4.mobFocus = var1;
                try {
                    Thread.sleep(0L);
                    return;
                }
                catch (InterruptedException var5) {
                    return;
                }
            }
            var4.mobFocus = var1;
        }
    }

    protected static void gameAC(Char var0) {
        if (var0 != null) {
            Char var1 = Char.getMyChar();
            Char.gameAC(var0.cx, TileMap.gameAD(var0.cx, var0.cy));
            var1.charFocus = var0;
            try {
                Thread.sleep(1L);
                return;
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
    }

    public static void gameAA(SkillPaint var0) {
        // Skip hieu ung khi dang auto (giam lag)
        if (Code.timBG) return;
        if (reAE.size() > 0 || reAF.size() > 0) {
            int var2;
            EffectPaint[] var1 = new EffectPaint[reAE.size() + reAF.size()];
            for (var2 = 0; var2 < reAE.size(); ++var2) {
                var1[var2] = new EffectPaint();
                var1[var2].effCharPaint = GameScr.efs[var0.id - 1];
                var1[var2].eMob = (Mob)reAE.elementAt(var2);
            }
            for (var2 = 0; var2 < reAF.size(); ++var2) {
                var1[var2 + Auto.reAE.size()] = new EffectPaint();
                var1[var2 + Auto.reAE.size()].effCharPaint = GameScr.efs[var0.id - 1];
                var1[var2 + Auto.reAE.size()].eChar = (Char)reAF.elementAt(var2);
            }
            if (var1.length > 1) {
                EPosition var4 = new EPosition();
                if (var1[0].eMob != null) {
                    var4 = new EPosition(var1[0].eMob.x, var1[0].eMob.y);
                } else if (var1[0].eChar != null) {
                    var4 = new EPosition(var1[0].eChar.cx, var1[0].eChar.cy);
                }
                MyVector var5 = new MyVector();
                for (int var3 = 1; var3 < var1.length; ++var3) {
                    if (var1[var3].eMob != null) {
                        var5.addElement(new EPosition(var1[var3].eMob.x, var1[var3].eMob.y));
                    } else if (var1[var3].eChar != null) {
                        var5.addElement(new EPosition(var1[var3].eChar.cx, var1[var3].eChar.cy));
                    }
                    if (var3 > 5) break;
                }
                Lightning.gameAA(var5, var4, true, Char.getMyChar().gameAW());
            }
            Char.getMyChar().effPaints = var1;
        }
    }

    private boolean gameAL() {
        return this.gameAA && GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID;
    }

    protected final boolean gameAG() {
        return this.gameAA && GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId != Char.getMyChar().charID;
    }

    public void gameAM() {
        if (Code.gameAV && Code.gameAX != null && Code.gameAX.length > 0) {
            Code.gameAW = (Code.gameAW + 1) % Code.gameAX.length;
            this.zoneID = Code.gameAX[Code.gameAW];
            Auto.gameAA(this.zoneID);
            if (this.gameAL()) {
                Service.gI().gameAK("khu" + this.zoneID);
            }
            return;
        }
        this.gameAB((int)TileMap.zoneID);
    }

    /**
     * Dem so quai song tren map hien tai.
     */
    private static int countAliveMobs() {
        int count = 0;
        try {
            int size = GameScr.vMob.size();
            for (int i = 0; i < size; i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob mob = (Mob) o;
                    if (mob.hp > 0 && mob.status != 0 && mob.status != 1) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {}
        return count;
    }

    /*
     * Unable to fully structure code
     */
    private boolean gameAA(int var1, int var2, int var3) {
        if (var1 >= 4) {
            return false;
        }
        int var4;
        Mob var5;
        boolean var6;
        for (var4 = 0; var4 < Auto.gameAN.size(); ++var4) {
            var5 = (Mob)Auto.gameAN.elementAt(var4);
            if (var5.levelBoss == 0 || var5.hp <= 0 || var5.status == 0) {
                Auto.gameAN.removeElement(var5);
                --var4;
                continue;
            }
            if (var5.levelBoss == 3) {
                var6 = !(this instanceof TaThu) && !(this instanceof TuDanh);
            } else {
                var6 = var5.isBoss && (var1 & 6) != 6 || var5.levelBoss == 1 && (var1 & 2) == 0 || var5.levelBoss == 2 && (var1 & 4) == 0;
            }
            if (!var6 || Res.abs(var2 - var5.xFirst) > 200 || Res.abs(var3 - var5.yFirst) > 100) continue;
            return true;
        }
        return false;
    }

    private boolean gameAD(int var1, int var2) {
        if (Char.NePk && !(this instanceof TaThu)) {
            for (int var3 = 0; var3 < gameAX.size(); ++var3) {
                Char var4 = (Char)gameAX.elementAt(var3);
                if (Auto.gameAB(var4) || Res.abs(var1 - var4.cx) > 300 || Res.abs(var2 - var4.cy) > 300) continue;
                return true;
            }
            return false;
        }
        return false;
    }

    protected static Mob gameAA(int var0, int var1) {
        Mob var2 = null;
        Char var3 = Char.getMyChar();
        int var4 = var0 - var3.gameAD() - 10;
        int var5 = var0 + var3.gameAD() + 10;
        int var6 = var1 - var3.gameAE() - (var3.nClass.classId != 0 && var3.nClass.classId != 1 && var3.nClass.classId != 3 && var3.nClass.classId != 5 ? 0 : 40);
        int var12 = var1 + var3.gameAE();
        if (var12 > var1 + 30) {
            var12 = var1 + 30;
        }
        int var7 = -1;
        for (int var8 = 0; var8 < GameScr.vMob.size(); ++var8) {
            int var11;
            Mob var9 = (Mob)GameScr.vMob.elementAt(var8);
            int var10 = Math.abs(var0 - var9.x);
            int n = var10 = var10 > (var11 = Math.abs(var1 - var9.y)) ? var10 : var11;
            if (var4 > var9.x || var9.x > var5 || var6 > var9.y || var9.y > var12 || var9.status == 0 || var9.status == 1 || var7 != -1 && var10 >= var7) continue;
            var2 = var9;
            var7 = var10;
        }
        return var2;
    }

    protected final void gameAA(int var1, boolean var2) {
        if (Code.gameAS < 0 || Code.gameAS >= Code.gameAT.size()) {
            Code.gameAS = 0;
        }
        while (true) {
            int var3 = (Integer)Code.gameAT.elementAt(Code.gameAS);
            int var4 = (Integer)Code.gameAU.elementAt(Code.gameAS);
            Mob var5 = Auto.gameAA(var3, var4);
            if (!(this.gameAA(var1, var3, var4) || this.gameAD(var3, var4) || var5 == null || this.gameAA(var1, var5.x, var5.y))) {
                this.gameAV = Char.getMyChar().cx;
                this.gameAW = Char.getMyChar().cy;
                Char.gameAC(var3, var4);
                Char.getMyChar().mobFocus = var5;
                Service.gI().gameAB(var5.mobId);
                try {
                    Thread.sleep(1L);
                    return;
                }
                catch (InterruptedException var6) {
                    return;
                }
            }
            if (++Code.gameAS != Code.gameAT.size()) continue;
            Code.gameAS = 0;
            if (!Char.ChuyenMapHetQuai || !var2) continue;
            this.gameAM();
        }
    }

    protected Mob gameAA(Char var1, int var2, int var3, Char var4, boolean var5) {
        Mob var22;
        if (Code.gameAR && Code.gameAT.size() > 0) {
            this.gameAA(var3, var5);
            return Auto.gameAA(var1.cx, var1.cy);
        }

        int var6 = var3;
        int var7 = var2;
        var3 = var1.cy;
        var2 = var1.cx;
        Auto var21 = this;
        int var8 = -1;
        int var9 = -1;
        int var10 = -1;
        Mob var11 = null;
        MyVector var12 = GameScr.vMob;
        int var13 = 0;
        while (true) {
            if (var13 >= var12.size()) {
                var22 = var11;
                break;
            }
            Mob var14 = (Mob)var12.elementAt(var13);
            if (var14 != null && var14.hp > 0 && var14.status != 0 && var14.status != 1 && Auto.gameAA(var14, var7) && Auto.gameAC(var14.levelBoss, var6) && (var4 == null || var4.charID == Char.getMyChar().charID || Res.gameAA(var14.xFirst, var14.yFirst, var4.cx, var4.cy) <= 1000) && !var21.gameAA(var6, var14.x, var14.y) && !var21.gameAD(var14.x, var14.y)) {
                if (var21.gameAD) {
                    if (var21.mapID != 157 && var21.mapID != 158 && var21.mapID != 159) {
                        if (var8 == -1 || var14.levelBoss < var10 || var14.yFirst < var8 || var14.yFirst == var8 && var14.xFirst < var9) {
                            var10 = var14.levelBoss;
                            var8 = var14.yFirst;
                            var9 = var14.xFirst;
                            var11 = var14;
                        }
                    } else if (var14.isBoss) {
                        var22 = var14;
                        break;
                    }
                } else if (Code.gameAN == -1 || Res.gameAA(Code.gameAO, Code.gameAP, var14.xFirst, var14.yFirst) <= Code.gameAN) {
                    int var18;
                    MyVector var15 = var12;
                    Mob var16 = var14;
                    int var17 = 0;
                    for (var18 = 0; var18 < var15.size(); ++var18) {
                        Mob var19 = (Mob)var15.elementAt(var18);
                        if (var19 == null || var19.hp <= 0 || var19.status == 0 || var19.status == 1 || !Auto.gameAA(var16, var7) || !Auto.gameAC(var16.levelBoss, var6) || Res.abs(var19.x - var16.x) > 100 || Res.abs(var19.y - var16.y) > 50) continue;
                        ++var17;
                    }
                    if (var17 > Auto.skill1.maxFight) {
                        var17 = Auto.skill1.maxFight;
                    }
                    var17 = var16.levelBoss << 4 | var17 & 0xF;
                    int n = var18 = var4 != null && var4.charID != Char.getMyChar().charID ? Res.gameAA(var4.cx, var4.cy, var14.xFirst, var14.yFirst) : Res.gameAA(var2, var3, var14.xFirst, var14.yFirst);
                    if (var17 > var10 || var17 == var10 && var18 < var8) {
                        var10 = var17;
                        var8 = var18;
                        var11 = var14;
                    }
                }
            }
            ++var13;
        }
        if (var22 != null) {
            this.gameAC(var22);
            return var22;
        }
        if (System.currentTimeMillis() - this.gameAR > 100L && !this.gameAH()) {
            if (this.gameAD) {
                int var23 = TileMap.gameAH(TileMap.mapID);
                if (var23 >= 0) {
                    this.mapID = var23;
                }
                this.gameAF = -1;
                this.gameAE = -1;
                try {
                    Thread.sleep(5L);
                }
                catch (InterruptedException interruptedException) {}
            } else if (var5 && Char.ChuyenMapHetQuai) {
                this.gameAM();
            }
        }
        return null;
    }

    protected final Char gameAA(Char var1, int var2) {
        for (int var3 = 0; var3 < GameScr.vCharInMap.size(); ++var3) {
            Char var4 = (Char)GameScr.vCharInMap.elementAt(var3);
            if (var4 == null || Auto.gameAB(var4) || this.gameAA(var2, var4.cx, var4.cy) || this.gameAD(var4.cx, var4.cy) || Code.gameAD(var4.cName) || !SavePk.gameAC(var4.cName) || var4.cTypePk != 1 && var4.killCharId != var1.charID && var1.cPk >= 15) continue;
            return var4;
        }
        return null;
    }

    protected static Char gameAD(Char var0) {
        Char var1 = var0;
        int var2 = var0.cy;
        int var3 = var0.cx;
        var0 = null;
        Char var4 = Char.getMyChar();
        int var5 = var3 - var4.gameAD() - 10;
        int var6 = var3 + var4.gameAD() + 10;
        int var7 = var2 - var4.gameAE() - (var4.nClass.classId != 0 && var4.nClass.classId != 1 && var4.nClass.classId != 3 && var4.nClass.classId != 5 ? 0 : 40);
        int var8 = var2 + var4.gameAE() + (var4.nClass.classId != 0 && var4.nClass.classId != 1 && var4.nClass.classId != 3 && var4.nClass.classId != 5 ? 0 : 40);
        int var9 = -1;
        for (int var10 = 0; var10 < GameScr.vCharInMap.size(); ++var10) {
            int var13;
            Char var11 = (Char)GameScr.vCharInMap.elementAt(var10);
            int var12 = Math.abs(var3 - var11.cx);
            int n = var12 = var12 > (var13 = Math.abs(var2 - var11.cy)) ? var12 : var13;
            if (var5 > var11.cx || var11.cx > var6 || var7 > var11.cy || var11.cy > var8 || Auto.gameAB(var11) || !Auto.gameAA(var4, var11) || Code.gameAD(var11.cName) || var9 != -1 && var12 >= var9) continue;
            var0 = var11;
            var9 = var12;
        }
        var1.charFocus = var0;
        return var1.charFocus;
    }

    protected final void gameAB(int var1, int var2) {
        Char var3 = Char.getMyChar();
        if (!Auto.gameAJ()) {
            boolean var8;
            Char var4 = this.gameAA && GameScr.vParty.size() > 0 ? ((Party)GameScr.vParty.firstElement()).c : null;
            boolean var5 = !this.gameAA || Code.gameAH == null || var3.cName.equals(Code.gameAH) && LockGame.gameBH();
            Mob var6 = var3.mobFocus;
            Char var7 = var3.charFocus;
            if (Code.gameBO && (var7 == null || Code.gameAD(var7.cName) || !SavePk.gameAC(var7.cName) && !Auto.gameAA(var3, var7)) && (var7 = this.gameAA(var3, var2)) == null) {
                var7 = Auto.gameAD(var3);
            }
            boolean bl = var8 = var7 != null && SavePk.gameAC(var7.cName);
            if (var7 == null && this.gameAU) {
                Service.gI().gameAZ(0);
                this.gameAU = false;
            }
            if (Code.gameBO && var3.cPk >= 5 && System.currentTimeMillis() - this.gameAT > 5000L) {
                Item var9 = Char.gameAF(257);
                if (var9 != null && var9.template.id == 257) {
                    Service.gI().useItem(var9.indexUI);
                }
                this.gameAT = System.currentTimeMillis();
            }
            if (Code.gameAR && Code.gameAT.size() > 0 && Code.gameAS < 0) {
                this.gameAA(var2, var5);
                return;
            }
            boolean var21 = false;
            if (this.gameAA(var2, var3.cx, var3.cy) || this.gameAD(var3.cx, var3.cy) || var6 != null && this.gameAA(var2, var6.x, var6.y)) {
                try {
                    Thread.sleep(500L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                GameScr.gameAC("N\u00e9");
                if (Char.ChuyenMapHetQuai && var5) {
                    this.gameAM();
                    var21 = true;
                } else {
                    var21 = false;
                }
                if (var21) {
                    return;
                }
                var21 = true;
                var6 = null;
            }
            if (var6 == null || var6.hp <= 0 || var6.status == 0 || var6.status == 1 || !Auto.gameAA(var6, var1) || !Auto.gameAC(var6.levelBoss, var2) || System.currentTimeMillis() - this.gameAR > 1500L) {
                var6 = this.gameAA(var3, var1, var2, var4, var5);
            }
            if (var6 == null && var21 && this.gameAV > 0 && this.gameAW > 0) {
                Char.gameAC(this.gameAV, this.gameAW);
            }
            if (Char.DanhNhom && GameScr.vParty.size() > 0 && var3.nClass.classId == 6 && var3.cHP > 0) {
                for (int var24 = 0; var24 < var3.vSkillFight.size(); ++var24) {
                    Skill var10 = (Skill)var3.vSkillFight.elementAt(var24);
                    if (var10 == null || var10.template.type != 4) continue;
                    if (var10.gameAA()) break;
                    for (int var17 = 0; var17 < GameScr.vParty.size(); ++var17) {
                        Party var11 = (Party)GameScr.vParty.elementAt(var17);
                        if (var11.charId == var3.charID || var11.c == null || var11.c.cHP > 0) continue;
                        Char var18 = var11.c;
                        if (Math.abs(var3.cx - var18.cx) > 50 || Math.abs(var3.cy - var18.cy) > 50) {
                            Char.gameAC(var18.cx, var18.cy);
                        }
                        try {
                            Thread.sleep(500L);
                        }
                        catch (InterruptedException interruptedException) {
                            // empty catch block
                        }
                        Service.gI().gameAX(var11.charId);
                        var10.lastTimeUseThisSkill = System.currentTimeMillis();
                        var10.paintCanNotUseSkill = true;
                        if (!Code.timBG) {
                            var3.gameAB(GameScr.sks[var10.template.id], 0);
                        }
                        try {
                            Thread.sleep(100L);
                            return;
                        }
                        catch (InterruptedException var13) {
                            return;
                        }
                    }
                    break;
                }
            }
            if (Char.SanTaTl && !this.gameAD && (var6 == null || var6.levelBoss == 0 && (var2 & 6) != 0)) {
                var21 = (var2 & 2) != 0;
                var5 = (var2 & 4) != 0;
                for (int var17 = 0; var17 < gameAN.size(); ++var17) {
                    Mob var22 = (Mob)gameAN.elementAt(var17);
                    if (var22.hp <= 0 || var22.status == 0 || var22.status == 1 || this.gameAA(var2, var22.x, var22.y) || this.gameAD(var22.x, var22.y) || !Auto.gameAA(var22, var1) || (!var21 || var22.levelBoss != 1) && (!var5 || var22.levelBoss != 2)) continue;
                    var6 = var22;
                    this.gameAC(var22);
                    break;
                }
            }
            if (skill1 != null) {
                Mob var20;
                Skill var26 = skill1;
                if (var26.gameAA() && (Char.DungHoTro || this instanceof As20)) {
                    block11: for (int var25 = 0; var25 < var3.vSkillFight.size(); ++var25) {
                        Skill var19 = (Skill)var3.vSkillFight.elementAt(var25);
                        if (var19 == null || System.currentTimeMillis() - var19.lastTimeUseThisSkill < (long)var19.coolDown - 300L) continue;
                        if (var19.template.type == 2) {
                            if ((var3.gameAD != null || !Char.DungPhanThan) && var19.template.id >= 67 && var19.template.id <= 72 || !Char.KhienMana && var19.template.id == 31 || var19.template.id == 15 && Char.DotQuai && (var3.cHP >= var3.cMaxHP * Char.aHpValue / 100 || !var3.isHuman) || var19.template.id == 6 && !var3.isHuman) continue;
                            int var23 = (int)(System.currentTimeMillis() / 1000L);
                            for (int var17 = 0; var17 < var3.vEff.size(); ++var17) {
                                Effect var12 = (Effect)var3.vEff.elementAt(var17);
                                if (var12 != null && (var12.template.iconId == var19.template.iconId || var19.template.id == 58 && var12.template.type == 7) && var12.timeLenght - (var23 - var12.timeStart) >= 2) continue block11;
                            }
                        } else if (!(var19.template.type == 3 && var6 != null && var6.levelBoss == 0 && var6.hp > var6.maxHp / 2 ? var19.template.id != 4 || Char.DotQuai && var3.cHP < var3.cMaxHP * Char.aHpValue / 100 : !(var19.template.id != 7 && var19.template.id != 16 && var19.template.id != 25 && var19.template.id != 34 && var19.template.id != 43 || var6 == null || var6.levelBoss == 0 && var6.hp < var6.maxHp / 2 || (var19.template.id == 7 || var19.template.id == 16) && var6.isFire || (var19.template.id == 25 || var19.template.id == 34) && !var6.isIce || var19.template.id == 43 && !var6.isWind))) {
                            continue;
                        }
                        var26 = var19;
                        try {
                            Thread.sleep(100L);
                            break;
                        }
                        catch (InterruptedException interruptedException) {
                            // empty catch block
                            break;
                        }
                    }
                }
                if (var26.template.type == 2) {
                    if (System.currentTimeMillis() - var26.lastTimeUseThisSkill >= (long)var26.coolDown) {
                        var26.lastTimeUseThisSkill = System.currentTimeMillis();
                        Service.gI().gameAG(var26.template.id);
                        Service.gI().gameAR();
                        if (!Code.timBG) {
                            var3.gameAB(GameScr.sks[var26.template.id], 0);
                        }
                    } else {
                        var26.paintCanNotUseSkill = true;
                    }
                } else if (!Code.gameBO || var7 == null || Auto.gameAB(var7) || !var8 && !Auto.gameAA(var3, var7)) {
                    int var23;
                    if (var6 == null || var1 != -1 && var6.templateId != var1 || !Auto.gameAC(var6.levelBoss, var2)) {
                        return;
                    }
                    if (!(var26.template.type != 1 && var26.template.type != 3 || Res.abs(var3.cx - var6.xFirst) <= var26.dx + 30 && Res.abs(var3.cy - var6.yFirst) <= var26.dy + 30)) {
                        // Teleport truc tiep den quai de danh neu con ngoai tam thay vi bo target va dung im
                        int groundY = TileMap.gameAD(var6.xFirst, var6.yFirst);
                        int targetY = (groundY > 0 && Math.abs(groundY - var6.yFirst) <= 150) ? groundY : var6.yFirst;
                        try {
                            Char.gameAC(var6.xFirst, targetY);
                            var3.cx = var6.xFirst;
                            var3.cy = targetY;
                            Service.gI().gameAC(var6.xFirst, targetY);
                        } catch (Exception e) {}
                        if (Res.abs(var3.cx - var6.xFirst) > var26.dx + 60 || Res.abs(var3.cy - var6.yFirst) > var26.dy + 60) {
                            var3.mobFocus = null;
                            var6 = null;
                            return;
                        }
                    }
                    int var25 = var26.dx;
                    int var17 = var26.dy;
                    reAE.removeAllElements();
                    reAF.removeAllElements();
                    reAE.addElement(var6);
                    for (var23 = 0; var23 < GameScr.vMob.size() && reAE.size() + reAF.size() < var26.maxFight; ++var23) {
                        var20 = (Mob)GameScr.vMob.elementAt(var23);
                        if (var20.status == 0 || var20.status == 1 || var20.equals(var6) || var6.xFirst - 100 > var20.xFirst || var20.xFirst > var6.xFirst + 100 || var6.yFirst - 50 > var20.yFirst || var20.yFirst > var6.yFirst + 50 || !Auto.gameAC(var20.levelBoss, var2) || var1 != -1 && var20.templateId != var1) continue;
                        reAE.addElement(var20);
                    }
                    for (var23 = 0; var23 < GameScr.vCharInMap.size() && reAE.size() + reAF.size() < var26.maxFight; ++var23) {
                        Char var18 = (Char)GameScr.vCharInMap.elementAt(var23);
                        if (var18.cHP <= 0 || var18.statusMe == 14 || var18.statusMe == 5 || var18.statusMe == 15 || !(var18.cTypePk == 3 || var3.cTypePk == 3 || var18.cTypePk == 1 && var3.cTypePk == 1 || var3.killCharId >= 0 && var3.killCharId == var18.charID || var3.testCharId >= 0 && var3.testCharId == var18.charID) && var18.killCharId != var3.charID || Code.gameAD(var18.cName) || var6.x - var25 > var18.cx || var18.cx > var6.x + var25 || var6.y - var17 > var18.cy || var18.cy > var6.y + var17) continue;
                        reAF.addElement(var18);
                    }
                    if (System.currentTimeMillis() - var26.lastTimeUseThisSkill >= (long)var26.coolDown) {
                        var26.lastTimeUseThisSkill = System.currentTimeMillis();
                        MultiSkillAttack.attackMultiSkill(var3, reAE, reAF);
                    } else {
                        var26.paintCanNotUseSkill = true;
                    }
                } else {
                    int var23;
                    if (var8) {
                        if (!(var26.template.type != 1 && var26.template.type != 3 || Res.abs(var3.cx - var7.cx) <= var26.dx + 30 && Res.abs(var3.cy - var7.cy) <= var26.dy + 30 || System.currentTimeMillis() - this.gameAS <= 1500L)) {
                            Auto.gameAC(var7);
                            this.gameAS = System.currentTimeMillis();
                        }
                        if (var7.killCharId != var3.charID && var7.cTypePk != 3) {
                            this.gameAU = true;
                            Service.gI().gameAZ(3);
                        }
                    }
                    int var25 = var26.dx;
                    int var17 = var26.dy;
                    reAE.removeAllElements();
                    reAF.removeAllElements();
                    reAF.addElement(var7);
                    for (var23 = 0; var23 < GameScr.vCharInMap.size() && reAE.size() + reAF.size() < var26.maxFight; ++var23) {
                        Char var18 = (Char)GameScr.vCharInMap.elementAt(var23);
                        if (var18.cHP <= 0 || var18.statusMe == 14 || var18.statusMe == 5 || var18.statusMe == 15 || var18.equals(var7) || !(var18.cTypePk == 3 || var3.cTypePk == 3 || var18.cTypePk == 1 && var3.cTypePk == 1 || var3.killCharId >= 0 && var3.killCharId == var18.charID || var3.testCharId >= 0 && var3.testCharId == var18.charID) && var18.killCharId != var3.charID || Code.gameAD(var18.cName) || var7.cx - var25 > var18.cx || var18.cx > var7.cx + var25 || var7.cy - var17 > var18.cy || var18.cy > var7.cy + var17) continue;
                        reAF.addElement(var18);
                    }
                    for (var23 = 0; var23 < GameScr.vMob.size() && reAE.size() + reAF.size() < var26.maxFight; ++var23) {
                        var20 = (Mob)GameScr.vMob.elementAt(var23);
                        if (var20.status == 0 || var20.status == 1 || var7.cx - var25 > var20.x || var20.x > var7.cx + var25 || var7.cy - var17 > var20.y || var20.y > var7.cy + var17 || !Auto.gameAC(var20.levelBoss, var2) || var1 != -1 && var20.templateId != var1) continue;
                        reAE.addElement(var20);
                    }
                    if (System.currentTimeMillis() - var26.lastTimeUseThisSkill >= (long)var26.coolDown) {
                        var26.lastTimeUseThisSkill = System.currentTimeMillis();
                        Service.gI().gameAG(var26.template.id);
                        Service.gI().gameAA(reAE, reAF, 2);
                        if (!Code.timBG) {
                            var3.gameAB(GameScr.sks[var26.template.id], 0);
                        }
                    } else {
                        var26.paintCanNotUseSkill = true;
                    }
                }
                this.gameAR = System.currentTimeMillis();
            }
        }
    }

    protected boolean gameAH() {
        if (!(this instanceof TaThu) && !Code.gameAQ) {
            Char var1 = Char.getMyChar();
            int var2 = Code.gameAM < 0 ? -1 : Code.gameAM;
            for (int var3 = 0; var3 < GameScr.vItemMap.size(); ++var3) {
                ItemMap var4 = (ItemMap)GameScr.vItemMap.elementAt(var3);
                if (var4.gameAK || (var1.nClass.classId != 1 || var4.template.id != 218) && var4.template.type != 19 && (!Code.gameAA(var4.template) || Char.gameBG() <= 2 && (!var4.template.isUpToUp || !Char.gameAJ(var4.template.id))) || var2 >= 0 && Res.gameAA(var1.cx, var1.cy, var4.xEnd, var4.yEnd) >= var2 || this.gameAD(var4.x, var4.y)) continue;
                return true;
            }
            return false;
        }
        return false;
    }

    protected final void gameAC(int var1) {
        if (!Code.gameAQ) {
            Char var2 = Char.getMyChar();
            if (!Auto.gameAJ()) {
                int var4;
                gameBA.removeAllElements();
                int var3 = this.gameAA(Char.DanhQuai, Char.DanhTA, Char.DanhTL);
                for (var4 = 0; var4 < GameScr.vItemMap.size(); ++var4) {
                    ItemMap var5 = (ItemMap)GameScr.vItemMap.elementAt(var4);
                    if (var5.gameAK || (var2.nClass.classId != 1 || var5.template.id != 218) && (!Code.gameAA(var5.template) && var5.template.id != var1 || Char.gameBG() <= 2 && var5.template.type != 19 && (!var5.template.isUpToUp || !Char.gameAJ(var5.template.id))) || this.gameAA(var3, var5.xEnd, var5.yEnd) || this.gameAD(var5.xEnd, var5.yEnd) || Code.gameAM >= 0 && Res.gameAA(var2.cx, var2.cy, var5.xEnd, var5.yEnd) >= Code.gameAM) continue;
                    gameBA.addElement(var5);
                }
                if (gameBA.size() > 0) {
                    var4 = var2.cx;
                    int var11 = var2.cy;
                    Mob var10 = var2.mobFocus;
                    block5: for (var3 = 0; var3 < gameBA.size(); ++var3) {
                        try {
                            Thread.sleep(1L);
                        }
                        catch (InterruptedException interruptedException) {
                            // empty catch block
                        }
                        ItemMap var6 = (ItemMap)gameBA.elementAt(var3);
                        Char.gameAC(var6.xEnd, TileMap.gameAD(var6.xEnd, var6.yEnd));
                        var2.itemFocus = var6;
                        for (int var7 = 0; var7 < 4 && var6.status != 2 && !var6.gameAK; ++var7) {
                            Service.gI().gameAQ(var6.itemMapID);
                            if (LockGame.gameAC()) break;
                            if (this.gameAD(var2.cx, var2.cy) || var2.cHP <= 0) break block5;
                        }
                        var6.gameAK = true;
                        var6.gameAL = System.currentTimeMillis();
                    }
                    try {
                        Thread.sleep(500L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    Char.gameAC(var4, var11);
                    var2.mobFocus = var10;
                }
            }
        }
    }

    protected final void gameAI() {
        if (TileMap.mapID != 22) {
            this.gameAA(22, -2, -1, -1);
            return;
        }
        Char char_ = Char.getMyChar();
        if (char_.gameAD != null) {
            for (int i = 0; i < char_.vSkillFight.size(); ++i) {
                Skill skill = (Skill)char_.vSkillFight.elementAt(i);
                if (skill == null || skill.gameAA() || skill.template.id < 67 || skill.template.id > 72) continue;
                Service.gI().gameAG(skill.template.id);
                Service.gI().gameAR();
                LockGame.gameBC();
                break;
            }
            GameScr.gameAH(12);
            Service.gI().gameAJ(12, 3);
            LockGame.gameBC();
            return;
        }
    }

    protected static boolean gameAJ() {
        Char var0 = Char.getMyChar();
        if (var0.isHuman && var0.cHP < var0.cMaxHP) {
            for (int var1 = 0; var1 < var0.vEff.size(); ++var1) {
                Effect var2 = (Effect)var0.vEff.elementAt(var1);
                if (var2 == null || var2.template.type != 12) continue;
                return true;
            }
        }
        return false;
    }

    public abstract void gameAK();

    public String toString() {
        return "";
    }

    static {
        gameAN = new MyVector();
        gameAX = new MyVector();
        gameAO = 0;
        gameAY = false;
        gameAZ = -1L;
        reAE = new MyVector();
        reAF = new MyVector();
        gameBA = new MyVector();
        gameBB = 0L;
    }
}

