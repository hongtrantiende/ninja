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

        int maxRetries = 3;
        for (int retry = 0; retry <= maxRetries; retry++) {
            try {
                if (retry > 0) {
                    GameScr.gameAC("[AutoLogin] Retry " + retry + " TK: " + user);
                    Auto.Sleep(2000L);
                } else {
                    GameScr.gameAC("[AutoLogin] Login TK: " + user + "...");
                }

                // Reset NV cu
                try {
                    if (SelectCharScr.gameAA() != null) {
                        SelectCharScr.gameAA().name = null;
                        SelectCharScr.gameAA().isNullChar = true;
                    }
                    Char.gameAJ();
                } catch (Exception e) {}

                // Disconnect
                try {
                    if (Session_ME.gameAA() != null) {
                        Session_ME.gameAA().gameAB();
                    }
                } catch (Exception e) {}
                Auto.Sleep(800L);

                // Set user/pass
                try {
                    SelectServerScr.uname = user;
                    SelectServerScr.pass = pass;
                    RMS.gameAA("acc", user);
                    RMS.gameAA("pass", pass);
                } catch (Exception e) {}

                // Ve LoginScr
                try {
                    if (GameCanvas.loginScr != null) {
                        GameCanvas.loginScr.gameAB();
                    }
                } catch (Exception e) {}
                Auto.Sleep(800L);

                // Don dep dialog cu
                try {
                    GameCanvas.isLoading = false;
                    GameCanvas.endDlg();
                    InfoDlg.gameAD();
                    LockGame.gameBK();
                } catch (Exception e) {}

                // Connect socket
                try {
                    GameCanvas.gameAC();
                } catch (Exception e) {}
                Auto.Sleep(1200L);

                // Gui login
                Service.gI().gameAA(user, pass, "1.8.8");
                LoginScr.isLoggingIn = true;
                boolean sentLogin = true;

                // Cho ket qua — timeout 40 giay
                long start = System.currentTimeMillis();
                int createAttempts = 0;
                boolean needRetry = false;

                while (System.currentTimeMillis() - start < 40000) {
                    Auto.Sleep(300L);

                    // A: Vao game thanh cong
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
                            GameScr.gameAC("[AutoLogin] OK: " + user + " (" + myChar.cName + ")");
                            return true;
                        }
                    }

                    // B: Chon nhan vat
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
                            SelectCharScr.gameAK = existingChar;
                            Service.gI().gameAB(existingChar);
                            Auto.Sleep(1500L);
                        } else if (scs.isNullChar || scs.name == null) {
                            String charName = getCleanCharName(user, createAttempts++);
                            Service.gI().gameAA(charName, 1, 2);
                            Auto.Sleep(2000L);
                        }
                        continue;
                    }

                    // C: Tao nhan vat
                    if (GameCanvas.currentScreen == CreateCharScr.gameAA()) {
                        String charName = getCleanCharName(user, createAttempts++);
                        Service.gI().gameAA(charName, 1, 2);
                        Auto.Sleep(2000L);
                        continue;
                    }

                    // D: Bi da ve LoginScr SAU KHI da gui login = qua tai / loi
                    // Chi detect sau 5 giay (tranh nham luc dang ket noi)
                    if (sentLogin && System.currentTimeMillis() - start > 5000) {
                        if (GameCanvas.currentScreen instanceof LoginScr) {
                            GameScr.gameAC("[AutoLogin] Bi da ve Login (qua tai?) - thu lai...");
                            try {
                                GameCanvas.endDlg();
                                InfoDlg.gameAD();
                                LockGame.gameBK();
                                GameCanvas.isLoading = false;
                            } catch (Exception e) {}
                            needRetry = true;
                            break;
                        }
                    }
                }

                if (needRetry) continue;

                // Timeout nhung chua vao game
                if (retry < maxRetries) {
                    GameScr.gameAC("[AutoLogin] Timeout 40s! Thu lai...");
                    try {
                        GameCanvas.endDlg();
                        InfoDlg.gameAD();
                        LockGame.gameBK();
                        GameCanvas.isLoading = false;
                    } catch (Exception e) {}
                    continue;
                }

            } catch (Exception e) {
                GameScr.gameAC("[AutoLogin] Loi: " + e.getMessage());
            } finally {
                GameCanvas.isLoading = false;
                InfoDlg.gameAD();
                LockGame.gameBK();
            }
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
