/**
 * AutoRollTAQDung — Bên ẶC ĐỨNG (Emulator 1).
 *
 * Chức năng:
 * - Cầm thẻ Roll TAQ (ID 905) trong hành trang.
 * - Gõ chat: "rolldung" -> Bật/Tắt.
 * - Tự động bấm "Có" (Đồng ý) ngay lập tức khi clone gửi lời mời giao dịch (popup "mời bạn giao dịch").
 * - Tự động xử lý giao dịch thông minh:
 *   + Nếu clone chưa đặt đồ (clone lấy thẻ) -> Đặt 1 thẻ Roll TAQ (ID 905) -> Khóa -> Đồng ý.
 *   + Nếu clone đặt đồ (clone trả đồ) -> Đặt rỗng -> Khóa -> Đồng ý nhận đồ.
 * - Tự động sắp xếp hành trang sau mỗi lần giao dịch.
 */
public class AutoRollTAQDung implements Runnable {
    public static volatile boolean isAuto = false;
    public static int cloneCount = 0;

    /** ID thẻ Roll TAQ */
    private static final int THE_ROLL_TAQ_ID = 905;

    public void run() {
        try {
            GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] B\u1eaft \u0111\u1ea7u Auto \u1eb6c \u0110\u1ee9ng!");
            int soThe = countItemInBag(THE_ROLL_TAQ_ID);
            GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] S\u1ed1 th\u1ebb Roll TAQ (ID 905): " + soThe);
            cloneCount = 0;

            while (isAuto) {
                // 1. Chờ có lời mời giao dịch (tự động bấm "Có" khi hiện dialog mời GD)
                if (!waitForTradeScreen(120000)) {
                    if (!isAuto) break;
                    continue;
                }

                GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] \u0110\u00e3 v\u00e0o giao d\u1ecbch! \u0110ang x\u1eed l\u00fd...");
                Auto.Sleep(300L);

                // 2. Kiểm tra xem clone có đặt đồ vào GD không (GameScr.gameCX)
                boolean cloneHasItems = false;
                if (GameScr.gameCX != null) {
                    for (int c = 0; c < GameScr.gameCX.length; c++) {
                        if (GameScr.gameCX[c] != null) {
                            cloneHasItems = true;
                            break;
                        }
                    }
                }

                Item[] tradeItems = new Item[12];
                if (cloneHasItems) {
                    // Clone đang trả đồ -> ặc đứng đặt rỗng để nhận
                    GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] Clone \u0111ang tr\u1ea3 \u0111\u1ed3 -> Nh\u1eadn \u0111\u1ed3...");
                } else {
                    // Clone lấy thẻ -> ặc đứng đặt 1 thẻ Roll TAQ
                    Char myChar = Char.getMyChar();
                    int count = 0;
                    if (myChar != null && myChar.arrItemBag != null) {
                        for (int i = 0; i < myChar.arrItemBag.length && count < 1; i++) {
                            Item item = myChar.arrItemBag[i];
                            if (item != null && item.template != null && item.template.id == THE_ROLL_TAQ_ID && !item.isLock) {
                                tradeItems[count++] = item;
                            }
                        }
                    }
                    if (count > 0) {
                        GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] \u0110\u1eb7t 1 th\u1ebb Roll TAQ cho clone...");
                    } else {
                        GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] H\u1ebft th\u1ebb Roll TAQ trong h\u00e0nh trang!");
                    }
                }

                // 3. Đặt items và Khóa giao dịch
                GameScr.gI().gameEE = 0;
                GameScr.gameCW = tradeItems;
                Service.gI().gameAA(0, tradeItems);
                GameScr.gI().gameEC = 1; // Khóa GD

                // 4. Chờ đối phương khóa giao dịch (typeTradeOrder == 1)
                long start = System.currentTimeMillis();
                while (GameScr.gI().typeTradeOrder != 1 && GameScr.isPaintTrade) {
                    if (!isAuto || System.currentTimeMillis() - start >= 15000) {
                        Service.gI().gameAI();
                        break;
                    }
                    Auto.Sleep(200L);
                }

                // 5. Bấm Đồng ý
                Auto.Sleep(300L);
                Service.gI().gameAJ();

                // 6. Chờ hoàn thành hoặc đóng màn hình GD
                start = System.currentTimeMillis();
                while (GameScr.isPaintTrade && GameScr.gI().typeTradeOrder != 2) {
                    if (!isAuto || System.currentTimeMillis() - start >= 5000) {
                        break;
                    }
                    Auto.Sleep(200L);
                }

                // Đồng bộ và xóa thẻ đã đưa khỏi local
                if (!cloneHasItems && tradeItems[0] != null) {
                    try {
                        Char.getMyChar().arrItemBag[tradeItems[0].indexUI] = null;
                    } catch (Exception e) {}
                }

                LockGame.LockAA(500L);
                GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] Giao d\u1ecbch th\u00e0nh c\u00f4ng!");

                // Tự động sắp xếp hành trang + rương
                try {
                    Service.gI().gameAG(); // Sắp xếp hành trang
                    Service.gI().gameAF(); // Sắp xếp rương
                } catch (Exception e) {}

                Auto.Sleep(500L);
            }
        } catch (Exception e) {
            GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] L\u1ed7i: " + e.getMessage());
        }
        isAuto = false;
        GameScr.gameAC("[\u1eb6c \u0110\u1ee9ng] D\u1eebng Auto.");
    }

    /** Chờ màn hình giao dịch mở — TỰ ĐỘNG BẤM "CÓ" KHI CÓ DIALOG MỜI GD */
    private boolean waitForTradeScreen(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!GameScr.isPaintTrade) {
            if (!isAuto || System.currentTimeMillis() - start >= timeoutMs) return false;

            // Kiểm tra và tự động bấm nút "Có" (Đồng ý GD) ở cả currentDialog và msgdlg
            acceptTradeDialogIfPresent(GameCanvas.currentDialog);
            acceptTradeDialogIfPresent(GameCanvas.msgdlg);

            Auto.Sleep(150L);
        }
        return true;
    }

    /** Helper tự động click nút "Có" trong dialog mời giao dịch */
    private void acceptTradeDialogIfPresent(Dialog dlg) {
        if (dlg == null) return;
        try {
            // Nút "Có" (Đồng ý) thường nằm ở left hoặc center
            Command cmd = dlg.left;
            if (cmd == null || cmd.idAction != 88810) {
                if (dlg.center != null && dlg.center.idAction == 88810) {
                    cmd = dlg.center;
                }
            }

            if (cmd != null && cmd.idAction == 88810) {
                // 1. Thực thi actionListener chính xác như khi user ấn nút Có trên màn hình
                if (cmd.actionListener != null) {
                    cmd.actionListener.perform(cmd.idAction, cmd.p);
                }
                // 2. Gửi packet chấp nhận giao dịch trực tiếp nếu có charId
                if (cmd.p instanceof Integer) {
                    Service.gI().gameAL(((Integer) cmd.p).intValue());
                }
                // 3. Đóng dialog
                GameCanvas.endDlg();
            }
        } catch (Exception e) {}
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
