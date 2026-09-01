/**
 * AutoLogin — Tự động Đăng xuất -> Đăng nhập tài khoản mới -> Tự tạo hoặc chọn nhân vật -> Vào game.
 *
 * Hỗ trợ các lệnh chat:
 * - dncacao -> Đăng xuất và đăng nhập vào tài khoản cacao1 (pass 000000)
 * - dncacao2 -> Đăng nhập vào cacao2
 * - dn vaicalon1 -> Đăng nhập vào vaicalon1
 * - dn [tên_tk] [pass] -> Đăng nhập với tài khoản & mật khẩu tùy ý
 */
public class AutoLogin implements Runnable {
    public static volatile boolean isRunning = false;
    public String username;
    public String password;

    public AutoLogin(String username, String password) {
        this.username = username;
        this.password = (password != null && password.length() > 0) ? password : "000000";
    }

    public void run() {
        if (isRunning) {
            GameScr.gameAC("[AutoLogin] \u0110ang c\u00f3 ti\u1ebfn tr\u00ecnh \u0111\u0103ng nh\u1eadp kh\u00e1c \u0111ang ch\u1ea1y!");
            return;
        }
        isRunning = true;
        try {
            GameScr.gameAC("[AutoLogin] B\u1eaft \u0111\u1ea7u \u0111\u1ed5i sang TK: " + username + "...");

            // 1. Đăng xuất nếu đang trong game
            try {
                if (Session_ME.gameAA() != null) {
                    Session_ME.gameAA().gameAB(); // disconnect socket
                }
            } catch (Exception e) {}
            Auto.Sleep(1000L);

            // Chuyển màn hình về LoginScr
            try {
                if (GameCanvas.loginScr != null) {
                    GameCanvas.loginScr.gameAB();
                }
            } catch (Exception e) {}
            Auto.Sleep(1200L);

            // 2. Dọn dẹp dialog cũ và kết nối lại máy chủ
            try {
                GameCanvas.endDlg();
                InfoDlg.gameAB();
            } catch (Exception e) {}

            // Kết nối socket đến máy chủ game
            try {
                GameCanvas.gameAC();
            } catch (Exception e) {}
            Auto.Sleep(1000L);

            // Gửi gói tin đăng nhập
            Service.gI().gameAA(username, password, "1.8.8");
            LoginScr.isLoggingIn = true;
            GameScr.gameAC("[AutoLogin] \u0110\u00e3 g\u1eedi th\u00f4ng tin \u0111\u0103ng nh\u1eadp...");

            // 3. Vòng lặp chờ nhận danh sách NV / tạo NV / vào game
            long start = System.currentTimeMillis();
            while (isRunning && System.currentTimeMillis() - start < 30000) {
                Auto.Sleep(300L);

                // TRƯỜNG HỢP A: Đã vào game thành công (GameScr)
                if (GameCanvas.currentScreen instanceof GameScr) {
                    Char myChar = Char.getMyChar();
                    if (myChar != null && myChar.cName != null && myChar.cName.length() > 0) {
                        GameCanvas.endDlg();
                        InfoDlg.gameAB();
                        GameScr.gameAC("[AutoLogin] === \u0110\u0102NG NH\u1eacP TH\u00c0NH C\u00d4NG! ===");
                        GameScr.gameAC("[AutoLogin] T\u00e0i kho\u1ea3n: " + username + " (NV: " + myChar.cName + ")");
                        InfoMe.gameAA("\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng: " + username);
                        break;
                    }
                }

                // TRƯỜNG HỢP B: Màn hình chọn nhân vật (SelectCharScr)
                if (GameCanvas.currentScreen == SelectCharScr.gameAA()) {
                    SelectCharScr scs = SelectCharScr.gameAA();
                    String existingChar = null;
                    if (scs.name != null) {
                        for (int i = 0; i < scs.name.length; i++) {
                            if (scs.name[i] != null && scs.name[i].length() > 0) {
                                existingChar = scs.name[i];
                                break;
                            }
                        }
                    }

                    if (existingChar != null) {
                        // Đã có nhân vật -> Chọn nhân vật vào game
                        GameScr.gameAC("[AutoLogin] Ch\u1ecdn nh\u00e2n v\u1eadt: " + existingChar + "...");
                        SelectCharScr.gameAK = existingChar;
                        Service.gI().gameAB(existingChar);
                        GameCanvas.gameAB(mResources.gameBG);
                        GameCanvas.isLoading = true;
                        Auto.Sleep(2000L);
                    } else {
                        // Chưa có nhân vật -> Tạo nhân vật mới với tên là username
                        GameScr.gameAC("[AutoLogin] T\u1ea1o nh\u00e2n v\u1eadt m\u1edbi: " + username + "...");
                        Service.gI().gameAA(username, 1, 2); // Nam (1), Tóc (2)
                        Auto.Sleep(2500L);
                    }
                    continue;
                }

                // TRƯỜNG HỢP C: Màn hình tạo nhân vật (CreateCharScr)
                if (GameCanvas.currentScreen == CreateCharScr.gameAA()) {
                    GameScr.gameAC("[AutoLogin] \u0110ang t\u1ea1o nh\u00e2n v\u1eadt: " + username + "...");
                    Service.gI().gameAA(username, 1, 2); // Nam (1), Tóc (2)
                    Auto.Sleep(2500L);
                    continue;
                }
            }

        } catch (Exception e) {
            GameScr.gameAC("[AutoLogin] L\u1ed7i: " + e.getMessage());
        } finally {
            isRunning = false;
        }
    }
}
