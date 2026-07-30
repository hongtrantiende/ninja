public class SplitPatcher {
    public static boolean checkSplit(short id, String str) {
        try {
            if (str != null) {
                int count = Integer.parseInt(str.trim());
                if (count > 1) {
                    AutoSanBoss.tachDoLe(count);
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }
}
