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
            doLogin(username, password);
        } catch (Exception e) {
            GameScr.gameAC("[AutoLogin] L\u1ed7i: " + e.getMessage());
        } finally {
            isRunning = false;
        }
    }

    /**
     * Phương thức tĩnh thực hiện toàn bộ luồng đăng nhập & chọn/tạo nhân vật.
     * @return true nếu vào game thành công, false nếu thất bại/timeout.
     */
    public static boolean doLogin(String user, String pass) {
        if (user == null || user.length() == 0) return false;
        if (pass == null || pass.length() == 0) pass = "000000";

        try {
            GameScr.gameAC("[AutoLogin] B\u1eaft \u0111\u1ea7u \u0111\u1ed5i sang TK: " + user + "...");

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

            // 2. Dọn dẹp dialog cũ và tắt loading (DÙNG InfoDlg.gameAD() để HỦY dialog)
            try {
                GameCanvas.isLoading = false;
                GameCanvas.endDlg();
                InfoDlg.gameAD();
                LockGame.gameBK();
            } catch (Exception e) {}

            // Kết nối socket đến máy chủ game
            try {
                GameCanvas.gameAC();
            } catch (Exception e) {}
            Auto.Sleep(1000L);

            // Gửi gói tin đăng nhập
            Service.gI().gameAA(user, pass, "1.8.8");
            LoginScr.isLoggingIn = true;
            GameScr.gameAC("[AutoLogin] \u0110\u00e3 g\u1eedi th\u00f4ng tin \u0111\u0103ng nh\u1eadp...");

            // 3. Vòng lặp chờ nhận danh sách NV / tạo NV / vào game
            long start = System.currentTimeMillis();
            int createAttempts = 0;

            while (System.currentTimeMillis() - start < 35000) {
                Auto.Sleep(300L);

                // TRƯỜNG HỢP A: Đã vào game thành công (GameScr)
                if (GameCanvas.currentScreen instanceof GameScr) {
                    Char myChar = Char.getMyChar();
                    if (myChar != null && myChar.cName != null && myChar.cName.length() > 0) {
                        GameCanvas.isLoading = false;
                        GameCanvas.endDlg();
                        InfoDlg.gameAD();
                        LockGame.gameBK();
                        try {
                            myChar.isLockMove = false;
                            myChar.isLockAttack = false;
                        } catch (Exception e) {}

                        GameScr.gameAC("[AutoLogin] === \u0110\u0102NG NH\u1eacP TH\u00c0NH C\u00d4NG! ===");
                        GameScr.gameAC("[AutoLogin] T\u00e0i kho\u1ea3n: " + user + " (NV: " + myChar.cName + ")");
                        InfoMe.gameAA("\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng: " + user);
                        return true;
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
                        Auto.Sleep(2000L);
                    } else {
                        // Chưa có nhân vật -> Tạo nhân vật mới với tên sạch (tránh lọc từ tục)
                        String charName = getCleanCharName(user, createAttempts++);
                        GameScr.gameAC("[AutoLogin] T\u1ea1o nh\u00e2n v\u1eadt: " + charName + "...");
                        Service.gI().gameAA(charName, 1, 2); // Nam (1), Tóc (2)
                        Auto.Sleep(2500L);
                    }
                    continue;
                }

                // TRƯỜNG HỢP C: Màn hình tạo nhân vật (CreateCharScr)
                if (GameCanvas.currentScreen == CreateCharScr.gameAA()) {
                    String charName = getCleanCharName(user, createAttempts++);
                    GameScr.gameAC("[AutoLogin] \u0110ang t\u1ea1o nh\u00e2n v\u1eadt: " + charName + "...");
                    Service.gI().gameAA(charName, 1, 2); // Nam (1), Tóc (2)
                    Auto.Sleep(2500L);
                    continue;
                }
            }

        } catch (Exception e) {
            GameScr.gameAC("[AutoLogin] L\u1ed7i: " + e.getMessage());
        } finally {
            GameCanvas.isLoading = false;
            InfoDlg.gameAD();
            LockGame.gameBK();
        }
        return false;
    }

    /**
     * Tạo tên nhân vật sạch sẽ, không bị bộ lọc từ cấm của máy chủ chặn
     */
    public static String getCleanCharName(String user, int attempt) {
        if (user == null || user.length() == 0) {
            return "ninja" + (System.currentTimeMillis() % 10000);
        }
        String name = user.toLowerCase();
        // Lọc các từ ngữ máy chủ kiểm duyệt
        name = replaceString(name, "cacao", "kacao");
        name = replaceString(name, "vaicalon", "vcalon");
        name = replaceString(name, "cac", "kac");
        name = replaceString(name, "lon", "ln");
        name = replaceString(name, "buoi", "boi");
        name = replaceString(name, "cu", "ku");

        if (attempt == 1) {
            name = "k" + name;
        } else if (attempt >= 2) {
            name = "hr" + (System.currentTimeMillis() % 100000);
        }

        // Độ dài chuẩn trong Ninja School: 4 đến 15 ký tự
        if (name.length() < 4) {
            name = "hr" + name;
        }
        if (name.length() > 15) {
            name = name.substring(0, 15);
        }
        return name;
    }

    /** Helper thay thế chuỗi tương thích J2ME CLDC 1.1 */
    public static String replaceString(String src, String target, String replacement) {
        if (src == null || target == null || replacement == null || target.length() == 0) return src;
        StringBuffer sb = new StringBuffer();
        int start = 0;
        int idx = 0;
        while ((idx = src.indexOf(target, start)) != -1) {
            sb.append(src.substring(start, idx));
            sb.append(replacement);
            start = idx + target.length();
        }
        sb.append(src.substring(start));
        return sb.toString();
    }
}
