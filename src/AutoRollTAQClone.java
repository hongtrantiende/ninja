import java.util.Vector;

/**
 * AutoRollTAQClone — Bên ẶC ROLL (Emulator 2).
 *
 * Chức năng:
 * - Hỗ trợ chạy liên hoàn danh sách tài khoản:
 *   + "rollclone onjnvip1 cacao1" -> Chạy từ cacao1 đến cacao20, xong tự đăng xuất qua acc kế tiếp.
 *   + "rollclone onjnvip1 all" -> Chạy toàn bộ 9 nhóm tài khoản (180 acc).
 *   + "rollclone onjnvip1 all cacao6" -> Tiếp tục chạy từ cacao6 đến hết cacao20 và chạy tiếp tất cả các nhóm còn lại.
 *   + "rollclone onjnvip1" -> Chạy 1 lần trên ặc hiện tại không đăng xuất.
 * - Quy trình mỗi tài khoản:
 *   1. Đăng xuất & Đăng nhập vào game (tự tạo nhân vật sạch nếu chưa có NV).
 *   2. Đến NPC 24 -> Ô 4 "Nhập code" -> điền "aeharuna" -> nhận quà.
 *   3. Đến ặc đứng -> GD nhận thẻ Roll TAQ (ID 905).
 *   4. Bật Auto TAQ (NPC 51) 5 giây -> Tắt Auto TAQ.
 *   5. Đến ặc đứng -> GD trả SẠCH TẤT CẢ vật phẩm trong hành trang (kèm tự động sắp xếp sau mỗi lần GD).
 *   6. Hiện log hoàn thành và chuyển sang tài khoản kế tiếp.
 */
public class AutoRollTAQClone implements Runnable {
    public static volatile boolean isAuto = false;

    /** Tên ặc đứng để giao dịch */
    public static String tenAcDung = null;

    /** Danh sách tài khoản cần chạy {username, password} */
    public static Vector accountList = new Vector();

    /** ID thẻ Roll TAQ */
    private static final int THE_ROLL_TAQ_ID = 905;

    public void run() {
        try {
            if (tenAcDung == null || tenAcDung.length() == 0) {
                GameScr.gameAC("[Clone] L\u1ed7i: Ch\u01b0a c\u00f3 t\u00ean \u1eb7c \u0111\u1ee9ng!");
                GameScr.gameAC("[Clone] C\u00fa ph\u00e1p: rollclone TenAcDung [all / TaiKhoan] [TaiKhoanBatDau]");
                isAuto = false;
                return;
            }

            // TRƯỜNG HỢP 1: Có danh sách tài khoản cần chạy liên hoàn
            if (accountList != null && !accountList.isEmpty()) {
                int total = accountList.size();
                GameScr.gameAC("[Clone] === B\u1eaft \u0111\u1ea7u Roll Li\u00ean Ho\u00e0n: " + total + " t\u00e0i kho\u1ea3n ===");
                GameScr.gameAC("[Clone] \u1eb6c \u0111\u1ee9ng nh\u1eadn \u0111\u1ed3: " + tenAcDung);

                for (int a = 0; a < total && isAuto; a++) {
                    String[] acc = (String[]) accountList.elementAt(a);
                    String user = acc[0];
                    String pass = acc[1];

                    String nextAcc = (a + 1 < total) ? ((String[]) accountList.elementAt(a + 1))[0] : "H\u1ebfT";
                    GameScr.gameAC("[Clone] [" + (a + 1) + "/" + total + "] >>> \u0110ang ch\u1ea1y TK: " + user + " <<<");

                    // 1. Đăng nhập và tạo/chọn NV
                    boolean loginOk = AutoLogin.doLogin(user, pass);
                    if (!loginOk) {
                        GameScr.gameAC("[Clone] \u0110\u0103ng nh\u1eadp " + user + " th\u1ea5t b\u1ea1i/timeout! Chuy\u1ec3n sang: " + nextAcc);
                        Auto.Sleep(1000L);
                        continue;
                    }

                    Auto.Sleep(300L);
                    if (!isAuto) break;

                    // 2. Chạy quy trình 4 bước Roll TAQ
                    doSingleAccountRoll();

                    GameScr.gameAC("[Clone] === HO\u00c0N T\u1ea4T TK: " + user + " (" + (a + 1) + "/" + total + ")! Ti\u1ebfp theo: " + nextAcc + " ===");
                    Auto.Sleep(300L);
                }

                accountList.removeAllElements();
                GameScr.gameAC("[Clone] === HO\u00c0N T\u1ea4T TO\u00c0N B\u1ed8 DANH S\u00c1CH T\u00c0I KHO\u1ea2N! D\u1eebng Auto. ===");
            } else {
                // TRƯỜNG HỢP 2: Chạy 1 lần trên tài khoản hiện tại
                Char myChar = Char.getMyChar();
                if (myChar == null || myChar.cName == null) {
                    GameScr.gameAC("[Clone] L\u1ed7i: B\u1ea1n ch\u01b0a \u0111\u0103ng nh\u1eadp v\u00e0o game!");
                    isAuto = false;
                    return;
                }

                GameScr.gameAC("[Clone] === B\u1eaft \u0111\u1ea7u Auto Roll TAQ (1 Acc) ===");
                GameScr.gameAC("[Clone] \u1eb6c \u0111\u1ee9ng nh\u1eadn \u0111\u1ed3: " + tenAcDung);

                doSingleAccountRoll();

                GameScr.gameAC("[Clone] === HO\u00c0N T\u1ea4T! D\u1eebng Auto. ===");
            }

        } catch (Exception e) {
            GameScr.gameAC("[Clone] L\u1ed7i: " + e.getMessage());
        } finally {
            isAuto = false;
        }
    }

    /**
     * Thực hiện quy trình 4 bước Roll TAQ trên tài khoản đang đăng nhập
     */
    private void doSingleAccountRoll() {
        try {
            // Kiểm tra xem ặc đứng có trên map không
            Char target = findCharByName(tenAcDung);
            if (target == null) {
                GameScr.gameAC("[Clone] \u0110ang t\u00ecm " + tenAcDung + " tr\u00ean map...");
                for (int t = 0; t < 10 && target == null && isAuto; t++) {
                    Auto.Sleep(300L);
                    target = findCharByName(tenAcDung);
                }
                if (target == null) {
                    GameScr.gameAC("[Clone] Kh\u00f4ng th\u1ea5y " + tenAcDung + " tr\u00ean map!");
                    return;
                }
            }

            // BƯỚC 1: NPC 24 -> Ô 4 "Nhập code" -> code "aeharuna"
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 1: M\u1edf NPC 24 nh\u1eadp code aeharuna...");
            doNpcCode();
            Auto.Sleep(300L);
            if (!isAuto) return;

            // BƯỚC 2: Giao dịch với ặc đứng -> Lấy thẻ Roll TAQ (ID 905)
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 2: GD v\u1edbi " + tenAcDung + " l\u1ea5y th\u1ebb Roll...");
            boolean getCardSuccess = false;
            for (int tryTrade = 1; tryTrade <= 3 && !getCardSuccess && isAuto; tryTrade++) {
                if (doTradeWithTarget(tenAcDung, false)) {
                    getCardSuccess = true;
                } else {
                    GameScr.gameAC("[Clone] GD th\u1eed l\u1ea1i l\u1ea7n " + (tryTrade + 1) + "...");
                    Auto.Sleep(1000L);
                }
            }

            Auto.Sleep(800L);
            if (!isAuto) return;

            // Kiểm tra thẻ roll trong hành trang
            int soThe = countItemInBag(THE_ROLL_TAQ_ID);
            GameScr.gameAC("[Clone] S\u1ed1 th\u1ebb Roll TAQ trong h\u00e0nh trang: " + soThe);

            // BƯỚC 3: Bật Auto TAQ (5 giây) -> Tắt Auto TAQ
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 3: B\u1eaft \u0111\u1ea7u Roll TAQ (5 gi\u00e2y)...");
            AutoTAQ.isAuto = true;
            new Thread(new AutoTAQ()).start();
            GameScr.gameAC("B\u1eadt Auto Tr\u00e1i \u00c1c Qu\u1ef7 (NPC 51)!");
            Auto.Sleep(4000L);
            AutoTAQ.isAuto = false;
            GameScr.gameAC("T\u1eaft Auto Tr\u00e1i \u00c1c Qu\u1ef7!");
            Auto.Sleep(500L);
            GameScr.gameAC("[Clone] Roll TAQ xong!");
            if (!isAuto) return;

            // BƯỚC 4: GD trả SẠCH TẤT CẢ đồ cho ặc đứng
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 4: GD tr\u1ea3 s\u1ea1ch \u0111\u1ed3 cho " + tenAcDung + "...");
            // Sắp xếp hành trang trước khi bắt đầu GD
            try {
                Service.gI().gameAG();
                Service.gI().gameAF();
            } catch (Exception e) {}
            Auto.Sleep(500L);

            int gdCount = 0;
            while (hasItemsInBag() && isAuto && gdCount < 20) {
                gdCount++;
                GameScr.gameAC("[Clone] GD tr\u1ea3 \u0111\u1ed3 l\u1ea7n " + gdCount + "...");
                if (!doTradeWithTarget(tenAcDung, true)) {
                    GameScr.gameAC("[Clone] GD l\u1ea7n " + gdCount + " ch\u01b0a xong, th\u1eed l\u1ea1i...");
                    Auto.Sleep(1000L);
                    continue;
                }

                // Sắp xếp lại hành trang và rương ngay sau khi GD xong để đồng bộ sạch sẽ
                try {
                    Service.gI().gameAG(); // Sắp xếp hành trang
                    Service.gI().gameAF(); // Sắp xếp rương
                } catch (Exception e) {}
                Auto.Sleep(800L);
            }

            if (!hasItemsInBag()) {
                GameScr.gameAC("[Clone] \u0110\u00e3 tr\u1ea3 S\u1ea0CH to\u00e0n b\u1ed9 \u0111\u1ed3!");
            } else {
                GameScr.gameAC("[Clone] C\u00f2n m\u1ed9t s\u1ed1 \u0111\u1ed3 kh\u00f4ng GD \u0111\u01b0\u1ee3c (kh\u00f3a).");
            }
        } catch (Exception e) {
            GameScr.gameAC("[Clone] L\u1ed7i Roll: " + e.getMessage());
        }
    }

    // ===================== HELPER METHODS =====================

    /** Tạo danh sách tài khoản theo tham số lệnh chat (hỗ trợ bắt đầu từ vị trí cụ thể) */
    public static Vector generateAccountList(String param, String startFrom) {
        Vector list = new Vector();
        String p = (param != null) ? param.toLowerCase().trim() : "";
        String start = (startFrom != null) ? startFrom.toLowerCase().trim() : "";

        // 15 nhóm còn lại (suanon đã xong)
        String[] groups = new String[] {
            "chetha", "topmie", "kemtui", "compho", "haisan",
            "botkem", "kemcay", "misopa", "hotdog", "supkem",
            "cocmay", "trache", "daupha", "goikeo", "damsen"
        };

        if (p.equals("all")) {
            if (start.length() > 0) {
                int startGroupIdx = -1;
                int startNum = 1;

                for (int g = 0; g < groups.length; g++) {
                    if (start.startsWith(groups[g])) {
                        startGroupIdx = g;
                        String numStr = start.substring(groups[g].length()).trim();
                        if (numStr.length() > 0) {
                            try { startNum = Integer.parseInt(numStr); } catch (Exception e) { startNum = 1; }
                        }
                        break;
                    }
                }

                if (startGroupIdx != -1) {
                    for (int i = startNum; i <= 20; i++) {
                        list.addElement(new String[] { groups[startGroupIdx] + i, "000000" });
                    }
                    for (int g = startGroupIdx + 1; g < groups.length; g++) {
                        for (int i = 1; i <= 20; i++) {
                            list.addElement(new String[] { groups[g] + i, "000000" });
                        }
                    }
                    return list;
                }
            }

            // all: 15 nhom x 20 = 300 acc
            for (int g = 0; g < groups.length; g++) {
                for (int i = 1; i <= 20; i++) {
                    list.addElement(new String[] { groups[g] + i, "000000" });
                }
            }
            return list;
        }

        // Kiểm tra xem có bắt đầu bằng 1 trong 9 nhóm không (ví dụ: cacao1, cacao6, cacao)
        for (int g = 0; g < groups.length; g++) {
            String grp = groups[g];
            if (p.startsWith(grp)) {
                int startNum = 1;
                String numStr = p.substring(grp.length()).trim();
                if (numStr.length() > 0) {
                    try {
                        startNum = Integer.parseInt(numStr);
                    } catch (Exception e) {
                        startNum = 1;
                    }
                }
                for (int i = startNum; i <= 20; i++) {
                    list.addElement(new String[] { grp + i, "000000" });
                }
                return list;
            }
        }

        // Kiểm tra nếu là nhóm vaicalon 121 -> 320
        if (p.endsWith("vaicalon")) {
            int startNum = 121;
            String numStr = p.substring(0, p.length() - 8).trim();
            if (numStr.length() > 0) {
                try {
                    startNum = Integer.parseInt(numStr);
                } catch (Exception e) {
                    startNum = 121;
                }
            }
            for (int i = startNum; i <= 320; i++) {
                list.addElement(new String[] { i + "vaicalon", "azoazo" });
            }
            return list;
        }

        // Tài khoản đơn lẻ tùy ý
        list.addElement(new String[] { param, "000000" });
        return list;
    }

    /** NPC 24, chọn ô 4 "Nhập code" (index 3), nhập code "aeharuna" */
    private void doNpcCode() {
        try {
            GameCanvas.endDlg();
            InfoDlg.gameAD();
            Auto.Sleep(300L);

            Service.gI().gameAH(24);
            Auto.Sleep(500L);

            Service.gI().gameAC(24, 3, 0);

            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 8000) {
                try {
                    if (GameCanvas.inputDlg != null && GameCanvas.inputDlg.tfInput != null) {
                        break;
                    }
                } catch (Exception e) {}
                Auto.Sleep(300L);
            }
            Auto.Sleep(200L);

            GameCanvas.inputDlg.tfInput.gameAA("aeharuna");
            Auto.Sleep(200L);

            String inputText = GameCanvas.inputDlg.tfInput.gameAD();
            try {
                Short npcId = (Short) GameCanvas.inputDlg.center.p;
                Service.gI().gameAA(npcId.shortValue(), inputText);
            } catch (Exception e) {
                Service.gI().gameAA((short)24, inputText);
            }
            GameCanvas.endDlg();
            Auto.Sleep(600L);

            GameCanvas.endDlg();
            InfoDlg.gameAD();
            Auto.Sleep(300L);
        } catch (Exception e) {
            GameScr.gameAC("[Clone] NPC L\u1ed7i: " + e.getMessage());
        }
    }

    /**
     * Giao dịch với nhân vật theo tên.
     * @param targetName Tên nhân vật ặc đứng
     * @param isSending  true = gửi TẤT CẢ VP trong hành trang, false = nhận (đặt rỗng)
     */
    private boolean doTradeWithTarget(String targetName, boolean isSending) {
        try {
            Char target = findCharByName(targetName);
            if (target == null) {
                for (int t = 0; t < 6 && target == null; t++) {
                    Auto.Sleep(500L);
                    target = findCharByName(targetName);
                }
                if (target == null) {
                    GameScr.gameAC("[Clone] Kh\u00f4ng th\u1ea5y " + targetName + " tr\u00ean map!");
                    return false;
                }
            }

            Char myChar = Char.getMyChar();
            for (int r = 0; r < 12; r++) {
                if (Res.gameAA(myChar.cx, myChar.cy, target.cx, target.cy) < 50) break;
                Char.gameAC(target.cx, target.cy);
                Auto.Sleep(400L);
                if (!isAuto) return false;
                target = findCharByName(targetName);
                if (target == null) return false;
            }

            Service.gI().gameAS(target.charID);

            if (!waitForTradeScreen(15000)) {
                GameScr.gameAC("[Clone] " + targetName + " ch\u01b0a ch\u1ea5p nh\u1eadn GD!");
                return false;
            }

            Item[] tradeItems = new Item[12];
            if (isSending) {
                int count = 0;
                if (myChar.arrItemBag != null) {
                    for (int j = 0; j < myChar.arrItemBag.length && count < 12; j++) {
                        Item item = myChar.arrItemBag[j];
                        if (item != null && item.template != null && !item.isLock) {
                            tradeItems[count++] = item;
                        }
                    }
                }
                if (count == 0) {
                    Service.gI().gameAI(); // Không có gì để gửi
                    return true;
                }
            }

            GameScr.gI().gameEE = 0;
            GameScr.gameCW = tradeItems;
            Service.gI().gameAA(0, tradeItems);
            GameScr.gI().gameEC = 1; // Khóa giao dịch

            long start = System.currentTimeMillis();
            while (GameScr.gI().typeTradeOrder != 1 && GameScr.isPaintTrade) {
                if (!isAuto || System.currentTimeMillis() - start >= 15000) {
                    Service.gI().gameAI();
                    return false;
                }
                Auto.Sleep(200L);
            }

            Auto.Sleep(500L);
            Service.gI().gameAJ();

            start = System.currentTimeMillis();
            while (GameScr.isPaintTrade && GameScr.gI().typeTradeOrder != 2) {
                if (!isAuto || System.currentTimeMillis() - start >= 4000) {
                    break;
                }
                Auto.Sleep(200L);
            }

            LockGame.LockAA(800L);

            if (isSending) {
                for (int k = 0; k < 12; k++) {
                    if (tradeItems[k] != null) {
                        try {
                            Char.getMyChar().arrItemBag[tradeItems[k].indexUI] = null;
                        } catch (Exception e) {}
                    }
                }
            }

            GameScr.gameAC("[Clone] Giao d\u1ecbch th\u00e0nh c\u00f4ng!");
            return true;
        } catch (Exception e) {
            GameScr.gameAC("[Clone] GD l\u1ed7i: " + e.getMessage());
            return false;
        }
    }

    /** Chờ màn hình giao dịch mở — TỰ ĐỘNG BẤM "CÓ" NẾU CÓ DIALOG MỜI GD */
    private boolean waitForTradeScreen(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!GameScr.isPaintTrade) {
            if (!isAuto || System.currentTimeMillis() - start >= timeoutMs) return false;

            try {
                if (GameCanvas.currentDialog != null) {
                    if (GameCanvas.currentDialog.left != null && GameCanvas.currentDialog.left.idAction == 88810) {
                        Integer charId = (Integer) GameCanvas.currentDialog.left.p;
                        GameCanvas.endDlg();
                        Service.gI().gameAL(charId.intValue());
                    } else if (GameCanvas.currentDialog.center != null && GameCanvas.currentDialog.center.idAction == 88810) {
                        Integer charId = (Integer) GameCanvas.currentDialog.center.p;
                        GameCanvas.endDlg();
                        Service.gI().gameAL(charId.intValue());
                    }
                }
            } catch (Exception e) {}

            Auto.Sleep(200L);
        }
        return true;
    }

    /** Tìm nhân vật theo tên trên map */
    private Char findCharByName(String name) {
        try {
            for (int i = 0; i < GameScr.vCharInMap.size(); i++) {
                Char c = (Char) GameScr.vCharInMap.elementAt(i);
                if (c != null && c.cName != null && c.cName.equals(name)) return c;
            }
        } catch (Exception e) {}
        return null;
    }

    /** Kiểm tra hành trang còn items không khóa không */
    private boolean hasItemsInBag() {
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null || myChar.arrItemBag == null) return false;
            for (int i = 0; i < myChar.arrItemBag.length; i++) {
                Item item = myChar.arrItemBag[i];
                if (item != null && item.template != null && !item.isLock) return true;
            }
        } catch (Exception e) {}
        return false;
    }

    /** Đếm số item có ID cụ thể trong hành trang */
    private int countItemInBag(int itemId) {
        int count = 0;
        try {
            Char myChar = Char.getMyChar();
            if (myChar == null || myChar.arrItemBag == null) return 0;
            for (int i = 0; i < myChar.arrItemBag.length; i++) {
                Item item = myChar.arrItemBag[i];
                if (item != null && item.template != null && item.template.id == itemId) {
                    count += (item.quantity > 0 ? item.quantity : 1);
                }
            }
        } catch (Exception e) {}
        return count;
    }
}
