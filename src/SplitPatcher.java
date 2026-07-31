public class SplitPatcher {
    public static short savedSubId = -1;
    public static int savedTypeUI = 4;
    public static int savedIndexUI = -1;

    // Hook 1: Catch Packet 92 (short subId, String quantity)
    public static boolean checkSplit(short subId, String str) {
        return false; // Vô hiệu hóa hook cũ
    }

    // Hook 2: Catch Packet 13 (int typeUI, int indexUI, int count)
    public static boolean checkSplitInt(int typeUI, int indexUI, int count) {
        return false; // Vô hiệu hóa hook cũ
    }

    // Hook 3: Catch InputDlg.gameAA (String title, Command cmd)
    public static boolean checkInputDlg(String title, Command cmd) {
        return false; // Vô hiệu hóa hook cũ
    }

    public static void hookMenu(Menu menu, MyVector var1) {
        if (var1 != null) {
            for (int i = 0; i < var1.size(); i++) {
                Command cmd = (Command) var1.elementAt(i);
                // Nút "Tách" gốc trong GameScr có idAction = 110244
                if (cmd != null && cmd.idAction == 110244) {
                    Command tachLe = new Command("T\u00e1ch l\u1ebb", new IActionListener() {
                        public void perform(int id, Object p) {
                            GameCanvas.inputDlg.gameAA("Nh\u1eadp s\u1ed1 l\u01b0\u1ee3ng t\u00e1ch l\u1ebb", new Command("T\u00e1ch", new IActionListener() {
                                public void perform(int id2, Object p2) {
                                    try {
                                        int count = Integer.parseInt(GameCanvas.inputDlg.tfInput.gameAD().trim());
                                        doTachLeDirect(count);
                                    } catch (Exception e) {}
                                    GameCanvas.endDlg();
                                }
                            }, 99999, null), 1);
                            GameCanvas.inputDlg.tfInput.gameAA("3");
                        }
                    }, 99999, null);
                    // Chèn nút Tách lẻ ngay sau nút Tách gốc
                    var1.insertElementAt(tachLe, i + 1);
                    break;
                }
            }
        }
        // Gọi lại phương thức gốc để hiển thị menu
        menu.gameAA(var1);
    }

    public static void doTachLeDirect(final int count) {
        if (count <= 0) return;
        final int index = GameScr.gameBM; // Lấy vị trí món đồ đang chọn trong Hành trang
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    GameScr.gameAC("TSB: \u0110ang t\u00e1ch l\u1ebb " + count + " m\u00f3n...");
                    for (int i = 0; i < count; i++) {
                        // Gọi packet tách đồ trong Hành trang (-85)
                        Service.gI().gameAK(index, 1);
                        Thread.sleep(150);
                    }
                    GameScr.gameAC("TSB: \u0110\u00e3 t\u00e1ch xong " + count + " m\u00f3n l\u1ebb 1!");
                } catch (Exception e) {
                    GameScr.gameAC("TSB: L\u1ed7i khi t\u00e1ch \u0111\u1ed3!");
                }
            }
        }).start();
    }
}
