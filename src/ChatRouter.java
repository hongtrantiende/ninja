/**
 * ChatRouter - Xu ly cac lenh chat mo rong (tspkbsv, tspkbtg, tspkbvm, tspkbmn)
 * 
 * Thay the Code.gameAF(String) trong GameScr:
 * GameScr goi ChatRouter.checkAll(text) thay vi Code.gameAF(text)
 * checkAll goi Code.gameAF truoc, neu ko match thi check lenh mo rong.
 */
public class ChatRouter {
    
    /**
     * Thay the Code.gameAF(String) - goi gameAF goc + check lenh mo rong.
     * CUNG SIGNATURE: (Ljava/lang/String;)Z
     */
    public static boolean checkAll(String text) {
        if (text == null) return false;
        
        // Check lenh mo rong TRUOC
        if (text.equals("tspkbsv")) {
            AutoSanBoss.toggleSV();
            return true;
        }
        if (text.equals("tspkbtg")) {
            AutoSanBoss.toggleTG();
            return true;
        }
        if (text.equals("tspkbvm")) {
            AutoSanBoss.toggleVM();
            return true;
        }
        if (text.equals("tspkbmn")) {
            AutoSanBoss.toggleMN();
            return true;
        }
        
        // Fallback: goi Code.gameAF goc
        return Code.gameAF(text);
    }
}
