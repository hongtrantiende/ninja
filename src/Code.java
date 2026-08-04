/*
 * Decompiled with CFR 0.152.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

public final class Code
implements Runnable {
    public static Code gameAA;
    private static boolean gameCA;
    private static Thread gameCB;
    public static Auto gameAB;
    private static TanSat gameCC;
    public static Stanima gameAC;
    public static AutoNvhn gameAD;
    public static TaThu gameAE;
    private static TuDanh gameCD;
    private static Buff gameCE;
    public static AutoSell gameAF;
    private static DanhVong gameCF;
    public static LoiWin gameAG;
    private static LoiLoss gameCG;
    private static AutoPkAm gameCH;
    private static ChoPk gameCI;
    private static DanhPk gameCJ;
    public static String gameAH;
    public static MyVector gameAI;
    public static MyVector gameAJ;
    private static long gameCK;
    private static long gameCL;
    public static short[] nhat;
    public static short[] dell;
    public static short[] thow;
    public static int gameAM;
    public static int gameAN;
    public static int gameAO;
    public static int gameAP;
    public static boolean gameAQ;
    public static boolean gameAR;
    public static int gameAS;
    public static MyVector gameAT;
    public static MyVector gameAU;
    public static boolean gameAV;
    public static int gameAW;
    public static int[] gameAX;
    public static boolean gameAY;
    private static long gameCM;
    private static MyVector gameCN;
    private static MyVector gameCO;
    private static long gameCP;
    public static MyVector gameBA;
    public static MyVector gameBB;
    public static long gameBC;
    public static long gameBD;
    public static boolean gameBE = true;
    public static boolean timBG = true;
    public static boolean gameBG;
    public static int gameBH;
    public static boolean gameBI;
    public static int gameBJ;
    public static boolean gameBK;
    public static int gameBL;
    public static boolean gameBM;
    public static int gameBN;
    public static boolean gameBO;
    private static String[] gameCQ;
    public static int gameBU;
    public static int gameBV;
    public static int gameBW;
    public static int gameBX;
    public static int gameBY;
    public static long gameBZ;
    public static boolean gameCR;
    public static boolean HoiSinhLuong;
    private static long gameCS;
    private static final Random gameCT;
    private static long gameCU;

    private static void gameAP() {
        int var0;
        gameAA = new Code();
        gameCA = false;
        gameCC = new TanSat();
        gameAC = new Stanima();
        gameAD = new AutoNvhn();
        gameAE = new TaThu();
        gameCD = new TuDanh();
        gameCE = new Buff();
        gameAF = new AutoSell();
        gameCF = new DanhVong();
        gameAG = new LoiWin();
        gameCG = new LoiLoss();
        gameCH = new AutoPkAm();
        gameCI = new ChoPk();
        gameCJ = new DanhPk();
        gameCK = 0L;
        gameCL = 0L;
        nhat = new short[120];
        dell = new short[120];
        thow = new short[120];
        gameAM = -1;
        gameAN = -1;
        gameAO = -1;
        gameAP = -1;
        gameAQ = true;
        gameAR = false;
        gameAT = new MyVector();
        gameAU = new MyVector();
        gameAV = false;
        gameAX = new int[0];
        gameAY = false;
        gameCM = 0L;
        gameCN = new MyVector();
        gameCO = new MyVector();
        gameCP = 0L;
        gameBA = new MyVector();
        gameBB = new MyVector();
        for (var0 = 0; var0 < nhat.length; ++var0) {
            Code.nhat[var0] = -1;
        }
        for (var0 = 0; var0 < dell.length; ++var0) {
            Code.dell[var0] = -1;
        }
        for (var0 = 0; var0 < thow.length; ++var0) {
            Code.thow[var0] = -1;
        }
        gameAH = null;
        gameAI = new MyVector();
        gameAJ = new MyVector();
        try {
            int var3;
            ByteArrayInputStream var7 = new ByteArrayInputStream(RMS.gameAB("V6Group"));
            DataInputStream var1 = new DataInputStream(var7);
            gameAH = var1.readUTF();
            if (gameAH.equals("")) {
                gameAH = null;
            }
            int var2 = var1.readByte();
            for (var3 = 0; var3 < var2; ++var3) {
                gameAI.addElement(var1.readUTF());
            }
            int var10 = var1.readInt();
            for (var3 = 0; var3 < var10; ++var3) {
                gameAJ.addElement(var1.readUTF());
            }
            var1.close();
            var7.close();
        }
        catch (Exception var7) {
            // empty catch block
        }
        try {
            gameCQ = Code.gameAC(Code.gameAK("text.txt"), "\n");
        }
        catch (Exception var4) {
            var4.printStackTrace();
            gameCQ = new String[0];
        }
        gameBC = 0L;
        gameBD = 0L;
        gameBE = true;
        timBG = true;
        gameBG = false;
        gameBH = 5;
        gameBI = true;
        gameBJ = 10;
        gameBK = true;
        gameBL = 50;
        gameBM = false;
        gameBN = 100;
        gameBO = false;
        gameCR = false;
        HoiSinhLuong = false;
        Char.speedGame = 20;
        Char.DungHp = true;
        Char.aHpValue = 20;
        Char.DungMp = true;
        Char.aMpValue = 20;
        Char.DungFood = true;
        Char.CapFood = 50;
        Char.DungHoTro = true;
        Char.KhienMana = true;
        Char.DotQuai = true;
        Char.DungPhanThan = true;
        Char.NhatYen = true;
        Char.NhatVpNV = false;
        Char.NhatVpSk = true;
        Char.NhatAll = true;
        Char.NhatSvc = true;
        Char.ReMap = true;
        Char.TsMapTrong = false;
        Char.AutoMuaFood = true;
        Char.DieHetMp = true;
        Char.ReConnect = true;
        gameBU = 10;
        gameBV = 15;
        gameBW = -1;
        gameBX = -1;
        gameBY = -1;
        gameBZ = 50L;
        String var9 = RMS.gameAC("chipAutopk");
        if (var9 != null) {
            String[] var8 = Code.gameAC(var9, ";");
            try {
                gameBU = Integer.parseInt(var8[0]);
                gameBV = Integer.parseInt(var8[1]);
                gameBW = Integer.parseInt(var8[2]);
                gameBX = Integer.parseInt(var8[3]);
                gameBY = Integer.parseInt(var8[4]);
                gameBZ = Long.parseLong(var8[5]);
                return;
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
    }

    public final void gameAA() {
        if (!gameCA) {
            if (gameAB != null) {
                gameAB.gameAD();
            }
            gameCM = System.currentTimeMillis();
            gameCA = true;
            gameCB = new Thread(this);
            gameCB.start();
        }
    }

    public static void gameAB() {
        gameCA = false;
        if (gameCB != null) {
            LockGame.gameBK();
            gameCB.interrupt();
        }
    }

    public static void gameAA(Auto var0) {
        var0.reAB = gameAB;
        gameAB = var0;
        AutoPickup.start();
    }

    public static void gameAC() {
        LockGame.gameBK();
        gameAB = Code.gameAB.reAB;
    }

    public static void gameAA(int var0, int var1) {
        gameCC.gameAA(var0, var1, Char.TsMapTrong ? -1 : (int)TileMap.zoneID);
        Code.gameAA(gameCC);
    }

    private static void gameAC(int var0, int var1) {
        gameAC.gameAA(var0, var1, Char.TsMapTrong ? -1 : (int)TileMap.zoneID, false, false);
        Code.gameAA(gameAC);
    }

    private static void gameAA(boolean var0, boolean var1) {
        gameAC.gameAA(-1, (int)TileMap.mapID, (int)TileMap.zoneID, var0, var1);
        Code.gameAC.gameAA = true;
        Code.gameAA(gameAC);
    }

    public static void gameAD() {
        gameAD.gameAC();
        Code.gameAA(gameAD);
    }

    public static void gameAE() {
        gameAE.gameAC();
        Code.gameAA(gameAE);
    }

    private static void gameAQ() {
        gameCD.gameAC();
        Code.gameAA(gameCD);
    }

    private static void gameAB(boolean var0, boolean var1) {
        gameCE.gameAA((int)TileMap.mapID, (int)TileMap.zoneID, var0, var1);
        Code.gameAA(gameCE);
    }

    private static void gameAR() {
        gameAF.gameAC();
        Code.gameAA(gameAF);
    }

    private static void gameAS() {
        gameCF.gameAC();
        Code.gameAA(gameCF);
    }

    public static void gameAF() {
        LockGame.gameBK();
        if (gameAB != null) {
            try {
                if (gameAB.toString().equals("Auto San Boss")) {
                    AutoSanBoss.stop();
                }
            } catch (Exception e) {}
        }
        gameAB = null;
    }

    private static void gameAT() {
        gameCH.gameAL();
        Code.gameAA(gameCH);
    }

    public static MyVector gameAG() {
        MyVector var0 = new MyVector();
        for (int var1 = 0; var1 < gameAJ.size(); ++var1) {
            var0.addElement(var1 + ". " + (String)gameAJ.elementAt(var1));
        }
        return var0;
    }

    private static void gameAI(String var0) {
        if (!gameAJ.contains(var0)) {
            gameAJ.addElement(var0);
            Code.gameAV();
        }
    }

    private static void gameAJ(String var0) {
        if (gameAJ.contains(var0)) {
            gameAJ.removeElement(var0);
            Code.gameAV();
        }
    }

    private static void gameAU() {
        gameAJ.removeAllElements();
        Code.gameAV();
    }

    public static boolean gameAA(String var0) {
        return gameAJ.contains(var0);
    }

    private static void gameAV() {
        ByteArrayOutputStream var0 = new ByteArrayOutputStream();
        DataOutputStream var1 = new DataOutputStream(var0);
        try {
            int var2;
            var1.writeUTF(gameAH == null ? "" : gameAH);
            var1.writeByte(gameAI.size());
            for (var2 = 0; var2 < gameAI.size(); ++var2) {
                var1.writeUTF((String)gameAI.elementAt(var2));
            }
            var1.writeInt(gameAJ.size());
            for (var2 = 0; var2 < gameAJ.size(); ++var2) {
                var1.writeUTF((String)gameAJ.elementAt(var2));
            }
            var1.flush();
            var0.flush();
            RMS.gameAA("V6Group", var0.toByteArray());
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static boolean gameAB(String var0) {
        if (gameAH != null && !Code.gameAD(var0)) {
            String var1 = Char.getMyChar().cName;
            return var1.equals(gameAH) ? Code.gameAC(var0) : GameScr.vParty.size() > 1 && var1.equals(((Party)GameScr.vParty.firstElement()).name) && var0.equals(gameAH);
        }
        return false;
    }

    public static boolean gameAC(String var0) {
        for (int var1 = 0; var1 < gameAI.size(); ++var1) {
            if (!var0.equals(gameAI.elementAt(var1))) continue;
            return true;
        }
        return false;
    }

    public static boolean gameAD(String var0) {
        if (var0.equals(Char.getMyChar().cName)) {
            return true;
        }
        for (int var1 = 0; var1 < GameScr.vParty.size(); ++var1) {
            if (!((Party)GameScr.vParty.elementAt((int)var1)).name.equals(var0)) continue;
            return true;
        }
        return false;
    }

    public static boolean gameAH() {
        for (int var0 = 0; var0 < Char.LuyenDaMax - 1; ++var0) {
            if (Char.gameAK(var0) < 4) continue;
            return true;
        }
        return false;
    }

    private static int gameAG(int var0) {
        int var1 = 0;
        Char var2 = Char.getMyChar();
        for (int var3 = 0; var3 < var2.arrItemBag.length; ++var3) {
            Item var4 = var2.arrItemBag[var3];
            if (var4 == null || var4.template.type != 18 || var4.template.level != var0) continue;
            ++var1;
        }
        return var1;
    }

    public final void run() {
        try {
            while (gameCA) {
                long var1 = System.currentTimeMillis();
                try {
                    int var23;
                    int var6;
                    int var5;
                    Char var3 = Char.getMyChar();
                    int var4 = Char.gameBG();
                    if (gameAB != null) {
                        Item var18;
                        int var19;
                        if (!(gameAB instanceof AutoNhan) && !(gameAB instanceof AutoGuiDo) && AutoNhan.gameAE() && AutoNhan.nguoinhan != null && SetAuto.gomdo) {
                            Code.gameAA(new AutoGuiDo(AutoNhan.mapnhan, AutoNhan.khunhan, AutoNhan.nguoinhan));
                        }
                        if (!(!gameAY || gameAB instanceof AutoPkAm || gameAB instanceof Stanima && Code.gameAC.gameAV == 2 || Char.getMyChar().autoUpHp * 100L / GameScr.exps[Char.getMyChar().clevel] < gameBZ)) {
                            AutoPkAm.gameAV = gameAB;
                            Code.gameAT();
                        }
                        if (gameAH != null && System.currentTimeMillis() - gameCK > 5000L) {
                            if (gameAH.equals(var3.cName)) {
                                if (!Auto.gameAK && GameScr.vParty.size() <= 0) {
                                    Service.gI().gameAS();
                                }
                            } else if (GameScr.gameAA(gameAH) != null && GameScr.vParty.size() == 0) {
                                Service.gI().gameAH(gameAH);
                            }
                            gameCK = System.currentTimeMillis();
                        }
                        if (gameBD > 0L) {
                            long var8 = System.currentTimeMillis();
                            if (var8 - gameBC >= gameBD) {
                                gameBD = 0L;
                                LockGame.gameBK();
                                gameAB = null;
                                Session_ME.gameAA.gameAC();
                                Controller.gameAD().onDisconnected();
                                return;
                            }
                            gameBD -= var8 - gameBC;
                            gameBC = var8;
                        }
                        gameAB.gameAK();
                        if (var3.isHuman == Auto.gameAK && (var3.myskill == null || var3.myskill.template.id != Auto.skill1.template.id)) {
                            var3.myskill = Auto.skill1;
                        }
                        if (Char.DieHetMp && Auto.gameAM) {
                            Auto.gameAM = false;
                            if (!(gameAB instanceof TaThu || gameAB instanceof PkBoss || TileMap.isLangCo(TileMap.mapID) || var3.arrItemBody[15] != null)) {
                                Auto.reAD();
                            }
                        }
                        if (var3.statusMe != 14 && var3.statusMe != 5 && var3.cHP > 0) {
                            if (Char.DungMp && System.currentTimeMillis() - gameCL > 500L && Char.getMyChar().cMP < Char.getMyChar().cMaxMP * Char.aMpValue / 100) {
                                Char.getMyChar().gameAE(17);
                                gameCL = System.currentTimeMillis();
                            }
                            if (Char.DungHp && System.currentTimeMillis() - var3.lastUseHP > 2000L && Char.getMyChar().cHP < Char.getMyChar().cMaxHP * Char.aHpValue / 100) {
                                boolean var9 = false;
                                var5 = (int)(System.currentTimeMillis() / 1000L);
                                for (var19 = 0; var19 < Char.getMyChar().vEff.size(); ++var19) {
                                    Effect var7 = (Effect)Char.getMyChar().vEff.elementAt(var19);
                                    if (var7.template.id != 21 || var7.timeLenght - (var5 - var7.timeStart) < 2) continue;
                                    var9 = true;
                                    break;
                                }
                                if (!var9) {
                                    Char.getMyChar().gameAE(16);
                                    var3.lastUseHP = System.currentTimeMillis();
                                }
                            }
                        }
                        if (var3.sPoint > 0 && (Char.CongKN || gameAB instanceof As20) && Auto.skill1 != null && Auto.skill1.point < Auto.skill1.template.maxPoint) {
                            SkillTemplate var17 = Auto.skill1.template;
                            var5 = 0;
                            for (var19 = Auto.skill1.point + 1; var19 <= var17.maxPoint && var17.skills[var19].level <= var3.clevel && var5 < var3.sPoint; ++var5, ++var19) {
                            }
                            if (var5 > 0) {
                                GameScr.gameAC("C\u1ed9ng skill " + var17.name + " " + var5 + " \u0111i\u1ec3m");
                                Service.gI().gameAF(var17.id, var5);
                                if (LockGame.gameAU()) {
                                    Session_ME.gameAA().gameAD();
                                }
                            }
                        }
                        if (var3.pPoint > 0 && (Char.CongTN || gameAB instanceof As20)) {
                            int n = var6 = var3.gameAH() ? 3 : 0;
                            if (var3.pPoint >= 100) {
                                GameScr.gameAC("C\u1ed9ng ti\u1ec1m n\u0103ng " + mResources.gameMZ[var6] + " 60 \u0111i\u1ec3m, " + mResources.gameMZ[2] + " 40 \u0111i\u1ec3m");
                                Service.gI().gameAE(2, 40);
                                Service.gI().gameAE(var6, 60);
                            } else {
                                GameScr.gameAC("C\u1ed9ng ti\u1ec1m n\u0103ng " + mResources.gameMZ[var6] + " " + var3.pPoint + " \u0111i\u1ec3m");
                                Service.gI().gameAE(var6, var3.pPoint);
                            }
                            LockGame.gameAW();
                        }
                        if (gameCR && !(gameAB instanceof DanhVong)) {
                            for (var6 = 0; var6 < var3.arrItemBag.length; ++var6) {
                                var18 = var3.arrItemBag[var6];
                                if (!Code.gameAD(var18)) continue;
                                System.currentTimeMillis();
                                Service.gI().gameAH(var18.indexUI, 1);
                            }
                        }
                        if (gameCR && !(gameAB instanceof DanhVong)) {
                            for (int thowvar6 = 0; thowvar6 < var3.arrItemBag.length; ++thowvar6) {
                                Item itemvut = var3.arrItemBag[thowvar6];
                                if (!Code.thowAD(itemvut)) continue;
                                System.currentTimeMillis();
                                Service.gI().gameAR(itemvut.indexUI);
                            }
                        }
                        if (Char.gameBH() > 0 && SetAuto.catdo && !(gameAB instanceof AutoSell)) {
                            for (int n5 = 0; n5 < Code.gameAC(SetGomDo.itemcat, ",").length; ++n5) {
                                for (int n6 = 0; n6 < Char.getMyChar().arrItemBag.length; ++n6) {
                                    Item item3 = Char.getMyChar().arrItemBag[n6];
                                    if (item3 == null || item3.template.id != Integer.parseInt(Code.gameAC(SetGomDo.itemcat, ",")[n5])) continue;
                                    GameScr.gI().gameAD(4);
                                    Service.gI().gameAE(item3.indexUI);
                                }
                            }
                        }
                        if (SetAuto.dungtnp) {
                            Code.gameAB(538);
                        }
                        if (SetAuto.dungx2) {
                            Code.gameAB(248);
                        }
                        TileMap.gameBZ[138] = new short[]{(short)TanSat.mapid};
                        if (gameCO.size() > 0) {
                            int[] var20 = new int[]{150000, 247500, 408375, 673819, 1111801, 2056832, 4010822, 7420021, 12243035};
                            byte[] var10 = new byte[]{3, 5, 9, 4, 7, 10, 5, 7, 9};
                            for (var19 = 0; var19 < gameCO.size(); ++var19) {
                                var18 = (Item)gameCO.elementAt(var19);
                                var5 = var18.gameAU();
                                if (var18.gameAS) {
                                    if (System.currentTimeMillis() - var18.gameAU <= 0L && var18.gameAT >= var5) continue;
                                    var18.gameAS = false;
                                    continue;
                                }
                                if (var5 >= 0 && var5 < 9) {
                                    MyVector var11 = Char.gameAH(var5 < 3 ? 455 : (var5 < 6 ? 456 : 457));
                                    var6 = var20[var5];
                                    byte var12 = var10[var5];
                                    if (var3.yen < var6 || var11.size() < var12) continue;
                                    Item[] var13 = new Item[24];
                                    for (var6 = 0; var6 < var12; ++var6) {
                                        Item var14;
                                        var13[var6] = var14 = (Item)var11.elementAt(var11.size() - 1);
                                        var3.arrItemBag[var14.indexUI] = null;
                                        var11.removeElementAt(var11.size() - 1);
                                    }
                                    Service.gI().gameAC(var18, var13);
                                    var18.gameAS = true;
                                    var18.gameAT = var5;
                                    var18.gameAU = System.currentTimeMillis();
                                    continue;
                                }
                                gameCO.removeElementAt(var19--);
                            }
                        }
                        if (Char.LuyenTTT && var4 > 0) {
                            MyVector var21 = Char.gameAH(455);
                            while (var21.size() >= 9) {
                                Item[] var22 = new Item[24];
                                for (var19 = 0; var19 < 9; ++var19) {
                                    var22[var19] = var18 = (Item)var21.elementAt(var21.size() - 1);
                                    var3.arrItemBag[var18.indexUI] = null;
                                    var21.removeElementAt(var21.size() - 1);
                                }
                                Service.gI().gameAE(var22);
                            }
                            var4 = Char.gameBG();
                        }
                        if (Char.LuyenTTC && var4 > 0) {
                            MyVector var21 = Char.gameAH(456);
                            while (var21.size() >= 9) {
                                Item[] var22 = new Item[24];
                                for (var19 = 0; var19 < 9; ++var19) {
                                    var22[var19] = var18 = (Item)var21.elementAt(var21.size() - 1);
                                    var3.arrItemBag[var18.indexUI] = null;
                                    var21.removeElementAt(var21.size() - 1);
                                }
                                Service.gI().gameAE(var22);
                            }
                            var4 = Char.gameBG();
                        }
                        if (System.currentTimeMillis() - gameCP > 2000L) {
                            for (var6 = 0; var6 < gameBA.size(); ++var6) {
                                var5 = (Integer)gameBA.elementAt(var6);
                                var19 = (Integer)gameBB.elementAt(var6);
                                if (var19 < 5000) {
                                    gameBA.removeElementAt(var6);
                                    gameBB.removeElementAt(var6);
                                    --var6;
                                    continue;
                                }
                                var18 = Char.gameAG(var5);
                                if (var18 == null) continue;
                                Service.gI().gameAA(var18, var19);
                            }
                            var4 = Char.gameBG();
                            gameCP = System.currentTimeMillis();
                        }
                        if (TileMap.mapID != 138 && TileMap.isLangCo(TileMap.mapID) && (!Char.gameAJ(35) && !Char.gameAJ(37) || Char.DungFood && Char.AutoMuaFood && Char.CapFood <= 50 && var4 > 1 && Code.gameAG(Char.CapFood) == 0)) {
                            TileMap.gameAJ(0);
                            TileMap.gameAF();
                        }
                        if (TileMap.gameAD(TileMap.mapID) || TileMap.gameAF(TileMap.mapID)) {
                            if ((Char.AutoMuaFood || gameAB instanceof As10) && var4 > 1 && var3.ctaskId > 3) {
                                var19 = Char.CapFood;
                                int n = gameAB instanceof As10 ? (var3.ctaskId >= 9 ? 10 : 1) : var19;
                                if (n <= 50 && Code.gameAG(var19) == 0) {
                                    var5 = 2;
                                    for (var6 = 0; var6 < var3.vEff.size(); ++var6) {
                                        if (((Effect)var3.vEff.elementAt((int)var6)).template.type != 0) continue;
                                        --var5;
                                        break;
                                    }
                                    GameScr.gameAB(4, 0, 0);
                                    if (var19 == 50) {
                                        Service.gI().gameAB(9, 7, var5);
                                    } else {
                                        Service.gI().gameAB(9, var19 / 10, var5);
                                    }
                                    LockGame.gameAG();
                                }
                            }
                            if (TileMap.mapID == 138 && var4 > 1 && !Char.gameAJ(35) && !Char.gameAJ(37)) {
                                GameScr.gameAB(4, 0, 0);
                                Service.gI().gameAB(9, 6, 1);
                                LockGame.gameAG();
                                ++var4;
                            }
                            if (var4 < 10 && !(gameAB instanceof As10) && Char.LuyenDa && var3.ctaskId > 9 && var4 > 0 && Code.gameAH()) {
                                boolean var9 = TileMap.gameAF(TileMap.mapID);
                                if (var9) {
                                    var18 = Char.gameAF(37);
                                    if (var18 == null && (var18 = Char.gameAF(35)) == null) {
                                        GameScr.gameAB(4, 0, 0);
                                        Service.gI().gameAB(9, 6, 1);
                                        LockGame.gameAG();
                                        Auto.Sleep(100L);
                                        var18 = Char.gameAF(35);
                                    }
                                    if (var18 != null) {
                                        Service.gI().gameAI(var18.indexUI, 5);
                                        TileMap.gameAF();
                                    }
                                }
                                if (TileMap.gameAD(TileMap.mapID)) {
                                    GameScr.gameAB(6, 1, 1);
                                    LockGame.gameAQ();
                                    Vector<Item> var25 = new Vector<Item>();
                                    block19: for (var19 = 0; var19 < Char.LuyenDaMax - 1; ++var19) {
                                        Item var27;
                                        var25.removeAllElements();
                                        for (var23 = 0; var23 < var3.arrItemBag.length; ++var23) {
                                            var27 = var3.arrItemBag[var23];
                                            if (var27 == null || var27.template.id != var19) continue;
                                            var25.addElement(var27);
                                        }
                                        while (var25.size() >= 4) {
                                            var23 = 1;
                                            for (var5 = var19; var5 < Char.LuyenDaMax - 1 && GameScr.coinUpCrystals[var5] <= var3.yen && var23 << 2 <= var25.size() && var23 < 16; var23 <<= 2, ++var5) {
                                            }
                                            if (var23 == 1) break block19;
                                            GameScr.gameCT = new Item[24];
                                            for (var6 = 0; var6 < var23; ++var6) {
                                                GameScr.gameCT[var6] = var27 = (Item)var25.elementAt(0);
                                                var3.arrItemBag[var27.indexUI] = null;
                                                var25.removeElementAt(0);
                                            }
                                            Service.gI().gameAC(GameScr.gameCT);
                                            LockGame.gameAA();
                                            if (GameScr.gameCT[0] == null) continue;
                                            var3.arrItemBag[GameScr.gameCT[0].indexUI] = GameScr.gameCT[0];
                                        }
                                    }
                                    GameCanvas.endDlg();
                                }
                                if (Char.getMyChar().arrItemBox == null) {
                                    Service.gI().gameAI(4);
                                    LockGame.gameAS();
                                }
                                GameScr.gameAB(5, 0, 0);
                                var19 = Char.gameBH();
                                for (var5 = 0; var5 < var3.arrItemBag.length; ++var5) {
                                    var18 = var3.arrItemBag[var5];
                                    if (var18 == null || var18.template.id != Char.LuyenDaMax - 1 || var19 <= 0) continue;
                                    Service.gI().gameAE(var18.indexUI);
                                    --var19;
                                }
                                if (var9) {
                                    Auto.reAD();
                                }
                                var4 = Char.gameBG();
                                Service.gI().gameAF();
                                LockGame.gameAS();
                            }
                        }
                    }
                    if (gameAQ) {
                        var6 = 100;
                        ItemMap var28 = null;
                        var23 = 0;
                        while (true) {
                            if (var23 >= GameScr.vItemMap.size()) {
                                if (var28 == null) break;
                                Service.gI().gameAQ(var28.itemMapID);
                                Auto.Sleep(50L);
                                break;
                            }
                            ItemMap var24 = (ItemMap)GameScr.vItemMap.elementAt(var23);
                            var5 = Res.gameAA(var3.cx, var3.cy, var24.xEnd, var24.yEnd);
                            if ((var6 == -1 || var5 < var6) && (Code.gameAA(var24.template) || var3.nClass.classId == 1 && var24.template.id == 218) && (var4 > 2 || var24.template.type == 19 || var24.template.isUpToUp && Char.gameAJ(var24.template.id))) {
                                var6 = var5;
                                var28 = var24;
                            }
                            ++var23;
                        }
                    }
                    if (gameAY && var3.autoUpHp * 100L / GameScr.exps[var3.clevel] >= 95L) {
                        LockGame.gameBK();
                        gameAB = null;
                        Session_ME.gameAA().gameAB();
                        GameCanvas.instance.gameAP();
                    }
                    if (System.currentTimeMillis() - gameCM > 2000L) {
                        block26: for (var6 = 0; var6 < gameCN.size(); ++var6) {
                            var5 = (Integer)gameCN.elementAt(var6);
                            ItemTemplate var29 = ItemTemplates.gameAA((short)var5);
                            if (Char.gameAJ(var5)) {
                                for (var23 = 0; var23 < var3.vEff.size(); ++var23) {
                                    Effect var26 = (Effect)var3.vEff.elementAt(var23);
                                    if (var26 != null && var26.template.iconId == var29.iconID) continue block26;
                                }
                                var23 = Char.gameAI(var5);
                                if (var23 >= 0) {
                                    Service.gI().useItem(var23);
                                    continue;
                                }
                            }
                            gameCN.removeElementAt(var6);
                            --var6;
                        }
                        gameCM = System.currentTimeMillis();
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                if (Char.getMyChar().isCaptcha) {
                    LockGame.gameAI();
                }
                Auto.Sleep((var1 = System.currentTimeMillis() - var1) < 100L ? 100L - var1 : 0L);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static boolean gameAA(int var0) {
        return gameCN.contains(new Integer(var0));
    }

    public static void gameAB(int var0) {
        Integer var1 = new Integer(var0);
        if (!gameCN.contains(var1)) {
            gameCN.addElement(var1);
        }
    }

    public static void gameAC(int var0) {
        gameCN.removeElement(new Integer(var0));
    }

    public static boolean gameAA(Item var0) {
        return gameCO.contains(var0);
    }

    public static void gameAB(Item var0) {
        if (!gameCO.contains(var0)) {
            gameCO.addElement(var0);
        }
    }

    public static void gameAC(Item var0) {
        gameCO.removeElement(var0);
    }

    public static boolean gameAD(int var0) {
        return gameBA.contains(new Integer(var0));
    }

    public static int gameAE(int var0) {
        return (var0 = gameBA.indexOf(new Integer(var0))) >= 0 ? (Integer)gameBB.elementAt(var0) : 0;
    }

    public static void gameAB(int var0, int var1) {
        Integer var2 = new Integer(var0);
        if (!gameBA.contains(var2)) {
            gameBA.addElement(var2);
            gameBB.addElement(new Integer(var1));
        }
    }

    public static void gameAF(int var0) {
        if ((var0 = gameBA.indexOf(new Integer(var0))) >= 0) {
            gameBA.removeElementAt(var0);
            gameBB.removeElementAt(var0);
        }
    }

    public static MyVector gameAI() {
        MyVector var0 = new MyVector();
        for (int var1 = 0; var1 < gameBA.size(); ++var1) {
            int var2 = (Integer)gameBA.elementAt(var1);
            int var3 = (Integer)gameBB.elementAt(var1);
            ItemTemplate var4 = ItemTemplates.gameAA((short)var2);
            var0.addElement(var1 + ". " + var4.name + " id " + var2 + " gi\u00e1 " + var3);
        }
        return var0;
    }

    public static void gameAJ() {
        Char var0 = Char.getMyChar();
        for (int var1 = 0; var1 < gameCO.size(); ++var1) {
            Item var2 = (Item)gameCO.elementAt(var1);
            if (var2.indexUI < 0 || var2.indexUI >= var0.arrItemBag.length) continue;
            if (var0.arrItemBag[var2.indexUI] != null && var0.arrItemBag[var2.indexUI].gameAU() >= 0 && var0.arrItemBag[var2.indexUI].gameAU() < 9) {
                gameCO.setElementAt(var0.arrItemBag[var2.indexUI], var1);
                continue;
            }
            gameCO.removeElementAt(var1--);
        }
    }

    public static String gameAK() {
        String var0 = "";
        for (int var1 = 0; var1 < gameAX.length; ++var1) {
            var0 = var0 + (var1 == gameAX.length - 1 ? String.valueOf(gameAX[var1]) : gameAX[var1] + " ");
        }
        return var0;
    }

    public static void gameAE(String var0) {
        String[] var4 = Code.gameAC(var0, " ");
        int[] var1 = new int[var4.length];
        for (int var2 = 0; var2 < var4.length; ++var2) {
            try {
                var1[var2] = Integer.parseInt(var4[var2]);
                continue;
            }
            catch (Exception var3) {
                var1[var2] = -1;
            }
        }
        gameAX = var1;
    }

    public static void gameAA(short var0) {
        int var1;
        for (var1 = 0; var1 < nhat.length; ++var1) {
            if (nhat[var1] != var0) continue;
            return;
        }
        var1 = -1;
        for (int var2 = 0; var2 < nhat.length; ++var2) {
            if (nhat[var2] >= 0) continue;
            var1 = var2;
            break;
        }
        if (var1 == -1) {
            var1 = nhat.length;
            short[] var4 = new short[nhat.length + 10];
            System.arraycopy(var4, 0, nhat, 0, nhat.length);
            for (int var3 = nhat.length; var3 < var4.length; ++var3) {
                var4[var3] = -1;
            }
            nhat = var4;
        }
        Code.nhat[var1] = var0;
    }

    public static void gameAB(short var0) {
        for (int var1 = 0; var1 < nhat.length; ++var1) {
            if (nhat[var1] != var0) continue;
            Code.nhat[var1] = -1;
        }
    }

    public static void gameAL() {
        block0: for (int var0 = 0; var0 < nhat.length; ++var0) {
            if (nhat[var0] <= 0) continue;
            for (int var1 = 0; var1 <= var0; ++var1) {
                if (nhat[var1] != -1) continue;
                Code.nhat[var1] = nhat[var0];
                Code.nhat[var0] = -1;
                continue block0;
            }
        }
    }

    public static void gameAC(short var0) {
        int var1;
        for (var1 = 0; var1 < dell.length; ++var1) {
            if (dell[var1] != var0) continue;
            return;
        }
        var1 = -1;
        for (int var2 = 0; var2 < dell.length; ++var2) {
            if (dell[var2] >= 0) continue;
            var1 = var2;
            break;
        }
        if (var1 == -1) {
            var1 = dell.length;
            short[] var4 = new short[dell.length + 10];
            System.arraycopy(var4, 0, dell, 0, dell.length);
            for (int var3 = dell.length; var3 < var4.length; ++var3) {
                var4[var3] = -1;
            }
            dell = var4;
        }
        Code.dell[var1] = var0;
    }

    public static void thowAC(short var0) {
        int var1;
        for (var1 = 0; var1 < thow.length; ++var1) {
            if (thow[var1] != var0) continue;
            return;
        }
        var1 = -1;
        for (int var2 = 0; var2 < thow.length; ++var2) {
            if (thow[var2] >= 0) continue;
            var1 = var2;
            break;
        }
        if (var1 == -1) {
            var1 = thow.length;
            short[] var4 = new short[thow.length + 10];
            System.arraycopy(var4, 0, thow, 0, thow.length);
            for (int var3 = thow.length; var3 < var4.length; ++var3) {
                var4[var3] = -1;
            }
            thow = var4;
        }
        Code.thow[var1] = var0;
    }

    public static void gameAD(short var0) {
        for (int var1 = 0; var1 < dell.length; ++var1) {
            if (dell[var1] != var0) continue;
            Code.dell[var1] = -1;
        }
    }

    public static void gameAM() {
        block0: for (int var0 = 0; var0 < dell.length; ++var0) {
            if (dell[var0] <= 0) continue;
            for (int var1 = 0; var1 <= var0; ++var1) {
                if (dell[var1] != -1) continue;
                Code.dell[var1] = dell[var0];
                Code.dell[var0] = -1;
                continue block0;
            }
        }
    }

    public static void thowAD(short var0) {
        for (int var1 = 0; var1 < thow.length; ++var1) {
            if (thow[var1] != var0) continue;
            Code.thow[var1] = -1;
        }
    }

    public static void thowAM() {
        block0: for (int var0 = 0; var0 < thow.length; ++var0) {
            if (thow[var0] <= 0) continue;
            for (int var1 = 0; var1 <= var0; ++var1) {
                if (thow[var1] != -1) continue;
                Code.thow[var1] = thow[var0];
                Code.thow[var0] = -1;
                continue block0;
            }
        }
    }

    public static boolean gameAA(ItemTemplate var0) {
        if (gameAB instanceof As20) {
            if (var0.type == 19) {
                return true;
            }
            if ((var0.type == 16 || var0.type == 17) && var0.level == 10) {
                return true;
            }
            Char var3 = Char.getMyChar();
            if (Char.gameBG() <= 6) {
                return false;
            }
            if ((var3.ctaskId < 13 || var3.ctaskId == 13 && var3.arrItemBody[1] != null && var3.arrItemBody[1].upgrade < 2) && var0.type == 26 && var0.id > 0) {
                return true;
            }
            short var2 = var3.cgender == 1 ? (short)124 : 125;
            return var3.ctaskId <= 12 && (var0.id == 174 && !Char.gameAJ(174) || var0.id == var2 && !Char.gameAJ(var2));
        }
        if (gameAB instanceof As10) {
            return var0.type == 19;
        }
        if (var0.type == 19) {
            return Char.NhatYen;
        }
        if (var0.type != 16 && var0.type != 17) {
            if (var0.type == 26) {
                return Char.NhatDa && var0.id >= Char.CapNhatDa - 1;
            }
            if (var0.gameAA()) {
                return (Char.NhatTrangBi || gameAB instanceof Stanima) && var0.level >= Char.CapTrangBi;
            }
            if (var0.gameAB()) {
                return Char.NhatVpNV;
            }
            if (var0.type == 27) {
                if (var0.description.startsWith("V\u1eadt ph\u1ea9m s\u1ef1 ki\u1ec7n") || var0.description.startsWith("V\u1eadt ph\u1ea9m S\u1ef1 ki\u1ec7n")) {
                    return Char.NhatVpSk;
                }
                if (var0.name.startsWith("S\u00e1ch v\u00f5 c\u00f4ng")) {
                    return Char.NhatSvc;
                }
                if (TileMap.isLangCo(TileMap.mapID) && var0.id == 38) {
                    return false;
                }
            }
            for (int var1 = 0; var1 < nhat.length; ++var1) {
                if (nhat[var1] <= 0 || var0.id != nhat[var1]) continue;
                return true;
            }
            return Char.NhatAll;
        }
        return Char.NhatHpMp && var0.level >= Char.CapHpMp;
    }

    public static boolean gameAD(Item var0) {
        if (gameAB instanceof As10) {
            return false;
        }
        if (var0 == null) {
            return false;
        }
        if (var0.upgrade > 0) {
            return false;
        }
        for (int var1 = 0; var1 < dell.length; ++var1) {
            if (dell[var1] <= 0 || var0.template.id != dell[var1]) continue;
            return true;
        }
        return false;
    }

    public static boolean thowAD(Item var0) {
        if (gameAB instanceof As10) {
            return false;
        }
        if (var0 == null) {
            return false;
        }
        for (int var1 = 0; var1 < thow.length; ++var1) {
            if (thow[var1] <= 0 || var0.template.id != thow[var1]) continue;
            return true;
        }
        return false;
    }

    public static void gameAN() {
        Char var0 = Char.getMyChar();
        if (!Char.gameAJ(37) && !Char.gameAJ(35)) {
            Npc var1 = GameScr.gameAI(13);
            if (var1 != null && Math.abs(var1.cx - var0.cx) <= 200 && Math.abs(var1.cy - var0.cy) <= 200) {
                Char.gameAC(var1.cx > 200 ? var1.cx - 200 : var1.cx + 200, var1.cy);
            }
            Service.gI().gameAE();
        } else {
            Char.gameAC(var0.cx, TileMap.pxh);
        }
    }

    public static boolean gameAF(String var0) {
        int var4;
        int var1 = 0;
        StringBuffer var2 = new StringBuffer();
        StringBuffer var3 = new StringBuffer();
        for (var4 = 0; var4 < var0.length(); ++var4) {
            char var5 = var0.charAt(var4);
            if (var5 >= '0' && var5 <= '9' || var5 == ' ') {
                while (var4 < var0.length() && (var5 = var0.charAt(var4)) >= '0' && var5 <= '9') {
                    var3.append(var5);
                    ++var4;
                }
                break;
            }
            var2.append(var5);
        }
        String var31 = var2.toString().toLowerCase();
        if (var3.length() > 0) {
            try {
                var1 = Integer.parseInt(var3.toString());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (var31.equals("locdo")) {
            if (!(gameCR = !gameCR)) {
                GameScr.gameAC("T\u1eaft auto l\u1ecdc \u0111\u1ed3");
            } else {
                GameScr.gameAC("B\u1eadt auto l\u1ecdc \u0111\u1ed3");
            }
            return true;
        }
        if (var31.equals("hsl")) {
            if (!(HoiSinhLuong = !HoiSinhLuong)) {
                GameScr.gameAC("T\u1eaft H\u1ed3i Sinh L\u01b0\u1ee3ng");
            } else {
                GameScr.gameAC("B\u1eadt H\u1ed3i Sinh L\u01b0\u1ee3ng");
            }
            return true;
        }
        if (var31.equals("nhanda")) {
            AutoNhanDa.isAuto = !AutoNhanDa.isAuto;
            if (AutoNhanDa.isAuto) {
                GameScr.gameAC("B\u1eadt Auto Nh\u1eadn \u0110\u00e1!");
                new Thread(new AutoNhanDa()).start();
            } else {
                GameScr.gameAC("T\u1eaft Auto Nh\u1eadn \u0110\u00e1!");
            }
            return true;
        }
        if (var31.equals("gaoda")) {
            AutoGaoDa.isAuto = !AutoGaoDa.isAuto;
            if (AutoGaoDa.isAuto) {
                GameScr.gameAC("B\u1eadt Auto G\u1ea1o \u0110\u00e1!");
                new Thread(new AutoGaoDa()).start();
            } else {
                GameScr.gameAC("T\u1eaft Auto G\u1ea1o \u0110\u00e1!");
            }
            return true;
        }
        if (var31.equals("doidiem")) {
            AutoDoiDiem.isAuto = !AutoDoiDiem.isAuto;
            if (AutoDoiDiem.isAuto) {
                GameScr.gameAC("B\u1eadt Auto \u0110\u1ed5i \u0110i\u1ec3m (3356-240)!");
                new Thread(new AutoDoiDiem()).start();
            } else {
                GameScr.gameAC("T\u1eaft Auto \u0110\u1ed5i \u0110i\u1ec3m!");
            }
            return true;
        }
        if (var31.equals("check")) {
            AutoGoiVe.gameAC = true;
            new Thread(new AutoGoiVe(var1)).start();
            return true;
        }
        if (var31.equals("nhan")) {
            if (TileMap.mapID != AutoNhan.mapnhan) {
                GameScr.gameAC("H\u00e3y V\u1ec1 " + TileMap.mapNames[AutoNhan.mapnhan]);
            } else {
                if (TileMap.zoneID != AutoNhan.khunhan) {
                    GameScr.gI();
                    GameScr.gameAJ(AutoNhan.khunhan);
                    InfoMe.gameAA("Ch\u00e1t l\u1ea1i: nhan \u0111\u1ec3 b\u1eadt auto nh\u1eadn \u0111\u1ed3!", 50, mFont.tahoma_7_yellow);
                }
                if (TileMap.zoneID == AutoNhan.khunhan) {
                    Code.gameAA(new AutoNhan());
                }
            }
            return true;
        }
        if (var31.equals("ve")) {
            AutoGoiVe.gameAB = true;
            new Thread(new AutoGoiVe(var1)).start();
            return true;
        }
        if (var31.equals("rut")) {
            RutDo.gameAC = true;
            new Thread(new RutDo(var1)).start();
            return true;
        }
        if (var31.equals("gd")) {
            AutoGiaoDich.gameAA = (short)var1;
            if (Char.getMyChar().charFocus != null) {
                Code.gameAA(new AutoGiaoDich(Char.getMyChar().charFocus.cName));
                GameScr.gameAC("\u0110\u00e3 g\u1eedi l\u1eddi m\u1eddi gd v\u1eadt ph\u1ea9m ! ");
            } else {
                GameScr.gameAC("H\u00e3y ch\u1ec9 v\u00e0o \u0111\u1ed1i ph\u01b0\u01a1ng <VPGD>");
            }
            return true;
        }
        if (var31.startsWith("vdmq")) {
            Code.nhat[10] = 454;
            Code.nhat[11] = 455;
            Code.nhat[12] = 456;
            Code.nhat[13] = 457;
            Code.nhat[14] = 573;
            Code.nhat[15] = 574;
            Code.nhat[16] = 575;
            Code.nhat[17] = 38;
            Code.nhat[18] = 383;
            Code.nhat[19] = 384;
            Code.nhat[20] = 545;
            Code.nhat[21] = 524;
            Code.nhat[22] = 449;
            Code.nhat[23] = 450;
            Code.nhat[24] = 451;
            Code.nhat[25] = 452;
            Code.nhat[26] = 453;
            Code.nhat[27] = 576;
            Code.nhat[28] = 577;
            Code.nhat[29] = 578;
            GameScr.gameAC("\u0110\u00e3 Th\u00eam Item Nh\u1eb7t VDMQ");
            Service.gI().gameAK("vdmq");
            return true;
        }
        if (var31.equals("uppk")) {
            new SetPkAm().gameAA();
            return true;
        }
        if (var31.equals("adpk")) {
            if (gameAB instanceof DanhPk) {
                GameScr.gameAC("T\u1eaft auto \u0111\u00e1nh ai b\u1eadt pk");
                Code.gameAF();
            } else {
                GameScr.gameAC("B\u1eadt auto \u0111\u00e1nh ai b\u1eadt pk");
                gameCJ.gameAL();
                Code.gameAA(gameCJ);
            }
            return true;
        }
        if (var31.equals("acpk")) {
            if (gameAB instanceof ChoPk) {
                GameScr.gameAC("T\u1eaft auto ch\u1edd pk");
                Code.gameAF();
            } else {
                GameScr.gameAC("B\u1eadt auto ch\u1edd pk");
                gameCI.gameAL();
                Code.gameAA(gameCI);
            }
            return true;
        }
        if (var31.equals("atpk")) {
            if (gameAB instanceof AutoPkAm) {
                GameScr.gameAC("T\u1eaft auto pk \u00e2m kinh nghi\u1ec7m");
                Code.gameAF();
            } else {
                GameScr.gameAC("B\u1eadt auto pk \u00e2m kinh nghi\u1ec7m");
                if (gameAB != null) {
                    AutoPkAm.gameAV = gameAB;
                }
                gameAY = true;
                Code.gameAT();
            }
            return true;
        }
        if (var31.equals("s")) {
            if (var1 == 0) {
                GameScr.gameAC("Ch\u1ea1y \u0111i \u0111ou v\u1edbi t\u1ed1c \u0111\u1ed9 0?");
            } else if (var1 > 100) {
                GameScr.gameAC("T\u1ed1c gi\u00e0y n\u00ean \u0111\u1ec3 <= 100 \u0111\u1ec3 ko b\u1ecb gi\u1eadt!");
            } else {
                GameScr.gameAC("Fake t\u1ed1c ch\u1ea1y " + var1);
                gameBH = var1;
                gameBG = true;
            }
            return true;
        }
        if (var31.equals("rs")) {
            GameScr.gameAC("Reset t\u1ed1c ch\u1ea1y");
            gameBG = false;
            return true;
        }
        if (var31.equals("n")) {
            if (var1 == 0) {
                var1 = 100;
            }
            GameScr.gameAC("Fake t\u1ea7m ngang " + var1);
            gameBI = true;
            gameBJ = var1;
            return true;
        }
        if (var31.equals("c")) {
            if (var1 == 0) {
                var1 = 100;
            }
            GameScr.gameAC("Fake t\u1ea7m cao " + var1);
            gameBK = true;
            gameBL = var1;
            return true;
        }
        if (var31.equals("m")) {
            if (var1 == 0) {
                var1 = 1;
            }
            GameScr.gameAC("Fake lan " + var1);
            gameBM = true;
            gameBN = var1;
            return true;
        }
        if (var31.equals("rsk")) {
            GameScr.gameAC("Reset fake t\u1ea7m lan skill");
            gameBM = false;
            gameBI = false;
            gameBK = false;
            return true;
        }
        if (!var31.equals("bang") && !var31.equals("fz")) {
            if (!var31.equals("bangb") && !var31.equals("fb")) {
                if (!var31.equals("bangs") && !var31.equals("fs")) {
                    if (!var31.equals("pbang") && !var31.equals("wz")) {
                        if (var31.equals("u")) {
                            if (var1 == 0) {
                                var1 = 50;
                            }
                            GameScr.gameAC("Khinh k\u00f4ng " + var1);
                            Char.gameAC(Char.getMyChar().cx, Char.getMyChar().cy - var1);
                            return true;
                        }
                        if (var31.equals("d")) {
                            if (var1 == 0) {
                                var1 = 50;
                            }
                            GameScr.gameAC("\u0110\u1ed9n th\u1ed5 " + var1);
                            Char.gameAC(Char.getMyChar().cx, Char.getMyChar().cy + var1);
                            return true;
                        }
                        if (var31.equals("l")) {
                            if (var1 == 0) {
                                var1 = 50;
                            }
                            GameScr.gameAC("D\u1ecbch tr\u00e1i " + var1);
                            Char.gameAC(Char.getMyChar().cx - var1, Char.getMyChar().cy);
                            return true;
                        }
                        if (var31.equals("r")) {
                            if (var1 == 0) {
                                var1 = 50;
                            }
                            GameScr.gameAC("D\u1ecbch ph\u1ea3i " + var1);
                            Char.gameAC(Char.getMyChar().cx + var1, Char.getMyChar().cy);
                            return true;
                        }
                        if (var31.equals("g")) {
                            Char var29 = Char.getMyChar();
                            if (var29.charFocus != null) {
                                GameScr.gameAC("MoveTo " + var29.charFocus.cName);
                                Char.gameAC(var29.charFocus.cx, var29.charFocus.cy);
                            } else if (var29.npcFocus != null) {
                                GameScr.gameAC("MoveTo " + var29.npcFocus.cName);
                                Char.gameAC(var29.npcFocus.cx, var29.npcFocus.cy);
                            } else if (var29.mobFocus != null) {
                                GameScr.gameAC("MoveTo " + var29.mobFocus.getTemplate().name);
                                Char.gameAC(var29.mobFocus.xFirst, var29.mobFocus.yFirst);
                            } else if (var29.itemFocus != null) {
                                GameScr.gameAC("MoveTo " + var29.itemFocus.template.name);
                                Char.gameAC(var29.itemFocus.x, var29.itemFocus.y);
                            }
                            return true;
                        }
                        if (var31.equals("ta")) {
                            GameScr.gI().gameAD(9);
                            return true;
                        }
                        if (var31.equals("sw")) {
                            GameScr.gI().gameAD(36);
                            return true;
                        }
                        if (var31.equals("up")) {
                            SetTimeUp.gameAA().gameAB();
                            return true;
                        }
                        if (var31.equals("aq")) {
                            Char var29 = Char.getMyChar();
                            if (var29.mobFocus != null) {
                                GameScr.vMob.removeElement(var29.mobFocus);
                            }
                            return true;
                        }
                        if (var31.equals("z")) {
                            GameScr.gameAC((Char.ChuyenMapHetQuai ? "T\u1eaft" : "B\u1eadt") + " auto chuy\u1ec3n map");
                            Char.ChuyenMapHetQuai = !Char.ChuyenMapHetQuai;
                            return true;
                        }
                        if (var31.equals("rm")) {
                            GameScr.gameAC((Char.ReMap ? "T\u1eaft" : "B\u1eadt") + " auto next map");
                            Char.ReMap = !Char.ReMap;
                            return true;
                        }
                        if (var31.equals("x")) {
                            if (var1 == 0) {
                                var1 = -1;
                            }
                            GameScr.gameAC("KC Nh\u1eb7t " + var1);
                            gameAM = var1;
                            return true;
                        }
                        if (var31.equals("kts")) {
                            if (var1 == 0) {
                                var1 = -1;
                            }
                            GameScr.gameAC("KC T\u00e0n s\u00e1t " + var1);
                            gameAO = Char.getMyChar().cx;
                            gameAP = Char.getMyChar().cy;
                            gameAN = var1;
                            return true;
                        }
                        if (var31.equals("ts")) {
                            Mob var26 = Mob.gameAB(var1);
                            if (var26 == null) {
                                GameScr.gameAC("T\u00e0n s\u00e1t all");
                                Code.gameAA(-1, TileMap.mapID);
                            } else {
                                GameScr.gameAC("T\u00e0n s\u00e1t " + var26.getTemplate().name + " lv " + var1);
                                Code.gameAA(var26.templateId, TileMap.mapID);
                            }
                            return true;
                        }
                        if (var31.equals("tsx")) {
                            MobTemplate var10000;
                            MobTemplate var24 = var10000 = var1 >= 0 && var1 < Mob.arrMobTemplate.length ? Mob.arrMobTemplate[var1] : null;
                            if (var10000 == null) {
                                GameScr.gameAC("T\u00e0n s\u00e1t all");
                                Code.gameAA(-1, TileMap.mapID);
                            } else {
                                GameScr.gameAC("T\u00e0n s\u00e1t " + var24.name + " id " + var1);
                                Code.gameAA(var24.mobTemplateId, TileMap.mapID);
                            }
                            return true;
                        }
                        if (var31.equals("tsa")) {
                            GameScr.gameAC("T\u00e0n s\u00e1t all");
                            Code.gameAA(-1, TileMap.mapID);
                            return true;
                        }
                        if (var31.equals("anv")) {
                            if (TileMap.mapID != 1 && TileMap.mapID != 27 && TileMap.mapID != 72) {
                                GameScr.gameAC("B\u1ea1n ph\u1ea3i \u0111\u1ee9ng \u1edf tr\u01b0\u1eddng \u0111\u1ec3 Auto");
                                return true;
                            }
                            GameScr.gameAC("Auto Nhi\u1ec7m V\u1ee5 H\u1eb1ng Ng\u00e0y");
                            Code.gameAD();
                            return true;
                        }
                        if (var31.equals("att")) {
                            GameScr.gameAC("Auto T\u00e0 Th\u00fa");
                            Code.gameAE();
                            return true;
                        }
                        if (var31.equals("ak")) {
                            if (gameAB == gameCD) {
                                GameScr.gameAC("T\u1eaft t\u1ef1 \u0111\u00e1nh");
                                Code.gameAF();
                            } else {
                                GameScr.gameAC("B\u1eadt t\u1ef1 \u0111\u00e1nh");
                                Code.gameAQ();
                            }
                            return true;
                        }
                        if (var31.equals("pk")) {
                            gameBO = !gameBO;
                            GameScr.gameAC((gameBO ? " B\u1eadt " : " T\u1eaft ") + "auto pk!");
                            return true;
                        }
                        if (var31.equals("adv")) {
                            GameScr.gameAC("Auto Danh V\u1ecdng");
                            Code.gameAS();
                            return true;
                        }
                        if (var31.equals("ld")) {
                            new FormDanhVong().select();
                            return true;
                        }
                        if (var31.equals("nw")) {
                            if (gameAB == gameAG) {
                                GameScr.gameAC("T\u1eaft auto l\u00f4i \u0111\u00e0i win");
                                LockGame.gameBK();
                                gameAB = null;
                            } else {
                                GameScr.gameAC("B\u1eadt auto l\u00f4i \u0111\u00e0i win");
                                gameAG.gameAL();
                                Code.gameAA(gameAG);
                            }
                            return true;
                        }
                        if (var31.equals("nl")) {
                            if (gameAB == gameCG) {
                                GameScr.gameAC("T\u1eaft auto l\u00f4i \u0111\u00e0i lose");
                                LockGame.gameBK();
                                gameAB = null;
                            } else {
                                GameScr.gameAC("B\u1eadt auto l\u00f4i \u0111\u00e0i lose");
                                gameCG.gameAL();
                                Code.gameAA(gameCG);
                            }
                            return true;
                        }
                        if (!var31.equals("e") && !var31.equals("pe")) {
                            if (var31.equals("k")) {
                                GameScr.gameAC("Chuy\u1ec3n Khu: " + var1);
                                GameScr.gI();
                                GameScr.gameAJ(var1);
                                return true;
                            }
                            if (var31.equals("ltd")) {
                                if (!TileMap.gameAF(TileMap.mapID) && !TileMap.gameAD(TileMap.mapID)) {
                                    GameScr.gameAC("H\u00e3y \u0111\u1ee9ng \u1edf l\u00e0ng ho\u1eb7c tr\u01b0\u1eddng \u0111\u1ec3 l\u01b0u t\u1ecda \u0111\u1ed9");
                                } else {
                                    GameScr.gameAH(5);
                                    Service.gI().gameAH(5);
                                    Service.gI().gameAC(5, 1, 0);
                                }
                                return true;
                            }
                            if (var31.equals("nm")) {
                                GameScr.gameAC("Next map: " + var1);
                                TileMap.gameAM(var1);
                                return true;
                            }
                            if (var31.equals("gm")) {
                                if (var1 < TileMap.mapNames.length && var1 >= 0) {
                                    GameScr.gameAC("Go to: " + TileMap.mapNames[var1]);
                                    TileMap.gameAL(var1);
                                    return true;
                                }
                                return true;
                            }
                            if (var31.equals("npc")) {
                                if (var1 < Npc.arrNpcTemplate.length) {
                                    GameScr.gameAC("Act NPC: " + Npc.arrNpcTemplate[var1].name);
                                    GameScr.gameAH(var1);
                                }
                                return true;
                            }
                            if (var31.equals("hs")) {
                                GameScr.gameAC("Next to hirosaki");
                                TileMap.gameAL(1);
                                return true;
                            }
                            if (var31.equals("hr")) {
                                GameScr.gameAC("Next to haruna");
                                TileMap.gameAL(27);
                                return true;
                            }
                            if (var31.equals("oz")) {
                                GameScr.gameAC("Next to Ozawa(Oozaka)");
                                TileMap.gameAL(72);
                                return true;
                            }
                            if (var31.equals("kj")) {
                                GameScr.gameAC("Next to Kojin");
                                TileMap.gameAL(10);
                                return true;
                            }
                            if (var31.equals("sz")) {
                                GameScr.gameAC("Next to Sanzu");
                                TileMap.gameAL(17);
                                return true;
                            }
                            if (var31.equals("tn")) {
                                GameScr.gameAC("Next to Tone");
                                TileMap.gameAL(22);
                                return true;
                            }
                            if (var31.equals("lc")) {
                                GameScr.gameAC("Next to Ch\u00e0i");
                                TileMap.gameAL(32);
                                return true;
                            }
                            if (var31.equals("ck")) {
                                GameScr.gameAC("Next to Chakumi");
                                TileMap.gameAL(38);
                                return true;
                            }
                            if (var31.equals("eg")) {
                                GameScr.gameAC("Next to Echigo");
                                TileMap.gameAL(43);
                                return true;
                            }
                            if (var31.equals("os")) {
                                GameScr.gameAC("Next to Oshin");
                                TileMap.gameAL(48);
                                return true;
                            }
                            if (var31.equals("mnv")) {
                                GameScr.gameAC("Next to Map Nhi\u1ec7m V\u1ee5");
                                TileMap.gameAL(GameScr.gameBD());
                                return true;
                            }
                            if (var31.equals("mnvp")) {
                                GameScr.gameAC("Next to Map Nhi\u1ec7m V\u1ee5 Ph\u1ee5");
                                TaskOrder var34 = Char.gameAM(0);
                                if (var34 != null) {
                                    TileMap.gameAL(var34.mapId);
                                }
                                return true;
                            }
                            if (var31.equals("add")) {
                                GameScr.gameAC("Th\u00eam v\u1eadt ph\u1ea9m v\u00e0o ds nh\u1eb7t");
                                ItemMap var32 = Char.getMyChar().itemFocus;
                                if (var32 != null) {
                                    Code.gameAA(var32.template.id);
                                }
                                return true;
                            }
                            if (var31.equals("del")) {
                                GameScr.gameAC("X\u00f3a v\u1eadt ph\u1ea9m kh\u1ecfi ds nh\u1eb7t");
                                ItemMap var32 = Char.getMyChar().itemFocus;
                                if (var32 != null) {
                                    Code.gameAB(var32.template.id);
                                }
                                return true;
                            }
                            if (var31.equals("ait")) {
                                ItemTemplate var30 = ItemTemplates.gameAA((short)var1);
                                if (var30 != null) {
                                    GameScr.gameAC("Th\u00eam " + var30.name + " v\u00e0o ds nh\u1eb7t");
                                    Code.gameAA(var30.id);
                                }
                                return true;
                            }
                            if (var31.equals("dit")) {
                                ItemTemplate var30 = ItemTemplates.gameAA((short)var1);
                                if (var30 != null) {
                                    GameScr.gameAC("X\u00f3a " + var30.name + " kh\u1ecfi ds nh\u1eb7t");
                                    Code.gameAB(var30.id);
                                }
                                return true;
                            }
                            if (var31.equals("ajt")) {
                                ItemTemplate var30 = ItemTemplates.gameAA((short)var1);
                                if (var30 != null) {
                                    GameScr.gameAC("Th\u00eam " + var30.name + " v\u00e0o ds nh\u1eb7t");
                                    Code.gameAC(var30.id);
                                }
                                return true;
                            }
                            if (var31.equals("djt")) {
                                ItemTemplate var30 = ItemTemplates.gameAA((short)var1);
                                if (var30 != null) {
                                    GameScr.gameAC("X\u00f3a " + var30.name + " kh\u1ecfi ds nh\u1eb7t");
                                    Code.gameAD(var30.id);
                                }
                                return true;
                            }
                            if (var31.equals("cnhat")) {
                                if (gameAQ) {
                                    GameScr.gameAC("B\u1eadt nh\u1eb7t xa");
                                } else {
                                    GameScr.gameAC("B\u1eadt h\u00fat VP");
                                }
                                gameAQ = !gameAQ;
                                return true;
                            }
                            if (var31.equals("ruong")) {
                                GameScr.gI().gameAH();
                                return true;
                            }
                            if (var31.equals("vpnhat")) {
                                GameScr.gI().gameAD(46);
                                return true;
                            }
                            if (var31.equals("die")) {
                                Code.gameAN();
                                return true;
                            }
                            if (var31.equals("hds")) {
                                mResources.gameAA();
                                return true;
                            }
                            if (var31.equals("dcvt")) {
                                if (gameAR) {
                                    GameScr.gameAC("T\u1eaft \u0111\u00e1nh chuy\u1ec3n v\u1ecb tr\u00ed");
                                } else {
                                    GameScr.gameAC("B\u1eadt \u0111\u00e1nh chuy\u1ec3n v\u1ecb tr\u00ed");
                                }
                                boolean bl = gameAR = !gameAR;
                                if (Char.DanhNhom) {
                                    Service.gI().gameAK("dcvt " + (gameAR ? 1 : 0));
                                }
                                return true;
                            }
                            if (var31.equals("avt")) {
                                GameScr.gameAC("Th\u00eam v\u1ecb tr\u00ed " + gameAT.size());
                                gameAT.addElement(new Integer(Char.getMyChar().cx));
                                gameAU.addElement(new Integer(Char.getMyChar().cy));
                                if (Char.DanhNhom) {
                                    Service.gI().gameAK("avt " + Char.getMyChar().cx + " " + Char.getMyChar().cy);
                                }
                                return true;
                            }
                            if (var31.equals("dvt")) {
                                GameScr.gameAC("X\u00f3a h\u1ebft v\u1ecb tr\u00ed");
                                gameAT.removeAllElements();
                                gameAU.removeAllElements();
                                if (Char.DanhNhom) {
                                    Service.gI().gameAK("dvt");
                                }
                                return true;
                            }
                            if (var31.equals("dvtx")) {
                                GameScr.gameAC("X\u00f3a v\u1ecb tr\u00ed " + var1);
                                gameAT.removeElementAt(var1);
                                gameAU.removeElementAt(var1);
                                if (Char.DanhNhom) {
                                    Service.gI().gameAK("dtvx " + var1);
                                }
                                return true;
                            }
                            if (var31.equals("dck")) {
                                if (gameAV = !gameAV) {
                                    GameScr.gameAC("T\u1eaft \u0111\u00e1nh chuy\u1ec3n khu");
                                } else {
                                    GameScr.gameAC("B\u1eadt \u0111\u00e1nh chuy\u1ec3n khu");
                                    GameCanvas.inputDlg.gameAA("Khu", new Command("\u0110\u1eb7t", 1100090), 1);
                                    GameCanvas.inputDlg.tfInput.gameAA(Code.gameAK());
                                }
                                return true;
                            }
                            if (var31.equals("glv")) {
                                if (gameAY) {
                                    GameScr.gameAC("T\u1eaft gi\u1eef lv");
                                } else {
                                    GameScr.gameAC("B\u1eadt gi\u1eef lv");
                                }
                                gameAY = !gameAY;
                                return true;
                            }
                            if (var31.equals("addn")) {
                                GameScr.gameAC("Th\u00eam nh\u00f3m");
                                Char var29 = Char.getMyChar().charFocus;
                                if (var29 != null) {
                                    if (!Code.gameAC(var29.cName)) {
                                        gameAI.addElement(var29.cName);
                                    }
                                    Service.gI().gameAF(var29.cName);
                                }
                                return true;
                            }
                            if (var31.equals("cn")) {
                                GameScr.gameAC("X\u00f3a nh\u00f3m");
                                gameAH = null;
                                gameAI.removeAllElements();
                                Code.gameAV();
                                return true;
                            }
                            if (var31.equals("pt")) {
                                if (!Char.getMyChar().cName.equals(gameAH)) {
                                    GameScr.gameAC("B\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                    return true;
                                }
                                GameScr.gameAC("PT nh\u00f3m");
                                for (var4 = 0; var4 < gameAI.size(); ++var4) {
                                    var0 = (String)gameAI.elementAt(var4);
                                    if (!Code.gameAD(var0)) {
                                        Service.gI().gameAF(var0);
                                    }
                                    if (gameAB instanceof PkBoss) {
                                        Service.gI().gameAA(var0, "pkm " + Code.gameAB.mapID);
                                        Service.gI().gameAA(var0, "pkk " + Code.gameAB.zoneID);
                                        continue;
                                    }
                                    if (gameAB == null) continue;
                                    Service.gI().gameAA(var0, "map " + Code.gameAB.mapID);
                                    Service.gI().gameAA(var0, "khu " + Code.gameAB.zoneID);
                                }
                                return true;
                            }
                            if (var31.equals("sn")) {
                                GameScr.gameAC("L\u01b0u nh\u00f3m");
                                Code.gameAV();
                                return true;
                            }
                            if (var31.equals("tsn")) {
                                if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                    Mob var26 = Mob.gameAB(var1);
                                    if (var26 == null) {
                                        GameScr.gameAC("T\u00e0n s\u00e1t nh\u00f3m all");
                                        Code.gameAA(-1, TileMap.mapID);
                                    } else {
                                        GameScr.gameAC("T\u00e0n s\u00e1t nh\u00f3m " + var26.getTemplate().name + " lv " + var1);
                                        Code.gameAA(var26.templateId, TileMap.mapID);
                                    }
                                    Code.gameCC.gameAA = true;
                                    Service.gI().gameAK("ts " + Code.gameCC.mapID + " " + Code.gameCC.zoneID + " " + Code.gameCC.templateId);
                                    return true;
                                }
                                GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                return true;
                            }
                            if (var31.equals("tsnx")) {
                                if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                    MobTemplate var10000;
                                    MobTemplate var24 = var10000 = var1 >= 0 && var1 < Mob.arrMobTemplate.length ? Mob.arrMobTemplate[var1] : null;
                                    if (var10000 == null) {
                                        GameScr.gameAC("T\u00e0n s\u00e1t nh\u00f3m all");
                                        Code.gameAA(-1, TileMap.mapID);
                                    } else {
                                        GameScr.gameAC("T\u00e0n s\u00e1t nh\u00f3m " + var24.name + " id " + var1);
                                        Code.gameAA(var24.mobTemplateId, TileMap.mapID);
                                    }
                                    Code.gameCC.gameAA = true;
                                    Service.gI().gameAK("ts " + Code.gameCC.mapID + " " + Code.gameCC.zoneID + " " + Code.gameCC.templateId);
                                    return true;
                                }
                                GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                return true;
                            }
                            if (var31.equals("tsan")) {
                                if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                    GameScr.gameAC("T\u00e0n s\u00e1t nh\u00f3m all");
                                    Code.gameAA(-1, TileMap.mapID);
                                    Code.gameCC.gameAA = true;
                                    Service.gI().gameAK("tsa " + Code.gameCC.mapID + " " + Code.gameCC.zoneID);
                                    return true;
                                }
                                GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                return true;
                            }
                            if (var31.equals("attn")) {
                                if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                    GameScr.gameAC("Auto T\u00e0 Th\u00fa Nh\u00f3m");
                                    Code.gameAE();
                                    Code.gameAE.gameAA = true;
                                    Service.gI().gameAK("att " + Code.gameAE.mapID + " " + Code.gameAE.zoneID + " " + Code.gameAE.gameAV);
                                    return true;
                                }
                                GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                return true;
                            }
                            if (var31.equals("buff")) {
                                GameScr.gameAC("B\u1eadt Buff HS Xa");
                                Code.gameAB(true, true);
                                return true;
                            }
                            if (var31.equals("bux")) {
                                GameScr.gameAC("B\u1eadt Buff Xa");
                                Code.gameAB(true, false);
                                return true;
                            }
                            if (var31.equals("hsx")) {
                                GameScr.gameAC("B\u1eadt HS Xa");
                                Code.gameAB(false, true);
                                return true;
                            }
                            if (var31.equals("cy")) {
                                if (gameAB == null) {
                                    GameScr.gameAC("B\u1ea1n ch\u01b0a up y\u00ean");
                                } else {
                                    int var25 = Char.getMyChar().yen - Code.gameAB.gameAG;
                                    var1 = (int)((System.currentTimeMillis() - Code.gameAB.gameAI) / 1000L);
                                    GameScr.gameAC("Up " + var25 + " trong " + NinjaUtil.gameAB(var1) + " perh=" + var25 / var1 * 3600);
                                }
                                return true;
                            }
                            if (var31.equals("clv")) {
                                if (gameAB == null) {
                                    GameScr.gameAC("B\u1ea1n ch\u01b0a up level");
                                } else {
                                    long var38 = Char.getMyChar().cEXP - Code.gameAB.gameAH;
                                    float var28 = (float)(var38 * 10000L / GameScr.exps[Char.getMyChar().clevel]) / 100.0f;
                                    int var25 = (int)((System.currentTimeMillis() - Code.gameAB.gameAI) / 1000L);
                                    long var40 = var38 * 3600L / (long)var25;
                                    float var33 = (float)(var40 * 10000L / GameScr.exps[Char.getMyChar().clevel]) / 100.0f;
                                    GameScr.gameAC("Up " + var38 + " - " + var28 + "% trong " + NinjaUtil.gameAB(var25) + " perh=" + var40 + " - " + var33 + "%");
                                }
                                return true;
                            }
                            if (var31.equals("st")) {
                                Mob var26 = Mob.gameAB(var1);
                                if (var26 == null) {
                                    GameScr.gameAC("Stanima all");
                                    Code.gameAC(-1, TileMap.mapID);
                                } else {
                                    GameScr.gameAC("Stanima " + var26.getTemplate().name + " lv " + var1);
                                    Code.gameAC(var26.templateId, TileMap.mapID);
                                }
                                return true;
                            }
                            if (var31.equals("sta")) {
                                GameScr.gameAC("Stanima all");
                                Code.gameAC(-1, TileMap.mapID);
                                return true;
                            }
                            if (var31.equals("stn")) {
                                if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                    Mob var26 = Mob.gameAB(var1);
                                    if (var26 == null) {
                                        GameScr.gameAC("Stanima nh\u00f3m all");
                                        Code.gameAC(-1, TileMap.mapID);
                                    } else {
                                        GameScr.gameAC("Stanima nh\u00f3m " + var26.getTemplate().name + " lv " + var1);
                                        Code.gameAC(var26.templateId, TileMap.mapID);
                                    }
                                    Code.gameAC.gameAA = true;
                                    Service.gI().gameAK("st " + Code.gameAC.mapID + " " + Code.gameAC.zoneID + " " + Code.gameAC.gameAW);
                                    return true;
                                }
                                GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                return true;
                            }
                            if (var31.equals("stan")) {
                                if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                    GameScr.gameAC("Stanima nh\u00f3m all");
                                    Code.gameAC(-1, TileMap.mapID);
                                    Code.gameAC.gameAA = true;
                                    Service.gI().gameAK("sta " + Code.gameAC.mapID + " " + Code.gameAC.zoneID);
                                    return true;
                                }
                                GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                return true;
                            }
                            if (var31.equals("stx")) {
                                MobTemplate var10000;
                                MobTemplate var24 = var10000 = var1 >= 0 && var1 < Mob.arrMobTemplate.length ? Mob.arrMobTemplate[var1] : null;
                                if (var10000 == null) {
                                    GameScr.gameAC("T\u00e0n s\u00e1t all");
                                    Code.gameAA(-1, TileMap.mapID);
                                } else {
                                    GameScr.gameAC("T\u00e0n s\u00e1t " + var24.name + " id " + var1);
                                    Code.gameAC(var24.mobTemplateId, TileMap.mapID);
                                }
                                return true;
                            }
                            if (!var31.equals("stnx")) {
                                if (var31.equals("sts")) {
                                    GameScr.gameAC("Step Stanima");
                                    gameAC.gameAL();
                                    if (Char.getMyChar().cName.equals(gameAH) && GameScr.vParty.size() > 0) {
                                        Service.gI().gameAK("sts");
                                    }
                                    return true;
                                }
                                if (var31.equals("stb")) {
                                    if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId != Char.getMyChar().charID) {
                                        if (Char.getMyChar().nClass.classId != 6) {
                                            GameScr.gameAC("B\u1ea1n kh\u00f4ng ph\u1ea3i l\u00e0 qu\u1ea1t");
                                            return true;
                                        }
                                        GameScr.gameAC("Stanima Buff HS");
                                        Code.gameAA(true, true);
                                        return true;
                                    }
                                    GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                    return true;
                                }
                                if (var31.equals("stbx")) {
                                    if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId != Char.getMyChar().charID) {
                                        if (Char.getMyChar().nClass.classId != 6) {
                                            GameScr.gameAC("B\u1ea1n kh\u00f4ng ph\u1ea3i l\u00e0 qu\u1ea1t");
                                            return true;
                                        }
                                        GameScr.gameAC("Stanima Buff");
                                        Code.gameAA(true, false);
                                        return true;
                                    }
                                    GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                    return true;
                                }
                                if (var31.equals("sths")) {
                                    if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId != Char.getMyChar().charID) {
                                        if (Char.getMyChar().nClass.classId != 6) {
                                            GameScr.gameAC("B\u1ea1n kh\u00f4ng ph\u1ea3i l\u00e0 qu\u1ea1t");
                                            return true;
                                        }
                                        GameScr.gameAC("Stanima HS");
                                        Code.gameAA(false, true);
                                        return true;
                                    }
                                    GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                                    return true;
                                }
                                if (var31.equals("pkb")) {
                                    GameScr.gameAC("PK Th\u1ea7n Th\u00fa");
                                    Code.gameAA(new PkBoss(TileMap.mapID));
                                    if (gameAH != null && Char.getMyChar().cName.equals(gameAH) && GameScr.vParty.size() > 1) {
                                        Service.gI().gameAK("pkm " + TileMap.mapID);
                                    }
                                    return true;
                                }
                                if (var31.equals("pkk")) {
                                    GameScr.gameAC("PK Th\u1ea7n Th\u00fa");
                                    PkBoss var37 = new PkBoss(TileMap.mapID);
                                    new PkBoss(TileMap.mapID).zoneID = var1;
                                    Code.gameAA(var37);
                                    if (gameAH != null && Char.getMyChar().cName.equals(gameAH) && GameScr.vParty.size() > 1) {
                                        Service.gI().gameAK("pkm " + TileMap.mapID);
                                        Service.gI().gameAK("pkk " + var1);
                                    }
                                    return true;
                                }
                                if (var31.equals("lb")) {
                                    var0 = "";
                                    for (var1 = 0; var1 < GameScr.vMob.size(); ++var1) {
                                        Mob var36 = (Mob)GameScr.vMob.elementAt(var1);
                                        if (!var36.isBoss) continue;
                                        var0 = var0 + var36.getTemplate().name + " lv: " + var36.level + ", ";
                                    }
                                    GameScr.gameAC("Mob: " + var0);
                                    return true;
                                }
                                if (var31.equals("tb")) {
                                    new Thread(new TimBoss()).start();
                                    return true;
                                }
                                if (var31.startsWith("tl ")) {
                                    try {
                                        String[] parts = var31.trim().split(" ");
                                        if (parts.length == 3) { // tl <itemId> <count>
                                            int itemId = Integer.parseInt(parts[1]);
                                            int count = Integer.parseInt(parts[2]);
                                            AutoSanBoss.tachDoLeById(itemId, count);
                                        } else if (parts.length == 2) { // tl <count>
                                            int count = Integer.parseInt(parts[1]);
                                            AutoSanBoss.tachDoLe(count);
                                        }
                                    } catch (Exception e) {}
                                    return true;
                                }
                                if (var31.equals("ttb")) {
                                    ThongTinBoss.toggle();
                                    return true;
                                }
                                if (var31.equals("tspkb")) {
                                    AutoSanBoss.toggle();
                                    return true;
                                }
                                if (var31.equals("tspkball") || var31.equals("all")) {
                                    AutoSanBoss.toggleALL();
                                    return true;
                                }
                                if (var31.equals("tspkbsv") || var31.equals("sv")) {
                                    AutoSanBoss.toggleSV();
                                    return true;
                                }
                                if (var31.equals("tspkbtg") || var31.equals("tg")) {
                                    AutoSanBoss.toggleTG();
                                    return true;
                                }
                                if (var31.equals("tspkbvm") || var31.equals("vm")) {
                                    AutoSanBoss.toggleVM();
                                    return true;
                                }
                                if (var31.equals("tspkbmn") || var31.equals("mn")) {
                                    AutoSanBoss.toggleMN();
                                    return true;
                                }
                                if (var31.equals("nhat")) {
                                    AutoPickup.toggle();
                                    return true;
                                }
                                if (var31.equals("moinhom") || var31.equals("mnb")) {
                                    AutoSanBoss.autoInviteFriends();
                                    return true;
                                }
                                if (var31.startsWith("tach") || var31.startsWith("tl")) {
                                    String[] parts = Code.gameAC(var0, " ");
                                    int count = 30;
                                    if (parts.length > 1) {
                                        try {
                                            count = Integer.parseInt(parts[1]);
                                        } catch (Exception e) {}
                                    }
                                    AutoSanBoss.tachDoLe(count);
                                    return true;
                                }
                                if (var31.equals("sell")) {
                                    GameScr.gameAC("Auto Sell");
                                    Code.gameAR();
                                    return true;
                                }
                                if (var31.equals("h")) {
                                    Calendar var35 = Res.gameAB();
                                    GameScr.gameAC("Time " + var35.get(11) + ":" + var35.get(12) + ":" + var35.get(13));
                                    return true;
                                }
                                if (var31.equals("dt")) {
                                    Code.gameAA(new DanTre());
                                    return true;
                                }
                                if (var31.equals("dh")) {
                                    Code.gameAA(new DanhHeo());
                                    return true;
                                }
                                if (var31.equals("nv")) {
                                    Code.gameAA(new TraNv());
                                    return true;
                                }
                                if (var31.equals("f")) {
                                    GameScr.gI().gameAD(var1);
                                    return true;
                                }
                                if (var0.equals("hd9x")) {
                                    GameScr.gameAC("Hang \u0111\u1ed9ng 9x");
                                    Code.gameAA(new Hd9x());
                                    if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                        Service.gI().gameAK("hd9x");
                                    }
                                    return true;
                                }
                                if (var0.length() == 4) {
                                    if (var0.equals("as10")) {
                                        Code.gameAA(new As10());
                                        return true;
                                    }
                                    if (var0.equals("as20")) {
                                        Code.gameAA(new As20(0));
                                        return true;
                                    }
                                    if (var0.equals("a20k")) {
                                        Code.gameAA(new As20(1));
                                        return true;
                                    }
                                    if (var0.equals("a20t")) {
                                        Code.gameAA(new As20(2));
                                        return true;
                                    }
                                    if (var0.equals("a20u")) {
                                        Code.gameAA(new As20(3));
                                        return true;
                                    }
                                    if (var0.equals("a20c")) {
                                        Code.gameAA(new As20(4));
                                        return true;
                                    }
                                    if (var0.equals("a20d")) {
                                        Code.gameAA(new As20(5));
                                        return true;
                                    }
                                    if (var0.equals("a20q")) {
                                        Code.gameAA(new As20(6));
                                        return true;
                                    }
                                } else {
                                    if (var31.equals("boss")) {
                                        GameScr.gameAC("Auto Boss " + var1);
                                        Code.gameAA(new ChoBoss(var1));
                                        return true;
                                    }
                                    if (var31.equals("kpk")) {
                                        GameScr.gameAC("Khu PK " + var1);
                                        Auto.gameAO = var1;
                                        return true;
                                    }
                                    if (var31.equals("cpk")) {
                                        GameScr.gameAC("X\u00f3a ds PK");
                                        SavePk.gameAB();
                                        return true;
                                    }
                                    if (var0.startsWith("apk")) {
                                        String[] var12 = Code.gameAC(var0, " ");
                                        if (var12.length > 1) {
                                            GameScr.gameAC("Th\u00eam " + var12[1] + " v\u00e0o ds PK");
                                            SavePk.gameAA(var12[1]);
                                        } else if (Char.getMyChar().charFocus != null) {
                                            GameScr.gameAC("Th\u00eam " + Char.getMyChar().charFocus.cName + " v\u00e0o ds PK");
                                            SavePk.gameAA(Char.getMyChar().charFocus.cName);
                                        }
                                        return true;
                                    }
                                    if (var0.startsWith("go")) {
                                        String[] stringArray = Code.gameAC(var0, " ");
                                        if (stringArray.length > 1) {
                                            GameScr.gameAC("Cho " + stringArray[1] + " DS VBL");
                                            SetAuto.tenVBL = stringArray[1];
                                            Service.gI().gameAK("go " + stringArray[1]);
                                        }
                                        return true;
                                    }
                                    if (var0.startsWith("dpk")) {
                                        String[] var12 = Code.gameAC(var0, " ");
                                        if (var12.length > 1) {
                                            GameScr.gameAC("X\u00f3a " + var12[1] + " kh\u1ecfi ds PK");
                                            SavePk.gameAB(var12[1]);
                                        } else if (Char.getMyChar().charFocus != null) {
                                            GameScr.gameAC("X\u00f3a " + Char.getMyChar().charFocus.cName + " kh\u1ecfi ds PK");
                                            SavePk.gameAB(Char.getMyChar().charFocus.cName);
                                        }
                                        return true;
                                    }
                                    if (var31.equals("chs")) {
                                        GameScr.gameAC("X\u00f3a ds HS");
                                        Code.gameAU();
                                        return true;
                                    }
                                    if (var0.startsWith("ahs")) {
                                        String[] var12 = Code.gameAC(var0, " ");
                                        if (var12.length > 1) {
                                            GameScr.gameAC("Th\u00eam " + var12[1] + " v\u00e0o ds HS");
                                            Code.gameAI(var12[1]);
                                        } else if (Char.getMyChar().charFocus != null) {
                                            GameScr.gameAC("Th\u00eam " + Char.getMyChar().charFocus.cName + " v\u00e0o ds HS");
                                            Code.gameAI(Char.getMyChar().charFocus.cName);
                                        }
                                        return true;
                                    }
                                    if (var31.equals("dhs")) {
                                        String[] var12 = Code.gameAC(var0, " ");
                                        if (var12.length > 1) {
                                            GameScr.gameAC("X\u00f3a " + var12[1] + " kh\u1ecfi ds HS");
                                            Code.gameAJ(var12[1]);
                                        } else if (Char.getMyChar().charFocus != null) {
                                            GameScr.gameAC("X\u00f3a " + Char.getMyChar().charFocus.cName + " kh\u1ecfi ds PK");
                                            Code.gameAJ(Char.getMyChar().charFocus.cName);
                                        }
                                        return true;
                                    }
                                    if (var0.startsWith("a20s")) {
                                        String[] var12 = Code.gameAC(var0, " ");
                                        if (var12.length > 1) {
                                            Code.gameAA(new As20S(var12[1]));
                                        }
                                        return true;
                                    }
                                    if (var0.startsWith("dg")) {
                                        var1 = (var0 = var0.substring(3)).indexOf(32);
                                        if (var1 > 0) {
                                            try {
                                                ItemTemplate var13 = ItemTemplates.gameAA(Short.parseShort(var0.substring(0, var1)));
                                                ChatSell.gameAA(var13, var0.substring(var1 + 1, var0.length()));
                                                GameScr.gameAC("\u0110\u1eb7t gi\u00e1: " + var13.name);
                                            }
                                            catch (Exception var17) {
                                                var17.printStackTrace();
                                            }
                                        }
                                        return true;
                                    }
                                    if (var0.startsWith("asw")) {
                                        String[] var12 = Code.gameAC(var0, " ");
                                        try {
                                            var1 = Integer.parseInt(var12[1]);
                                            int var27 = Integer.parseInt(var12[2]);
                                            ItemTemplate var39 = ItemTemplates.gameAA((short)var1);
                                            GameScr.gameAC("Th\u00eam: " + var39.name + " gi\u00e1: " + var27 + " v\u00e0o ds b\u00e1n Shinwa");
                                            Code.gameAB(var1, var27);
                                        }
                                        catch (Exception var18) {
                                            var18.printStackTrace();
                                        }
                                        return true;
                                    }
                                    if (var0.startsWith("rsw")) {
                                        String[] var12 = Code.gameAC(var0, " ");
                                        try {
                                            var1 = Integer.parseInt(var12[1]);
                                            ItemTemplate var13 = ItemTemplates.gameAA((short)var1);
                                            if (Code.gameAD(var1)) {
                                                int var25 = Code.gameAE(var1);
                                                GameScr.gameAC("X\u00f3a: " + var13.name + " gi\u00e1: " + var25 + " kh\u1ecfi ds b\u00e1n Shinwa");
                                                Code.gameAF(var1);
                                            } else {
                                                GameScr.gameAC("Item " + var13.name + " ch\u01b0a c\u00f3 trong ds b\u00e1n Shinwa");
                                            }
                                        }
                                        catch (Exception var19) {
                                            var19.printStackTrace();
                                        }
                                        return true;
                                    }
                                    if (var0.startsWith("tnx")) {
                                        if (TileMap.gameAF(TileMap.mapID)) {
                                            int var15;
                                            String[] var12 = Code.gameAC(var0, " ");
                                            try {
                                                var1 = Integer.parseInt(var12[1]);
                                            }
                                            catch (Exception var22) {
                                                return false;
                                            }
                                            try {
                                                var15 = Integer.parseInt(var12[2]);
                                            }
                                            catch (Exception var21) {
                                                var15 = 0;
                                            }
                                            int var27 = var15;
                                            try {
                                                var15 = Integer.parseInt(var12[3]);
                                            }
                                            catch (Exception var20) {
                                                var15 = 0;
                                            }
                                            GameScr.gameAC("Auto l\u00e0m " + var1 + " ti\u00ean n\u1eef");
                                            new Thread(new AutoNpc(var1, var27, var15)).start();
                                            return true;
                                        }
                                        GameScr.gameAC("H\u00e3y \u0111\u1ee9ng Tr\u01b0\u1eddng \u0111\u1ec3 auto l\u00e0m ti\u00ean n\u1eef");
                                    }
                                }
                                return false;
                            }
                            if (GameScr.vParty.size() > 0 && ((Party)GameScr.vParty.firstElement()).charId == Char.getMyChar().charID) {
                                MobTemplate var10000;
                                MobTemplate var24 = var10000 = var1 >= 0 && var1 < Mob.arrMobTemplate.length ? Mob.arrMobTemplate[var1] : null;
                                if (var10000 == null) {
                                    GameScr.gameAC("Stanima nh\u00f3m all");
                                    Code.gameAC(-1, TileMap.mapID);
                                } else {
                                    GameScr.gameAC("Stanima nh\u00f3m " + var24.name + " id " + var1);
                                    Code.gameAC(var24.mobTemplateId, TileMap.mapID);
                                }
                                Code.gameAC.gameAA = true;
                                Service.gI().gameAK("st " + Code.gameAC.mapID + " " + Code.gameAC.zoneID + " " + Code.gameAC.gameAW);
                                return true;
                            }
                            GameScr.gameAC("Ch\u01b0a c\u00f3 nh\u00f3m ho\u1eb7c b\u1ea1n kh\u00f4ng l\u00e0 nh\u00f3m tr\u01b0\u1edfng");
                            return true;
                        }
                        GameScr.gameAC("End Auto");
                        Code.gameAF();
                        if (Char.DanhNhom) {
                            Service.gI().gameAK("pe");
                        }
                        return true;
                    }
                    GameScr.gameAC("Ph\u00e1 b\u0103ng");
                    gameBE = false;
                    timBG = false;
                    return true;
                }
                GameScr.gameAC("B\u0103ng skill");
                timBG = true;
                return true;
            }
            GameScr.gameAC("B\u0103ng boss");
            gameBE = true;
            return true;
        }
        GameScr.gameAC("\u0110\u00f3ng b\u0103ng");
        gameBE = true;
        timBG = true;
        return true;
    }

    public static void gameAG(String var0) {
        for (int var1 = 0; var1 < gameCQ.length; ++var1) {
            Code.gameAA(var0, gameCQ[var1].trim());
        }
    }

    public static void gameAO() {
        if (System.currentTimeMillis() - gameCU > 60000L) {
            gameCU = System.currentTimeMillis();
            MyVector var0 = new MyVector();
            var0.addElement(Char.getMyChar());
            Service.gI().gameAA(new MyVector(), var0, 2);
        }
    }

    public static void gameAH(String var0) {
        if (System.currentTimeMillis() - gameCS >= 5000L) {
            gameCS = System.currentTimeMillis();
            Calendar var1 = Calendar.getInstance();
            var1.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            var0 = "@" + (10 + gameCT.nextInt(89)) + " " + var0 + " " + var1.get(11) + ":" + var1.get(12) + ":" + var1.get(13);
            Service.gI().gameAC(var0);
        }
    }

    public static void gameAA(String var0, String var1) {
        ChatManager.gameAD().gameAA(var0, Char.getMyChar().cName, var1);
        Service.gI().gameAA(var0, var1);
        Auto.Sleep(20L);
    }

    private static String gameAK(String var0) {
        InputStream var3 = RMS.gameAA("/" + var0);
        try {
            byte[] var1 = new byte[var3.available()];
            var3.read(var1);
            var0 = new String(var1, "UTF-8");
        }
        catch (Exception var2) {
            var0 = "";
        }
        return var0;
    }

    public static void gameAB(String var0, String var1) {
        boolean var16;
        ChatTab var2;
        if (Char.DanhNhom && gameAH != null && var0.equals(gameAH) && !Char.getMyChar().cName.equals(gameAH)) {
            Code.gameAD(var0, var1);
        }
        if ((var2 = ChatManager.gameAD().gameAA(var0)) == null) {
            var16 = true;
        } else if (System.currentTimeMillis() - var2.gameAD > 1000L) {
            var2.gameAD = System.currentTimeMillis();
            var16 = true;
        } else {
            var16 = false;
        }
        String[] vaar7 = Code.gameAC(FormDanhVong.TenDoiThu, ",");
        for (int vara8 = 0; vara8 < vaar7.length; ++vara8) {
            if (!var0.equals(vaar7[vara8])) continue;
            if (var1.toLowerCase().equals("lodai")) {
                Code.gameAF();
                gameCG.gameAL();
                Code.gameAA(gameCG);
            }
            if (!var1.toLowerCase().equals("cusat")) continue;
            Code.gameAF();
            Code.gameAA(new LossPk());
        }
        for (int i = 0; i < Code.gameAC(AutoNhan.nguoinhan, ",").length; ++i) {
            if (!var0.equals(Code.gameAC(AutoNhan.nguoinhan, ",")[i]) || !var1.startsWith("cutve") || Code.gameAC(AutoNhan.nguoinhan, ",")[i] == null) continue;
            SetGomDo.itemgd = Code.gameAC(var1, " ")[3];
            Code.gameAA(new AutoGuiDo(Integer.parseInt(Code.gameAC(var1, " ")[1]), Integer.parseInt(Code.gameAC(var1, " ")[2]), var0));
            return;
        }
        if (var16) {
            Char var17 = Char.getMyChar();
            if (var1.startsWith("checkup")) {
                Code.gameAA(var1, "H\u00e0nh Trang: ctt=" + Char.gameAH(454).size() + " ;tts= " + Char.gameAH(455).size() + " ;ttt= " + Char.gameAH(456).size() + " ;ttc= " + Char.gameAH(457).size());
                Code.gameAA(var1, " Ru\u01a1ng: ctt= " + Char.CheckItemBox(454).size() + " ;tts= " + Char.CheckItemBox(455).size() + " ;ttt= " + Char.CheckItemBox(456).size() + " ;ttc= " + Char.CheckItemBox(457).size());
            }
            if (var1.toLowerCase().equals("yenxu")) {
                Code.gameAA(var0, "Y\u00ean: 2 T\u1ecfi - Xu: 2 T\u1ecfi - L\u01b0\u1ee3ng: 2 T\u1ecfi");
            } else {
                if (var1.toLowerCase().equals("level")) {
                    long var19 = (Char.getMyChar().cExpDown > 0L ? Char.getMyChar().cExpDown : Char.getMyChar().autoUpHp) * 10000L / GameScr.exps[Char.getMyChar().clevel];
                    long var7 = var19 % 100L;
                    Code.gameAA(var0, "LV: " + var17.clevel + " + " + (Char.getMyChar().cExpDown > 0L ? "-" : "") + var19 / 100L + "." + (var7 < 10L ? "0" + var7 : String.valueOf(var7)) + "%");
                    return;
                }
                if (gameAB != null && gameBD > 0L) {
                    if (var1.toLowerCase().equals("time")) {
                        Code.gameAA(var0, "Th\u1eddi gian c\u00f2n l\u1ea1i: " + NinjaUtil.gameAB((int)(gameBD / 1000L)));
                        return;
                    }
                } else if (gameAB instanceof AutoSell) {
                    gameAF.gameAA(var0, var1);
                    return;
                }
            }
        }
    }

    public static String[] gameAC(String var0, String var1) {
        int var2 = 0;
        int var3 = var1.length();
        int var4 = var0.indexOf(var1, 0);
        while (var4 != -1) {
            var4 += var3;
            var4 = var0.indexOf(var1, var4);
            ++var2;
        }
        String[] var7 = new String[var2 + 1];
        var4 = var0.indexOf(var1);
        int var5 = 0;
        int var6 = 0;
        while (var4 != -1) {
            var7[var6] = var0.substring(var5, var4);
            var5 = var4 + var3;
            var4 = var0.indexOf(var1, var5);
            ++var6;
        }
        var7[var6] = var0.substring(var5, var0.length());
        return var7;
    }

    public static void gameAD(String var0, String var1) {
        if (Char.DanhNhom && gameAH != null && var0.equals(gameAH) && !Char.getMyChar().cName.equals(gameAH)) {
            String[] var5 = Code.gameAC(var1, " ");
            try {
                if (var5[0].equals("dcvt")) {
                    gameAR = Integer.parseInt(var5[1]) == 1;
                    return;
                }
                if (var5[0].equals("setup")) {
                    Char.DungHp = "true".equalsIgnoreCase(var5[1]) || "1".equals(var5[1]);
                    Char.aHpValue = Integer.valueOf(var5[2]);
                    Char.DungMp = "true".equalsIgnoreCase(var5[3]) || "1".equals(var5[3]);
                    Char.aMpValue = Integer.valueOf(var5[4]);
                    Char.DungFood = "true".equalsIgnoreCase(var5[5]) || "1".equals(var5[5]);
                    Char.CapFood = Integer.valueOf(var5[6]);
                    Char.DungHoTro = "true".equalsIgnoreCase(var5[7]) || "1".equals(var5[7]);
                    Char.DungPhanThan = "true".equalsIgnoreCase(var5[8]) || "1".equals(var5[8]);
                    Char.KhienMana = "true".equalsIgnoreCase(var5[9]) || "1".equals(var5[9]);
                    Char.DotQuai = "true".equalsIgnoreCase(var5[10]) || "1".equals(var5[10]);
                    Char.NhatYen = "true".equalsIgnoreCase(var5[11]) || "1".equals(var5[11]);
                    Char.NhatHpMp = "true".equalsIgnoreCase(var5[12]) || "1".equals(var5[12]);
                    Char.CapHpMp = Integer.valueOf(var5[13]);
                    Char.NhatDa = "true".equalsIgnoreCase(var5[14]) || "1".equals(var5[14]);
                    Char.CapNhatDa = Integer.valueOf(var5[15]);
                    Char.LuyenDa = "true".equalsIgnoreCase(var5[16]) || "1".equals(var5[16]);
                    Char.LuyenDaMax = Integer.valueOf(var5[17]);
                    Char.NhatTrangBi = "true".equalsIgnoreCase(var5[18]) || "1".equals(var5[18]);
                    Char.CapTrangBi = Integer.valueOf(var5[19]);
                    Char.NhatTbLa = "true".equalsIgnoreCase(var5[20]) || "1".equals(var5[20]);
                    Char.NhatVpNV = "true".equalsIgnoreCase(var5[21]) || "1".equals(var5[21]);
                    Char.NhatVpSk = "true".equalsIgnoreCase(var5[22]) || "1".equals(var5[22]);
                    Char.NhatAll = "true".equalsIgnoreCase(var5[23]) || "1".equals(var5[23]);
                    Char.NhatSvc = "true".equalsIgnoreCase(var5[24]) || "1".equals(var5[24]);
                    Char.KhongNhat = "true".equalsIgnoreCase(var5[25]) || "1".equals(var5[25]);
                    Char.LuyenTTT = "true".equalsIgnoreCase(var5[26]) || "1".equals(var5[26]);
                    Char.LuyenTTC = "true".equalsIgnoreCase(var5[27]) || "1".equals(var5[27]);
                    Char.ReMap = "true".equalsIgnoreCase(var5[28]) || "1".equals(var5[28]);
                    Char.TsMapTrong = "true".equalsIgnoreCase(var5[29]) || "1".equals(var5[29]);
                    Char.AutoMuaFood = "true".equalsIgnoreCase(var5[30]) || "1".equals(var5[30]);
                    Char.DieHetMp = "true".equalsIgnoreCase(var5[31]) || "1".equals(var5[31]);
                    Char.ReConnect = "true".equalsIgnoreCase(var5[32]) || "1".equals(var5[32]);
                    Char.ChuyenMapHetQuai = "true".equalsIgnoreCase(var5[33]) || "1".equals(var5[33]);
                    Char.SanTaTl = "true".equalsIgnoreCase(var5[34]) || "1".equals(var5[34]);
                    Char.DanhQuai = "true".equalsIgnoreCase(var5[35]) || "1".equals(var5[35]);
                    Char.DanhTA = "true".equalsIgnoreCase(var5[36]) || "1".equals(var5[36]);
                    Char.DanhTL = "true".equalsIgnoreCase(var5[37]) || "1".equals(var5[37]);
                    Char.CongTN = "true".equalsIgnoreCase(var5[38]) || "1".equals(var5[38]);
                    Char.CongKN = "true".equalsIgnoreCase(var5[39]) || "1".equals(var5[39]);
                    Char.DanhNhom = "true".equalsIgnoreCase(var5[40]) || "1".equals(var5[40]);
                    Char.NePk = "true".equalsIgnoreCase(var5[41]) || "1".equals(var5[41]);
                    Char.DungCoLenh = "true".equalsIgnoreCase(var5[42]) || "1".equals(var5[42]);
                    Char.MuaCoLenh = "true".equalsIgnoreCase(var5[43]) || "1".equals(var5[43]);
                    Char.gameAC();
                    return;
                }
                if (var5[0].equals("vbl")) {
                    SetAuto.vbl = Integer.parseInt(var5[1]) == 1;
                    return;
                }
                if (var5[0].equals("go")) {
                    SetAuto.tenVBL = var5[1];
                    return;
                }
                if (var5[0].equals("avt")) {
                    GameScr.gameAC("Th\u00eam v\u1ecb tr\u00ed " + gameAT.size());
                    gameAT.addElement(Integer.valueOf(var5[1]));
                    gameAU.addElement(Integer.valueOf(var5[2]));
                    return;
                }
                if (var5[0].equals("dvt")) {
                    GameScr.gameAC("X\u00f3a h\u1ebft v\u1ecb tr\u00ed");
                    gameAT.removeAllElements();
                    gameAU.removeAllElements();
                    return;
                }
                if (var5[0].equals("dvtx")) {
                    int var6 = Integer.parseInt(var5[1]);
                    GameScr.gameAC("X\u00f3a v\u1ecb tr\u00ed " + var6);
                    gameAT.removeElementAt(var6);
                    gameAU.removeElementAt(var6);
                    return;
                }
                if (var5[0].equals("pe")) {
                    GameScr.gameAC("End Auto");
                    LockGame.gameBK();
                    gameAB = null;
                    return;
                }
                if (var5[0].equals("tsa")) {
                    if (gameAB == gameCE) {
                        Code.gameCE.mapID = Integer.parseInt(var5[1]);
                        Code.gameCE.zoneID = Integer.parseInt(var5[2]);
                        return;
                    }
                    gameCC.gameAA(-1, Integer.parseInt(var5[1]), Integer.parseInt(var5[2]));
                    Code.gameCC.gameAA = true;
                    Code.gameAA(gameCC);
                    return;
                }
                if (var5[0].equals("ts")) {
                    if (gameAB == gameCE) {
                        Code.gameCE.mapID = Integer.parseInt(var5[1]);
                        Code.gameCE.zoneID = Integer.parseInt(var5[2]);
                        return;
                    }
                    gameCC.gameAA(Integer.parseInt(var5[3]), Integer.parseInt(var5[1]), Integer.parseInt(var5[2]));
                    Code.gameCC.gameAA = true;
                    Code.gameAA(gameCC);
                    return;
                }
                if (var5[0].equals("att")) {
                    if (gameAB == gameCE) {
                        Code.gameCE.mapID = Integer.parseInt(var5[1]);
                        Code.gameCE.zoneID = Integer.parseInt(var5[2]);
                        return;
                    }
                    int var6 = Integer.parseInt(var5[1]);
                    int var8 = Integer.parseInt(var5[3]);
                    TaskOrder var3 = Char.gameAM(1);
                    if (var3 != null && var3.mapId == var6) {
                        gameAE.gameAC();
                    } else {
                        gameAE.gameAC(var6, var8);
                    }
                    Code.gameAE.zoneID = Integer.parseInt(var5[2]);
                    Code.gameAE.gameAA = true;
                    Code.gameAA(gameAE);
                    return;
                }
                if (var5[0].equals("sta")) {
                    if (gameAB == gameCE) {
                        Code.gameCE.mapID = Integer.parseInt(var5[1]);
                        Code.gameCE.zoneID = Integer.parseInt(var5[2]);
                        return;
                    }
                    gameAC.gameAA(-1, Integer.parseInt(var5[1]), Integer.parseInt(var5[2]), false, false);
                    Code.gameAC.gameAA = true;
                    Code.gameAA(gameAC);
                    return;
                }
                if (var5[0].equals("st")) {
                    if (gameAB == gameCE) {
                        Code.gameCE.mapID = Integer.parseInt(var5[1]);
                        Code.gameCE.zoneID = Integer.parseInt(var5[2]);
                        return;
                    }
                    gameAC.gameAA(Integer.parseInt(var5[3]), Integer.parseInt(var5[1]), Integer.parseInt(var5[2]), false, false);
                    Code.gameAC.gameAA = true;
                    Code.gameAA(gameAC);
                    return;
                }
                if (var5[0].equals("hd9x")) {
                    Code.gameAA(new Hd9x());
                    return;
                }
                if (var5[0].startsWith("vdmq")) {
                    Code.nhat[10] = 454;
                    Code.nhat[11] = 455;
                    Code.nhat[12] = 456;
                    Code.nhat[13] = 457;
                    Code.nhat[14] = 573;
                    Code.nhat[15] = 574;
                    Code.nhat[16] = 575;
                    Code.nhat[17] = 38;
                    Code.nhat[18] = 383;
                    Code.nhat[19] = 384;
                    Code.nhat[20] = 545;
                    Code.nhat[21] = 524;
                    Code.nhat[22] = 449;
                    Code.nhat[23] = 450;
                    Code.nhat[24] = 451;
                    Code.nhat[25] = 452;
                    Code.nhat[26] = 453;
                    Code.nhat[27] = 576;
                    Code.nhat[28] = 577;
                    Code.nhat[29] = 578;
                    return;
                }
                if (var5[0].equals("pkms")) {
                    if (gameAB instanceof PkBossS) {
                        PkBossS var2 = (PkBossS)gameAB;
                        ((PkBossS)gameAB).mapID = Integer.parseInt(var5[1]);
                        var2.gameAW = Integer.parseInt(var5[2]);
                        var2.gameAV = 3;
                        return;
                    }
                } else if (var5[0].equals("pkes")) {
                    if (gameAB instanceof PkBossS) {
                        ((PkBossS)Code.gameAB).gameAV = 4;
                        return;
                    }
                } else {
                    if (var5[0].equals("pkm")) {
                        if (gameAB == gameCE) {
                            Code.gameCE.mapID = Integer.parseInt(var5[1]);
                            return;
                        }
                        Auto var7 = gameAB instanceof PkBoss ? Code.gameAB.reAB : gameAB;
                        PkBoss pBoss = new PkBoss(Integer.parseInt(var5[1]));
                        pBoss.reAB = var7;
                        ChatRouter.startPartyBoss(pBoss);
                        return;
                    }
                    if (var5[0].equals("pkk")) {
                        if (gameAB instanceof PkBoss || gameAB == gameCE) {
                            ChatRouter.setPartyBossZone(Integer.parseInt(var5[1]));
                            return;
                        }
                    } else if (var5[0].equals("pke")) {
                        if (gameAB instanceof PkBoss) {
                            ChatRouter.stopPartyBoss();
                            return;
                        }
                    } else if (gameAB != null) {
                        if (var5[0].equals("map")) {
                            Code.gameAB.mapID = Integer.parseInt(var5[1]);
                            return;
                        }
                        if (var5[0].equals("khu")) {
                            Code.gameAB.zoneID = Integer.parseInt(var5[1]);
                            return;
                        }
                        if (gameAB instanceof TaThu) {
                            if (var5[0].equals("waitGr")) {
                                TaThu.gameAX = System.currentTimeMillis();
                                TaThu.gameAW = true;
                                return;
                            }
                            if (var5[0].equals("notifyGr")) {
                                TaThu.gameAW = false;
                                return;
                            }
                        } else if (gameAB instanceof Stanima && var5[0].equals("sts")) {
                            gameAC.gameAL();
                            return;
                        }
                    }
                }
                return;
            }
            catch (Exception var4) {
                var4.printStackTrace();
            }
        }
    }

    static {
        gameCS = 0L;
        gameCT = new Random();
        gameCU = 0L;
        Code.gameAP();
    }
}
