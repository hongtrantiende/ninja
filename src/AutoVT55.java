/**
 * AutoVTSetup — Tu dong them vi tri AVT cho cac map.
 * Lenh: avt55 (Map 55), avt58 (Map 58)
 *
 * Chi lam 1 viec: xoa AVT cu -> them vi tri vao Code.gameAT/gameAU
 * Sau do user go "dcvt" de bat di chuyen vi tri, va "ts" de danh.
 */
public class AutoVT55 {

    // === MAP 55: 11 vi tri (X=241, Y giam dan) ===
    private static final int[][] POS_MAP55 = {
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

    // === MAP 58: 12 vi tri ===
    private static final int[][] POS_MAP58 = {
        {182, 264},   // VT 1
        {336, 264},   // VT 2
        {539, 264},   // VT 3
        {196, 96},    // VT 4
        {357, 96},    // VT 5
        {771, 312},   // VT 6
        {957, 312},   // VT 7
        {1203, 312},  // VT 8
        {1354, 288},  // VT 9
        {1069, 216},  // VT 10
        {838, 216},   // VT 11
        {838, 144},   // VT 12
    };

    /** Them 11 VT Map 55. */
    public static void setup() {
        loadPositions(POS_MAP55, 55);
    }

    /** Them 12 VT Map 58. */
    public static void setup58() {
        loadPositions(POS_MAP58, 58);
    }

    /**
     * Xoa AVT cu, them vi tri moi vao Code.gameAT/gameAU.
     */
    private static void loadPositions(int[][] positions, int mapID) {
        try {
            Code.gameAT.removeAllElements();
            Code.gameAU.removeAllElements();

            for (int i = 0; i < positions.length; i++) {
                Code.gameAT.addElement(new Integer(positions[i][0]));
                Code.gameAU.addElement(new Integer(positions[i][1]));
            }

            GameScr.gameAC("\u0110\u00e3 th\u00eam " + positions.length + " VT Map " + mapID + "! G\u00f5 dcvt + ts");
        } catch (Exception e) {
            GameScr.gameAC("L\u1ed7i AVT: " + e.getMessage());
        }
    }
}
