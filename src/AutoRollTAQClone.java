/**
 * AutoRollTAQClone — Bên ẶC ROLL (Emulator 2).
 *
 * Chức năng:
 * - Bạn tự đăng nhập tài khoản clone vào game.
 * - Gõ chat: "rollclone [tên ặc đứng]" (hoặc chỉ cần click ặc đứng rồi gõ "rollclone").
 * - Bot sẽ tự động:
 *   1. Đến NPC 24 -> Ô 4 "Nhập code" -> điền "aeharuna" -> bấm Đồng ý.
 *   2. Đến ặc đứng -> GD nhận thẻ Roll TAQ (ID 905).
 *   3. Bật Auto TAQ (NPC 51) trong 5 giây -> Tắt Auto TAQ.
 *   4. Đến ặc đứng -> GD trả SẠCH TẤT CẢ vật phẩm trong hành trang (lặp lại đến khi hết sạch đồ).
 *   5. Thông báo hoàn thành và TỰ ĐỘNG DỪNG AUTO (KHÔNG đăng xuất).
 */
public class AutoRollTAQClone implements Runnable {
    public static volatile boolean isAuto = false;

    /** Tên ặc đứng để giao dịch */
    public static String tenAcDung = null;

    /** ID thẻ Roll TAQ */
    private static final int THE_ROLL_TAQ_ID = 905;

    public void run() {
        try {
            if (tenAcDung == null || tenAcDung.length() == 0) {
                GameScr.gameAC("[Clone] L\u1ed7i: Ch\u01b0a c\u00f3 t\u00ean \u1eb7c \u0111\u1ee9ng!");
                GameScr.gameAC("[Clone] C\u00fa ph\u00e1p: rollclone TenAcDung");
                isAuto = false;
                return;
            }

            Char myChar = Char.getMyChar();
            if (myChar == null || myChar.cName == null) {
                GameScr.gameAC("[Clone] L\u1ed7i: B\u1ea1n ch\u01b0a \u0111\u0103ng nh\u1eadp v\u00e0o game!");
                isAuto = false;
                return;
            }

            GameScr.gameAC("[Clone] === B\u1eaft \u0111\u1ea7u Auto Roll TAQ ===");
            GameScr.gameAC("[Clone] \u1eb6c \u0111\u1ee9ng nh\u1eadn \u0111\u1ed3: " + tenAcDung);

            // Kiểm tra xem ặc đứng có trên map không
            Char target = findCharByName(tenAcDung);
            if (target == null) {
                GameScr.gameAC("[Clone] \u0110ang t\u00ecm " + tenAcDung + " tr\u00ean map...");
                for (int t = 0; t < 10 && target == null && isAuto; t++) {
                    Auto.Sleep(500L);
                    target = findCharByName(tenAcDung);
                }
                if (target == null) {
                    GameScr.gameAC("[Clone] Kh\u00f4ng th\u1ea5y " + tenAcDung + " tr\u00ean map! D\u1eebng auto.");
                    isAuto = false;
                    return;
                }
            }

            // ========================================================
            // BƯỚC 1: NPC 24 → Ô 4 "Nhập code" → code "aeharuna"
            // ========================================================
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 1: M\u1edf NPC 24 nh\u1eadp code aeharuna...");
            doNpcCode();
            Auto.Sleep(2000L);
            if (!isAuto) return;

            // ========================================================
            // BƯỚC 2: Giao dịch với ặc đứng → Lấy thẻ Roll TAQ (ID 905)
            // ========================================================
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 2: GD v\u1edbi " + tenAcDung + " l\u1ea5y th\u1ebb Roll...");
            boolean getCardSuccess = false;
            for (int tryTrade = 1; tryTrade <= 3 && !getCardSuccess && isAuto; tryTrade++) {
                if (doTradeWithTarget(tenAcDung, false)) {
                    getCardSuccess = true;
                } else {
                    GameScr.gameAC("[Clone] GD th\u1eed l\u1ea1i l\u1ea7n " + (tryTrade + 1) + "...");
                    Auto.Sleep(2000L);
                }
            }

            Auto.Sleep(2000L);
            if (!isAuto) return;

            // Kiểm tra thẻ roll trong hành trang
            int soThe = countItemInBag(THE_ROLL_TAQ_ID);
            GameScr.gameAC("[Clone] S\u1ed1 th\u1ebb Roll TAQ trong h\u00e0nh trang: " + soThe);

            // ========================================================
            // BƯỚC 3: Bật Auto TAQ (5 giây) → Tắt Auto TAQ
            // ========================================================
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 3: B\u1eaft \u0111\u1ea7u Roll TAQ (5 gi\u00e2y)...");
            AutoTAQ.isAuto = true;
            new Thread(new AutoTAQ()).start();
            GameScr.gameAC("B\u1eadt Auto Tr\u00e1i \u00c1c Qu\u1ef7 (NPC 51)!");
            Auto.Sleep(5000L);
            AutoTAQ.isAuto = false;
            GameScr.gameAC("T\u1eaft Auto Tr\u00e1i \u00c1c Qu\u1ef7!");
            Auto.Sleep(2000L);
            GameScr.gameAC("[Clone] Roll TAQ xong!");
            if (!isAuto) return;

            // ========================================================
            // BƯỚC 4: GD trả SẠCH TẤT CẢ đồ cho ặc đứng
            // ========================================================
            GameScr.gameAC("[Clone] B\u01b0\u1edbc 4: GD tr\u1ea3 s\u1ea1ch \u0111\u1ed3 cho " + tenAcDung + "...");
            int gdCount = 0;
            while (hasItemsInBag() && isAuto && gdCount < 20) {
                gdCount++;
                GameScr.gameAC("[Clone] GD tr\u1ea3 \u0111\u1ed3 l\u1ea7n " + gdCount + "...");
                if (!doTradeWithTarget(tenAcDung, true)) {
                    GameScr.gameAC("[Clone] GD l\u1ea7n " + gdCount + " ch\u01b0a xong, th\u1eed l\u1ea1i...");
                    Auto.Sleep(2500L);
                    continue;
                }
                Auto.Sleep(2000L);
            }

            if (!hasItemsInBag()) {
                GameScr.gameAC("[Clone] \u0110\u00e3 tr\u1ea3 S\u1ea0CH to\u00e0n b\u1ed9 \u0111\u1ed3!");
            } else {
                GameScr.gameAC("[Clone] C\u00f2n m\u1ed9t s\u1ed1 \u0111\u1ed3 kh\u00f4ng GD \u0111\u01b0\u1ee3c (kh\u00f3a).");
            }

            // ========================================================
            // BƯỚC 5: HOÀN TẤT & DỪNG AUTO
            // ========================================================
            GameScr.gameAC("[Clone] === HO\u00c0N T\u1ea4T! D\u1eebng Auto. ===");
        } catch (Exception e) {
            GameScr.gameAC("[Clone] L\u1ed7i: " + e.getMessage());
        }
        isAuto = false;
    }

    // ===================== HELPER METHODS =====================

    /** NPC 24, chọn ô 4 "Nhập code" (index 3), nhập code "aeharuna" */
    private void doNpcCode() {
        try {
            // Đóng dialog cũ nếu có
            GameCanvas.endDlg();
            InfoDlg.gameAB();
            Auto.Sleep(1000L);

            // Mở NPC 24
            Service.gI().gameAH(24);
            Auto.Sleep(1500L);

            // Chọn ô 4 "Nhập code" (index 3, 0-based)
            Service.gI().gameAC(24, 3, 0);

            // Chờ InputDlg hiện lên từ server
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 8000) {
                try {
                    if (GameCanvas.inputDlg != null && GameCanvas.inputDlg.tfInput != null) {
                        break;
                    }
                } catch (Exception e) {}
                Auto.Sleep(300L);
            }
            Auto.Sleep(500L);

            // Điền code "aeharuna" vào ô nhập
            GameCanvas.inputDlg.tfInput.gameAA("aeharuna");
            Auto.Sleep(500L);

            // Bấm nút "Đồng ý" (center button)
            String inputText = GameCanvas.inputDlg.tfInput.gameAD();
            try {
                Short npcId = (Short) GameCanvas.inputDlg.center.p;
                Service.gI().gameAA(npcId.shortValue(), inputText);
            } catch (Exception e) {
                Service.gI().gameAA((short)24, inputText);
            }
            GameCanvas.endDlg();
            Auto.Sleep(1500L);

            // Đóng dialog kết quả
            GameCanvas.endDlg();
            InfoDlg.gameAB();
            Auto.Sleep(500L);
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

            // Di chuyển lại gần đối phương
            Char myChar = Char.getMyChar();
            for (int r = 0; r < 12; r++) {
                if (Res.gameAA(myChar.cx, myChar.cy, target.cx, target.cy) < 50) break;
                Char.gameAC(target.cx, target.cy);
                Auto.Sleep(800L);
                if (!isAuto) return false;
                target = findCharByName(targetName);
                if (target == null) return false;
            }

            // Gửi lời mời giao dịch
            Service.gI().gameAS(target.charID);

            // Chờ màn hình giao dịch mở
            if (!waitForTradeScreen(15000)) {
                GameScr.gameAC("[Clone] " + targetName + " ch\u01b0a ch\u1ea5p nh\u1eadn GD!");
                return false;
            }

            // Chuẩn bị items
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

            // Đặt items vào giao dịch
            GameScr.gI().gameEE = 0;
            GameScr.gameCW = tradeItems;
            Service.gI().gameAA(0, tradeItems);
            GameScr.gI().gameEC = 1; // Khóa giao dịch

            // Chờ đối phương khóa giao dịch
            long start = System.currentTimeMillis();
            while (GameScr.gI().typeTradeOrder != 1 && GameScr.isPaintTrade) {
                if (!isAuto || System.currentTimeMillis() - start >= 15000) {
                    Service.gI().gameAI();
                    return false;
                }
                Auto.Sleep(200L);
            }

            // Bấm Đồng ý
            Auto.Sleep(1000L);
            Service.gI().gameAJ();

            // Chờ hoàn thành giao dịch (hoặc đóng màn hình GD)
            start = System.currentTimeMillis();
            while (GameScr.isPaintTrade && GameScr.gI().typeTradeOrder != 2) {
                if (!isAuto || System.currentTimeMillis() - start >= 4000) {
                    break;
                }
                Auto.Sleep(200L);
            }

            // Đồng bộ
            LockGame.LockAA(1500L);

            // Xóa items khỏi local nếu gửi
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

            // Tự động chấp nhận lời mời giao dịch khi popup xuất hiện
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
