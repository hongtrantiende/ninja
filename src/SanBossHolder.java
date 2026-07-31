/**
 * SanBossHolder - Dummy Auto giu Code.gameAB != null
 * de menu hien "Tat Auto" khi AutoSanBoss dang chay.
 * Override cac method xu ly de khong lam gi.
 */
public final class SanBossHolder extends Auto {
    public void gameAC() {
        // Khong pop auto stack - giu Code.gameAB != null
    }

    public void gameAD() {
        // Khong xu ly auto-fight
    }

    public void gameAK() {
        // Abstract method required by Auto - khong lam gi
    }

    public String toString() {
        return "Auto San Boss";
    }
}
