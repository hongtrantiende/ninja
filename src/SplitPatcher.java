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
            injectNamMod(var1);
            for (int i = 0; i < var1.size(); i++) {
                Command cmd = (Command) var1.elementAt(i);
                if (cmd == null) continue;
                // Hook nut "Nhat Xa / Hut VP" goc (1100080) -> AutoPickup.toggle()
                if (cmd.idAction == 1100080) {
                    String label = AutoPickup.isRunning ? "H\u00fat VP: ON" : "H\u00fat VP: OFF";
                    Command hutVp = new Command(label, new IActionListener() {
                        public void perform(int id, Object p) {
                            AutoPickup.toggle();
                        }
                    }, 1100080, null);
                    var1.setElementAt(hutVp, i);
                }
                // Nut "Tach" goc trong GameScr co idAction = 110244
                if (cmd.idAction == 110244) {
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
                    // Chen nut Tach le ngay sau nut Tach goc
                    var1.insertElementAt(tachLe, i + 1);
                }
            }
        }
        // Goi lai phuong thuc goc de hien thi menu
        menu.gameAA(var1);
    }

    private static void injectNamMod(MyVector items) {
        boolean isMainMenu = false;
        boolean alreadyAdded = false;
        int insertAt = items.size();
        for (int i = 0; i < items.size(); i++) {
            Command cmd = (Command)items.elementAt(i);
            if (cmd == null) continue;
            if (cmd.idAction == 120001) alreadyAdded = true;
            if (cmd.idAction == 11000805) {
                isMainMenu = true;
                insertAt = i + 1;
            }
        }
        if (isMainMenu && !alreadyAdded) {
            Command namMod = new Command("NinjaNamod", new IActionListener() {
                public void perform(int id, Object parameter) {
                    NamMod.open();
                }
            }, 120001, null);
            items.insertElementAt(namMod, insertAt);
        }
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
