/**
 * AutoVT55 — Tu dong them 11 vi tri AVT cho Map 55 (Phong an Ounio).
 * Lenh: avt55
 *
 * Chi lam 1 viec: xoa AVT cu → them 11 vi tri vao Code.gameAT/gameAU
 * Sau do user go "dcvt" de bat di chuyen vi tri, va "ts" de danh.
 */
public class AutoVT55 {

    // 11 vi tri danh tren Map 55, X=241, Y giam dan
    private static final int[][] POSITIONS = {
        {241, 1344},  // VT 1
        {241, 1224},  // VT 2
        {241, 1104},  // VT 3
        {241, 984},   // VT 4
        {241, 864},   // VT 5
        {241, 744},   // VT 6
        {241, 624},   // VT 7
        {241, 504},   // VT 8
        {241, 384},   // VT 9
        {241, 264},   // VT 10
        {241, 144},   // VT 11
    };

    /**
     * Them 11 vi tri AVT vao he thong game goc.
     * Xoa danh sach cu truoc khi them.
     */
    public static void setup() {
        try {
            // Xoa danh sach AVT cu
            Code.gameAT.removeAllElements();
            Code.gameAU.removeAllElements();

            // Them 11 vi tri moi
            for (int i = 0; i < POSITIONS.length; i++) {
                int x = POSITIONS[i][0];
                int y = POSITIONS[i][1];
                Code.gameAT.addElement(new Integer(x));
                Code.gameAU.addElement(new Integer(y));
            }

            GameScr.gameAC("\u0110\u00e3 th\u00eam " + POSITIONS.length + " VT Map 55! G\u00f5 dcvt + ts");
        } catch (Exception e) {
            GameScr.gameAC("L\u1ed7i setup AVT: " + e.getMessage());
        }
    }
}
