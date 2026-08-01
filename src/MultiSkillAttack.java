/**
 * MultiSkillAttack v2 — Auto Multi-Skill + Auto Buff
 *
 * C\u1ea3i ti\u1ebfn t\u1eeb v1 d\u1ef1a tr\u00ean ph\u00e2n t\u00edch bytecode D\u1ee9a Mod:
 * P1: Cooldown check — kh\u00f4ng g\u1eedi skill ch\u01b0a h\u1ebft CD
 * P2: Auto Buff — t\u1ef1 d\u00f9ng skill type 2 (buff) v\u1edbi gameAR()
 * P3: Elemental Intelligence — skip fire tr\u00ean mob \u0111ang ch\u00e1y, etc.
 * P4: Smart Conditions — \u0111i\u1ec1u ki\u1ec7n \u0111\u1eb7c bi\u1ec7t cho buff (Khi\u00ean Mana, \u0110\u1ed1t Qu\u00e1i, Ph\u00e2n Th\u00e2n)
 *
 * G\u1ecdi t\u1eeb Auto.gameAG() (bytecode patched).
 */
public class MultiSkillAttack {

    // Delay gi\u1eefa m\u1ed7i skill (ms) - \u0111\u1ee7 \u0111\u1ec3 server kh\u00f4ng drop packet
    private static final long SKILL_DELAY_MS = 40L;
    // Th\u1eddi gian tr\u1eeb cooldown \u0111\u1ec3 d\u00f9ng s\u1edbm h\u01a1n 1 ch\u00FAt (gi\u1ed1ng d\u1ee9a mod: 300ms)
    private static final long CD_BUFFER_MS = 300L;

    /**
     * Entry point ch\u00ednh: g\u1eedi t\u1ea5t c\u1ea3 skill attack + buff.
     */
    public static void attackMultiSkill(Char myChar, MyVector reAE, MyVector reAF) {
        if (myChar == null) return;

        // L\u01b0u skill UI hi\u1ec7n t\u1ea1i \u0111\u1ec3 restore sau
        Skill originalSelectedSkill = myChar.myskill;

        // === PHASE 1: Auto Buff (skill type 2) ===
        autoBuff(myChar);

        // === PHASE 2: Multi-Skill Attack (type 0, 1, 3) ===
        boolean firedAssigned = false;

        // T\u00ECm mob focus (mob \u0111\u01b0\u1ee3c ch\u1ecdn target hi\u1ec7n t\u1ea1i)
        Mob focusMob = myChar.mobFocus;

        // Uu ti\u00ean ph\u00edm t\u1eaft (gamePB)
        Skill[] assignedSkills = GameScr.gamePB;
        if (assignedSkills != null && assignedSkills.length > 0) {
            for (int i = 0; i < assignedSkills.length; i++) {
                try {
                    Skill s = assignedSkills[i];
                    if (fireAttackSkill(myChar, s, reAE, reAF, focusMob)) {
                        firedAssigned = true;
                    }
                } catch (Exception e) {}
            }
        }

        // Fallback: d\u00f9ng vSkillFight n\u1ebfu ch\u01b0a g\u00e1n ph\u00edm t\u1eaft
        if (!firedAssigned && myChar.vSkillFight != null) {
            int size = myChar.vSkillFight.size();
            for (int i = 0; i < size; i++) {
                try {
                    Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                    fireAttackSkill(myChar, s, reAE, reAF, focusMob);
                } catch (Exception e) {}
            }
        }

        // Restore UI skill
        myChar.myskill = originalSelectedSkill;
    }

    // ========================
    // PHASE 1: AUTO BUFF
    // ========================

    /**
     * T\u1ef1 d\u00f9ng t\u1ea5t c\u1ea3 skill buff (type 2) ch\u01b0a active.
     * Dùng packet gameAR() thay v\u00ec gameAA().
     * Logic copy t\u1eeb d\u1ee9a mod bytecode.
     */
    private static void autoBuff(Char myChar) {
        if (myChar.vSkillFight == null) return;
        // Ch\u1ec9 buff khi DungHoTro = true ho\u1eb7c l\u00e0 As20 (ki\u1ec3u auto \u0111\u1eb7c bi\u1ec7t)
        // Kh\u00f4ng check l\u1ea1i As20 v\u00ec kh\u00f4ng c\u00f3 ref — lu\u00f4n cho ph\u00e9p buff
        if (!Char.DungHoTro) return;

        int size = myChar.vSkillFight.size();
        for (int i = 0; i < size; i++) {
            try {
                Skill s = (Skill) myChar.vSkillFight.elementAt(i);
                if (s == null || s.template == null) continue;
                if (s.template.type != 2) continue;

                // P1: Check cooldown
                if (!isSkillReady(s)) continue;

                // P4: Check \u0111i\u1ec1u ki\u1ec7n \u0111\u1eb7c bi\u1ec7t cho t\u1eebng skill buff
                if (!checkBuffConditions(myChar, s)) continue;

                // Check hi\u1ec7u \u1ee9ng \u0111\u00e3 active ch\u01b0a
                if (isBuffActive(myChar, s)) continue;

                // === G\u1eedi buff ===
                s.lastTimeUseThisSkill = System.currentTimeMillis();
                Service.gI().gameAG(s.template.id);  // Select skill
                Service.gI().gameAR();                // Buff self (KHAC v\u1edbi gameAA attack)

                // V\u1ebd hi\u1ec7u \u1ee9ng
                if (!Code.timBG && GameScr.sks != null
                        && s.template.id >= 0 && s.template.id < GameScr.sks.length) {
                    myChar.gameAB(GameScr.sks[s.template.id], 0);
                }

                try { Thread.sleep(100L); } catch (Exception ex) {}
            } catch (Exception e) {}
        }
    }

    /**
     * Ki\u1ec3m tra buff (Effect) c\u00f3 \u0111ang active tr\u00ean nh\u00e2n v\u1eadt kh\u00f4ng.
     * Logic: so icon c\u1ee7a skill v\u1edbi icon c\u1ee7a effect, c\u00f2n \u00edt nh\u1ea5t 2s th\u00ec skip.
     */
    private static boolean isBuffActive(Char myChar, Skill s) {
        if (myChar.vEff == null) return false;
        int nowSec = (int) (System.currentTimeMillis() / 1000L);
        int size = myChar.vEff.size();
        for (int i = 0; i < size; i++) {
            try {
                Effect eff = (Effect) myChar.vEff.elementAt(i);
                if (eff == null || eff.template == null) continue;

                boolean match = (eff.template.iconId == s.template.iconId);
                // Skill 58 match qua effect type 7 (theo d\u1ee9a mod bytecode)
                if (!match && s.template.id == 58 && eff.template.type == 7) {
                    match = true;
                }

                if (match) {
                    int remaining = eff.timeLenght - (nowSec - eff.timeStart);
                    if (remaining >= 2) {
                        return true; // Buff v\u1eabn c\u00f2n, kh\u00f4ng c\u1ea7n d\u00f9ng l\u1ea1i
                    }
                }
            } catch (Exception e) {}
        }
        return false;
    }

    /**
     * P4: \u0110i\u1ec1u ki\u1ec7n \u0111\u1eb7c bi\u1ec7t cho m\u1ed7i lo\u1ea1i buff.
     * Logic tr\u00edch t\u1eeb d\u1ee9a mod bytecode.
     */
    private static boolean checkBuffConditions(Char myChar, Skill s) {
        int id = s.template.id;

        // Skill 31 (Khi\u00ean Mana): ch\u1ec9 d\u00f9ng khi ch\u01b0a c\u00f3 khi\u00ean
        if (id == 31 && Char.KhienMana) return false;

        // Skill 15 (\u0110\u1ed1t Qu\u00e1i): ch\u1ec9 d\u00f9ng khi DotQuai=true v\u00e0 HP < aHpValue%
        if (id == 15) {
            if (!Char.DotQuai) return false;
            if (!myChar.isHuman) return false;
            if (myChar.cHP >= (myChar.cMaxHP * Char.aHpValue / 100)) return false;
        }

        // Skill 6: ch\u1ec9 d\u00f9ng khi isHuman
        if (id == 6 && !myChar.isHuman) return false;

        return true;
    }

    // ========================
    // PHASE 2: ATTACK SKILLS
    // ========================

    /**
     * G\u1eedi 1 skill t\u1ea5n c\u00f4ng (type 0, 1, 3).
     * Tr\u1ea3 v\u1ec1 true n\u1ebfu \u0111\u00e3 fire th\u00e0nh c\u00f4ng.
     */
    private static boolean fireAttackSkill(Char myChar, Skill s,
            MyVector reAE, MyVector reAF, Mob focusMob) {
        if (s == null || s.template == null) return false;
        int type = s.template.type;
        if (type != 0 && type != 1 && type != 3) return false;

        // P1: Check cooldown
        if (!isSkillReady(s)) {
            s.paintCanNotUseSkill = true;
            return false;
        }

        // P3: Check elemental intelligence
        if (focusMob != null && !checkElementalConditions(s, focusMob)) {
            return false;
        }

        // P3: Skill type 3 ch\u1ec9 d\u00f9ng khi mob HP > 50% ho\u1eb7c l\u00e0 boss
        if (type == 3 && focusMob != null) {
            if (focusMob.levelBoss == 0 && focusMob.hp <= focusMob.maxHp / 2) {
                // Mob th\u01b0\u1eddng HP th\u1ea5p — kh\u00f4ng c\u1ea7n d\u00f9ng skill m\u1ea1nh
                // Ngo\u1ea1i l\u1ec7: Skill 4 (\u0110\u1ed1t Qu\u00e1i) khi DotQuai + HP th\u1ea5p
                if (s.template.id == 4 && Char.DotQuai
                        && myChar.cHP < (myChar.cMaxHP * Char.aHpValue / 100)) {
                    // cho ph\u00e9p
                } else {
                    return false;
                }
            }
        }

        // L\u1ecdc m\u1ee5c ti\u00eau
        MyVector targetMobs = getTargetMobsForSkill(myChar, s);
        if (targetMobs.size() == 0 && reAE != null && reAE.size() > 0) {
            targetMobs = reAE;
        }
        if (targetMobs.size() == 0) return false;

        // === G\u1eedi attack ===
        s.lastTimeUseThisSkill = System.currentTimeMillis();
        Service.gI().gameAG(s.template.id);
        Service.gI().gameAA(targetMobs, reAF != null ? reAF : new MyVector(), 1);

        // V\u1ebd hi\u1ec7u \u1ee9ng
        if (GameScr.sks != null && s.template.id >= 0 && s.template.id < GameScr.sks.length) {
            myChar.gameAB(GameScr.sks[s.template.id], 0);
        }

        try { Thread.sleep(SKILL_DELAY_MS); } catch (Exception ex) {}
        return true;
    }

    // ========================
    // HELPERS
    // ========================

    /**
     * P1: Ki\u1ec3m tra skill \u0111\u00e3 h\u1ebft cooldown ch\u01b0a.
     * Tr\u1eeb 300ms buffer gi\u1ed1ng d\u1ee9a mod \u0111\u1ec3 d\u00f9ng s\u1edbm h\u01a1n 1 ch\u00FAt.
     */
    private static boolean isSkillReady(Skill s) {
        long elapsed = System.currentTimeMillis() - s.lastTimeUseThisSkill;
        return elapsed >= ((long) s.coolDown - CD_BUFFER_MS);
    }

    /**
     * P3: Ki\u1ec3m tra \u0111i\u1ec1u ki\u1ec7n nguy\u00ean t\u1ed1 tr\u01b0\u1edbc khi d\u00f9ng skill t\u1ea5n c\u00f4ng.
     * - Skill 7, 16 (Fire): skip n\u1ebfu mob \u0111ang ch\u00e1y
     * - Skill 25, 34 (Ice): ch\u1ec9 d\u00f9ng n\u1ebfu mob \u0111\u00f4ng \u0111\u00e1
     * - Skill 43 (Wind): ch\u1ec9 d\u00f9ng n\u1ebfu mob c\u00f3 gi\u00f3
     * Ch\u1ec9 \u00e1p d\u1ee5ng cho boss v\u00e0 mob HP > 50%.
     */
    private static boolean checkElementalConditions(Skill s, Mob mob) {
        int id = s.template.id;

        // Ch\u1ec9 check nguy\u00ean t\u1ed1 khi boss ho\u1eb7c mob HP > 50%
        if (mob.levelBoss == 0 && mob.hp < mob.maxHp / 2) return true;

        // Fire skills: skip n\u1ebfu mob \u0111\u00e3 ch\u00e1y
        if ((id == 7 || id == 16) && mob.isFire) return false;

        // Ice skills: ch\u1ec9 d\u00f9ng khi mob \u0111\u00f4ng
        if ((id == 25 || id == 34) && !mob.isIce) return false;

        // Wind skill: ch\u1ec9 d\u00f9ng khi mob c\u00f3 gi\u00f3
        if (id == 43 && !mob.isWind) return false;

        return true;
    }

    /**
     * Qu\u00e9t danh s\u00e1ch mob s\u1ed1ng trong t\u1ea7m skill, t\u1ed1i \u0111a maxFight.
     * D\u00f9ng dx+30, dy+30 gi\u1ed1ng d\u1ee9a mod (kh\u00f4ng ph\u1ea3i +40 nh\u01b0 v1).
     */
    private static MyVector getTargetMobsForSkill(Char myChar, Skill s) {
        MyVector vTargets = new MyVector();
        if (GameScr.vMob == null) return vTargets;

        int maxTargets = s.maxFight > 0 ? s.maxFight : 1;
        int rangeX = s.dx > 0 ? s.dx + 30 : 120;
        int rangeY = s.dy > 0 ? s.dy + 30 : 100;

        int size = GameScr.vMob.size();
        for (int i = 0; i < size; i++) {
            try {
                Mob mob = (Mob) GameScr.vMob.elementAt(i);
                if (mob != null && mob.status != 0 && mob.status != 1 && mob.hp > 0) {
                    int diffX = Math.abs(myChar.cx - mob.x);
                    int diffY = Math.abs(myChar.cy - mob.y);
                    if (diffX <= rangeX && diffY <= rangeY) {
                        vTargets.addElement(mob);
                        if (vTargets.size() >= maxTargets) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {}
        }
        return vTargets;
    }
}
