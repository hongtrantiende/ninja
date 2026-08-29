/**
 * ModInit — Khoi tao som toan bo he thong Auto / Săn Boss ngay khi vao game.
 * 
 * Khac phuc triet de van de: Khi thoat han game ra vao lai, TS Boss Uu Tien
 * da bat trong RMS nhung khong tu chay do class chua duoc load cho den khi mo menu NamMod.
 */
public class ModInit {
    private static boolean initialized = false;

    public static synchronized void initAll() {
        if (initialized) return;
        initialized = true;

        try {
            AutoBossEvent.loadConfigFromRMS();
        } catch (Exception e) {}

        try {
            AutoSuicide.loadConfigFromRMS();
        } catch (Exception e) {}

        try {
            TsBoost.loadConfigFromRMS();
        } catch (Exception e) {}

        try {
            AutoBossNotice.loadConfigFromRMS();
        } catch (Exception e) {}

        try {
            BossLog.loadFromRMS();
        } catch (Exception e) {}

        try {
            ExploitConfig.loadConfigFromRMS();
        } catch (Exception e) {}

        try {
            AutoVipMap.loadConfigFromRMS();
        } catch (Exception e) {}

        try {
            AutoTuLuyen.loadConfigFromRMS();
        } catch (Exception e) {}
    }
}
