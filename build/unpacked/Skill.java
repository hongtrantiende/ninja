/*
 * Decompiled with CFR 0.152.
 */
public final class Skill {
    public SkillTemplate template;
    public short skillId;
    public int point;
    public int level;
    public int coolDown;
    public long lastTimeUseThisSkill;
    public int dx;
    public int dy;
    public int maxFight;
    public int manaUse;
    public SkillOption[] options;
    public boolean paintCanNotUseSkill = false;

    public final void gameAA(int var1, int var2, mGraphics var3) {
        SmallImage.gameAB(var3, this.template.iconId, var1, var2, 0, StaticObj.VCENTER_HCENTER);
        long var4 = System.currentTimeMillis() - this.lastTimeUseThisSkill;
        long cd = this.getCoolDown();
        if (cd > 0L && var4 < cd) {
            var3.gameAA(0x333333);
            if (this.paintCanNotUseSkill && GameCanvas.gameTick % 6 > 2) {
                var3.gameAA(0x444444);
            }
            int var6 = (int)(var4 * 18L / cd);
            var3.gameAD(var1 - 9, var2 - 9 + var6, 18, 18 - var6);
        } else {
            this.paintCanNotUseSkill = false;
        }
    }

    public final long getCoolDown() {
        return Code.skillDelay >= 0 ? (long)Code.skillDelay : (long)this.coolDown;
    }

    public final boolean gameAA() {
        return System.currentTimeMillis() - this.lastTimeUseThisSkill < this.getCoolDown();
    }

    public final int gameAB() {
        return Code.gameBI ? Code.gameBJ : this.dx;
    }

    public final int gameAC() {
        return Code.gameBK ? Code.gameBL : this.dy;
    }

    public final int gameAD() {
        return Code.gameBM ? Code.gameBN : this.maxFight;
    }
}
