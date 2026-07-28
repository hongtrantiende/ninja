public final class Skill {
    public SkillTemplate template;
    public short skillId;
    public int point;
    public int level;
    public int coolDown = 10;
    public long lastTimeUseThisSkill;
    public int dx;
    public int dy;
    public int maxFight;
    public int manaUse;
    public SkillOption[] options;
    public boolean paintCanNotUseSkill;

    public Skill() {
        this.paintCanNotUseSkill = false;
        this.coolDown = 10;
    }

    public final void gameAA(int x, int y, mGraphics g) {
        SmallImage.gameAB(g, this.template.iconId, x, y, 0, StaticObj.VCENTER_HCENTER);
        long elapsed = System.currentTimeMillis() - this.lastTimeUseThisSkill;
        if (elapsed < (long)this.coolDown) {
            g.gameAA(3355443);
            if (this.paintCanNotUseSkill && GameCanvas.gameTick % 6 > 2) {
                g.gameAA(4473924);
            }
            int h = (int)(elapsed * 18L / (long)this.coolDown);
            g.gameAD(x - 9, y - 9 + h, 18, 18 - h);
        } else {
            this.paintCanNotUseSkill = false;
        }
    }

    public final boolean gameAA() {
        return System.currentTimeMillis() - this.lastTimeUseThisSkill < (long)this.coolDown;
    }

    public final int gameAB() {
        if (Code.gameBI) {
            return Code.gameBJ;
        }
        return this.dx;
    }

    public final int gameAC() {
        if (Code.gameBK) {
            return Code.gameBL;
        }
        return this.dy;
    }

    public final int gameAD() {
        if (Code.gameBM) {
            return Code.gameBN;
        }
        return this.maxFight;
    }
}
