/**
 * GhostBoss v4 - Danh boss VO HINH + Multi-Hit + Skill Attack.
 *
 * Tricks exploit:
 * 1. GHOST: Boss ko can hien thi, dung tai vi tri spawn, brute-force mobId
 * 2. MULTI-HIT: Gui cung 1 mobId nhieu lan trong 1 packet → nhieu hit/packet
 * 3. SKILL ATK: Chon skill manh nhat truoc khi gui attack → dame gap boi
 * 4. NO COOLDOWN: Spam skill lien tuc khong can doi CD (server co the ko check)
 * 5. AUTO BUFF: Tu dong buff truoc khi danh (tang dame)
 *
 * Lenh:
 *   "gb" / "gb63" — ghost boss map 63 (id 211)
 *   "gb all"      — danh TAT CA quai trong map, xa toan map
 */
public final class GhostBoss implements Runnable {
    public static boolean isRunning = false;
    private static Thread thread;
    private static int targetMapId = -1;
    private static int mode = 0;
    private static final int MODE_BOSS = 0;
    private static final int MODE_ALL = 1;

    // Config
    private static final int ATTACK_DELAY_MS = 30;
    private static final int ROUND_DELAY_MS = 300;
    private static final int BOSS_ID_M63 = 211;     // ID boss map 63
    private static final int HITS_PER_PACKET = 3;

    // === REUSABLE VECTORS (tranh tao moi moi frame = giam GC/RAM) ===
    private static final MyVector reusableMobs = new MyVector();
    private static final MyVector reusableChars = new MyVector();

    // Toa do boss spawn — {mapId, x, y}
    private static final int[][] BOSS_POSITIONS = {
        {63, 1124, 264},
    };

    private GhostBoss() {}

    public static void toggle() {
        if (isRunning) {
            stop();
            GameScr.gameAC("GhostBoss: OFF");
        } else {
            int mapId = findActiveBossMap();
            if (mapId < 0) {
                GameScr.gameAC("GB: Khong boss spawn! Thu gb63.");
                return;
            }
            startOnMap(mapId);
        }
    }

    public static void startOnMap(int mapId) {
        if (isRunning) stop();
        targetMapId = mapId;
        mode = MODE_BOSS;
        isRunning = true;
        thread = new Thread(new GhostBoss());
        thread.start();
        GameScr.gameAC("GB Boss: ON - M" + mapId + " (id=" + BOSS_ID_M63 + ")");
    }

    /**
     * Mode ALL: danh tat ca quai trong map hien tai, xa toan map.
     */
    public static void startAll() {
        if (isRunning) {
            stop();
            GameScr.gameAC("GB ALL: OFF");
            return;
        }
        targetMapId = TileMap.mapID;
        mode = MODE_ALL;
        isRunning = true;
        thread = new Thread(new GhostBoss());
        thread.start();
        GameScr.gameAC("GB ALL: ON - Danh tat ca quai M" + targetMapId);
    }

    public static void stop() {
        isRunning = false;
        targetMapId = -1;
        mode = MODE_BOSS;
    }

    public void run() {
        sleep(500);
        if (mode == MODE_ALL) {
            runAllMode();
        } else {
            runBossMode();
        }
        isRunning = false;
        GameScr.gameAC("GB: Dung.");
    }

    // ========== MODE ALL: danh tat ca quai, xa toan map ==========

    private void runAllMode() {
        Char myChar = Char.getMyChar();
        if (myChar == null) return;

        int bestSkillId = findBestAttackSkill(myChar);
        autoBuff(myChar);

        while (isRunning) {
            try {
                if (myChar.cName == null) { waitReconnect(); continue; }

                // Hoi sinh
                if (myChar.statusMe == 14 || myChar.cHP <= 0) {
                    respawnFast(); sleep(1000); continue;
                }

                // Lay danh sach tat ca mob trong zone
                if (GameScr.vMob == null || GameScr.vMob.size() == 0) {
                    sleep(500); continue;
                }

                // Chon skill
                if (bestSkillId >= 0) {
                    try { Service.gI().gameAG(bestSkillId); } catch (Exception e) {}
                }

                // Gui attack cho TAT CA mob trong vMob (reuse vector, khong tao moi)
                reusableMobs.removeAllElements();
                try {
                    for (int i = 0; i < GameScr.vMob.size(); i++) {
                        Object o = GameScr.vMob.elementAt(i);
                        if (o instanceof Mob) {
                            Mob mob = (Mob) o;
                            if (mob.hp > 0 && mob.status != 0 && mob.status != 1) {
                                reusableMobs.addElement(mob);
                            }
                        }
                    }
                } catch (Exception e) {}

                if (reusableMobs.size() > 0) {
                    reusableChars.removeAllElements();
                    int sType = bestSkillId >= 0 ? 2 : 1;
                    Service.gI().gameAA(reusableMobs, reusableChars, sType);
                }

                sleep(ATTACK_DELAY_MS);

                // Nhat do
                grabItems();

            } catch (Exception e) {
                sleep(500);
            }
        }
    }

    // ========== MODE BOSS: ghost boss map 63 ==========

    private void runBossMode() {
        while (isRunning) {
            try {
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cName == null) {
                    waitReconnect(); continue;
                }
                if (TileMap.mapID != targetMapId) {
                    GameScr.gameAC("GB: Di toi M" + targetMapId + "...");
                    travelToMap(targetMapId);
                    if (!isRunning || TileMap.mapID != targetMapId) {
                        sleep(5000); continue;
                    }
                }
                walkToBossPosition();
                autoBuff(myChar);
                GameScr.gameAC("GB: GHOST ATTACK M" + targetMapId + " id=" + BOSS_ID_M63);
                ghostAttack(myChar);
            } catch (Exception e) {
                sleep(3000);
            }
        }
    }

    /**
     * TRICK 5: Auto Buff — tu dong buff tat ca skill type 2 de tang dame.
     */
    private void autoBuff(Char myChar) {
        try {
            if (myChar.vSkillFight == null) return;
            for (int i = 0; i < myChar.vSkillFight.size(); i++) {
                Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                if (s == null || s.template == null) continue;
                if (s.template.type == 2) {
                    // Skill type 2 = buff/debuff. Dung gameAR de tu buff.
                    Service.gI().gameAR(s.template.id);
                    sleep(100);
                }
            }
        } catch (Exception e) {}
    }

    /**
     * CORE: Ghost Attack voi Multi-Hit + Skill Attack.
     */
    private void ghostAttack(Char myChar) {
        long startTime = System.currentTimeMillis();
        int round = 0;

        // Tim skill manh nhat de dung
        int bestSkillId = findBestAttackSkill(myChar);

        while (isRunning && TileMap.mapID == targetMapId) {

            // Hoi sinh neu chet
            if (myChar.statusMe == 14 || myChar.cHP <= 0) {
                respawnFast();
                sleep(1000);
                if (TileMap.mapID != targetMapId) return;
                walkToBossPosition();
                autoBuff(myChar);
            }

            // === THU BOSS THAT TRUOC ===
            Mob realBoss = findBossInVMob();
            if (realBoss != null) {
                GameScr.gameAC("GB: Boss that! mobId=" + realBoss.mobId);
                myChar.mobFocus = realBoss;
                while (isRunning && realBoss.hp > 0 && realBoss.status != 0
                       && TileMap.mapID == targetMapId) {
                    sleep(500);
                    try { if (!GameScr.vMob.contains(realBoss)) break; } catch (Exception e) { break; }
                }
                myChar.mobFocus = null;
                GameScr.gameAC("GB: Boss chet! Nhat do...");
                grabItems(); sleep(3000); continue;
            }

            // === GHOST ATTACK: Spam thang id boss 211 ===
            if (myChar.cName == null) return;

            // TRICK 3+4: Chon skill truoc moi hit (bypass CD)
            if (bestSkillId >= 0) {
                try { Service.gI().gameAG(bestSkillId); } catch (Exception e) {}
            }

            // TRICK 1+2: Multi-hit + Skill attack — chi target id 211
            sendMultiHitAttack((short)BOSS_ID_M63, bestSkillId >= 0 ? 2 : 1);

            sleep(ATTACK_DELAY_MS);

            round++;
            if (round % 10 == 0) {
                long sec = (System.currentTimeMillis() - startTime) / 1000;
                GameScr.gameAC("GB: " + round + " rnd, " + sec + "s");
            }

            grabItems();

            if (System.currentTimeMillis() - startTime > 2400000L) {
                GameScr.gameAC("GB: Het 40p.");
                return;
            }

            sleep(ROUND_DELAY_MS);
        }
    }

    /**
     * TRICK 1: Multi-Hit — gui cung 1 mobId nhieu lan trong 1 packet.
     * Packet format: writeByte(count) + writeByte(mobId) × count
     * Server co the tinh damage nhieu lan!
     */
    private static void sendMultiHitAttack(short mobId, int skillType) {
        try {
            // Tao nhieu fake mob cung mobId
            MyVector mobs = new MyVector();
            for (int i = 0; i < HITS_PER_PACKET; i++) {
                Mob fake = new Mob(
                    mobId, false, false, false, false, false,
                    1, 0, 99999, 99999, 1,
                    (short)1124, (short)264,   // vi tri boss M63
                    (byte)2, (byte)0, true, false
                );
                mobs.addElement(fake);
            }
            MyVector chars = new MyVector();

            // Gui attack packet
            Service.gI().gameAA(mobs, chars, skillType);
        } catch (Exception e) {}
    }

    /**
     * TRICK 2: Tim skill attack manh nhat (type 1 hoac 3) de dung.
     */
    private int findBestAttackSkill(Char myChar) {
        try {
            if (myChar.vSkillFight == null) return -1;
            int bestId = -1;
            for (int i = 0; i < myChar.vSkillFight.size(); i++) {
                Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                if (s == null || s.template == null) continue;
                // Type 1 = attack, type 3 = AOE attack
                if (s.template.type == 1 || s.template.type == 3) {
                    bestId = s.template.id;
                    // Uu tien type 3 (AOE)
                    if (s.template.type == 3) return bestId;
                }
            }
            return bestId;
        } catch (Exception e) { return -1; }
    }

    private static Mob findBossInVMob() {
        try {
            if (GameScr.vMob == null) return null;
            for (int i = 0; i < GameScr.vMob.size(); i++) {
                Object o = GameScr.vMob.elementAt(i);
                if (o instanceof Mob) {
                    Mob mob = (Mob) o;
                    if (mob.isBoss && mob.hp > 0 && mob.status != 0 && mob.status != 1)
                        return mob;
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private void walkToBossPosition() {
        int bx = -1, by = -1;
        for (int i = 0; i < BOSS_POSITIONS.length; i++) {
            if (BOSS_POSITIONS[i][0] == targetMapId) {
                bx = BOSS_POSITIONS[i][1]; by = BOSS_POSITIONS[i][2]; break;
            }
        }
        if (bx < 0) return;
        Char myChar = Char.getMyChar();
        if (myChar == null) return;

        Char.gameAC(bx, by);
        for (int w = 0; w < 100 && isRunning; w++) {
            int dx = myChar.cx - bx; if (dx < 0) dx = -dx;
            int dy = myChar.cy - by; if (dy < 0) dy = -dy;
            if (dx < 50 && dy < 50) break;
            sleep(100);
        }
    }

    private static int findActiveBossMap() {
        int[][] data = {{63, 12, 18, 20, 22}};
        java.util.Calendar cal = java.util.Calendar.getInstance(
            java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int now = cal.get(java.util.Calendar.HOUR_OF_DAY) * 3600
                + cal.get(java.util.Calendar.MINUTE) * 60
                + cal.get(java.util.Calendar.SECOND);
        for (int i = 0; i < data.length; i++) {
            for (int j = 1; j < data[i].length; j++) {
                int diff = now - data[i][j] * 3600;
                if (diff >= 0 && diff < 2400) return data[i][0];
            }
        }
        return -1;
    }

    private void travelToMap(int mapId) {
        try { Code.gameAA(new PkBoss(mapId)); } catch (Exception e) { return; }
        for (int w = 0; w < 300 && isRunning && TileMap.mapID != mapId; w++) sleep(100);
        try { if (Code.gameAB instanceof PkBoss) Code.gameAC(); } catch (Exception e) {}
    }

    private void respawnFast() {
        for (int i = 0; i < 10 && isRunning; i++) {
            try {
                GameCanvas.endDlg(); sleep(10);
                Auto.gameAN.removeAllElements();
                Auto.gameAM = false;
                GameScr.gameAB(5, 0, 0); sleep(10);
                if (Code.HoiSinhLuong && Char.getMyChar().luong > 0) {
                    Service.gI().gameAL();
                } else {
                    Service.gI().gameAK();
                    TileMap.gameAF();
                }
                sleep(300);
                if (Char.getMyChar().statusMe != 14 && Char.getMyChar().cHP > 0) return;
            } catch (Exception e) { return; }
            sleep(200);
        }
    }

    private void grabItems() {
        try { AutoPickup.grabOnce(); } catch (Exception e) {}
    }

    private void waitReconnect() {
        GameScr.gameAC("GB: Mat ket noi...");
        for (int i = 0; i < 60 && isRunning; i++) {
            sleep(1000);
            try { if (Char.getMyChar() != null && Char.getMyChar().cName != null) { sleep(3000); return; } } catch (Exception e) {}
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
