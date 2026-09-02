/**
 * AutoCreateAllChars — Tự động đăng nhập từng tài khoản dự phòng,
 * tạo nhân vật, vào game, rồi đăng xuất chuyển tk tiếp theo.
 *
 * Lệnh chat:
 * - taonv all              -> Chạy tất cả 9600 tk
 * - taonv all buncha5      -> Bắt đầu từ buncha5
 * - taonv buncha           -> Chạy 1 nhóm buncha (1-20)
 * - taonv stop             -> Dừng
 */
public class AutoCreateAllChars implements Runnable {
    public static volatile boolean isAuto = false;
    public static java.util.Vector accountList = new java.util.Vector();

    public void run() {
        try {
            int total = accountList.size();
            if (total == 0) {
                GameScr.gameAC("[TaoNV] Danh sach trong!");
                isAuto = false;
                return;
            }

            GameScr.gameAC("[TaoNV] === BAT DAU: " + total + " tai khoan ===");
            int ok = 0;
            int fail = 0;

            for (int a = 0; a < total && isAuto; a++) {
                String[] acc = (String[]) accountList.elementAt(a);
                String user = acc[0];
                String pass = acc[1];

                GameScr.gameAC("[TaoNV] [" + (a + 1) + "/" + total + "] " + user);

                boolean loginOk = AutoLogin.doLogin(user, pass);
                if (loginOk) {
                    ok++;
                    GameScr.gameAC("[TaoNV] OK: " + user + " (" + ok + "/" + (a + 1) + ")");
                    Auto.Sleep(1000L);
                } else {
                    fail++;
                    GameScr.gameAC("[TaoNV] FAIL: " + user);
                    Auto.Sleep(500L);
                }

                if ((a + 1) % 50 == 0) {
                    GameScr.gameAC("[TaoNV] === TIEN DO: " + (a + 1) + "/" + total + " | OK:" + ok + " FAIL:" + fail + " ===");
                }
            }

            GameScr.gameAC("[TaoNV] === HOAN TAT! OK:" + ok + " FAIL:" + fail + " TONG:" + total + " ===");
        } catch (Exception e) {
            GameScr.gameAC("[TaoNV] Loi: " + e.getMessage());
        } finally {
            isAuto = false;
            accountList.removeAllElements();
        }
    }

    public static java.util.Vector generateBackupAccounts(String param, String startFrom) {
        java.util.Vector list = new java.util.Vector();
        String p = (param != null) ? param.toLowerCase().trim() : "";
        String start = (startFrom != null) ? startFrom.toLowerCase().trim() : "";

        String[] groups = new String[] {
            "buncha", "bunmam", "bunrau", "bunbon", "buntau",
            "bungio", "bunhue", "bunken", "bunkho", "bunsat",
            "comgao", "comran", "comtam", "combin", "comcay",
            "comtoi", "comcha", "comxao", "comhop", "comsen",
            "phobac", "photai", "phonam", "phochn", "phocua",
            "phosat", "phoxao", "phokho", "photom", "phocay",
            "miexao", "mievit", "mieheo", "miequa", "mienam",
            "miebac", "miegan", "miekho", "mietom", "miecua",
            "chagio", "chaque", "chanem", "chadon", "chalam",
            "chabun", "chacom", "chapho", "chamie", "chabap",
            "kemlan", "kemcam", "kemdau", "kemsua", "kemqua",
            "kemtao", "kemnho", "kemdua", "kemman", "kemxoi",
            "capcay", "capnam", "capbap", "capkho", "capheo",
            "capcam", "capdau", "capsua", "capxoi", "capkem",
            "ganheo", "gancay", "ganran", "gankho", "gannau",
            "gantom", "ganbam", "gancua", "gansat", "gancha",
            "sotkho", "sotcay", "sotcam", "sotdau", "sotvit",
            "sotheo", "sottom", "sotcua", "sotnau", "sotran",
            "topcam", "topdau", "topsua", "topxoi", "topkem",
            "topnho", "topdua", "topman", "topque", "toptao",
            "nuocep", "nuocda", "nuocla", "nuocoi", "nuocam",
            "nuocot", "nuocme", "nuocvo", "nuocdl", "nuocbo",
            "sinhhp", "sinhto", "sinhdl", "sinhnl", "sinhat",
            "sinhbo", "sinhcm", "sinhdu", "sinhkl", "sinhtl",
            "tramix", "tratao", "tradau", "trasun", "tradao",
            "trakim", "trabac", "tranam", "travai", "tracam",
            "capcfe", "cafeda", "cafeno", "cafesu", "cafedl",
            "cafecm", "cafenl", "cafeat", "cafebo", "cafekl",
            "cocola", "cocacl", "cocamx", "cocadl", "cocaep",
            "cocame", "cocavo", "cocakl", "cocanl", "cocacm",
            "suadau", "suacam", "suadua", "suaman", "suaque",
            "suatao", "suanho", "suaxoi", "suakem", "suabot",
            "sodacl", "sodamx", "sodadl", "sodaep", "sodame",
            "sodavo", "sodakl", "sodanl", "sodacm", "sodabp",
            "yogurt", "yogudl", "yogunl", "yoguat", "yogubo",
            "yogucm", "yogukl", "yogusu", "yoguep", "yogume",
            "matcha", "matchb", "matchc", "matchd", "matche",
            "matchf", "matchg", "matchh", "matchi", "matchj",
            "camqui", "camsai", "cambac", "camnam", "camtay",
            "camdao", "camkim", "camvai", "camdua", "camman",
            "buoich", "buoisa", "buoiba", "buoina", "buoita",
            "buoida", "buoiki", "buoiva", "buoidu", "buoima",
            "xoaisp", "xoaich", "xoaisa", "xoaiba", "xoaina",
            "xoaita", "xoaida", "xoaiki", "xoaiva", "xoaidu",
            "nhansp", "nhancl", "nhansa", "nhanba", "nhanna",
            "nhanta", "nhanda", "nhanki", "nhanva", "nhandu",
            "voiclm", "voicla", "voiclb", "voicld", "voicle",
            "voiclf", "voiclg", "voiclh", "voicli", "voiclj",
            "mitcay", "mitdua", "mitman", "mitque", "mittao",
            "mitnho", "mitkem", "mitxoi", "mitbot", "mitcam",
            "duacam", "duaman", "duaque", "duatao", "duanho",
            "duakem", "duaxoi", "duabot", "duasen", "duadau",
            "oiduoi", "oichua", "oingot", "oidang", "oicham",
            "oisach", "oixanh", "oidong", "oivang", "oitron",
            "lehdai", "lehsen", "lehdau", "lehcam", "lehman",
            "lehque", "lehtao", "lehnho", "lehkem", "lehxoi",
            "gatroi", "gatron", "gavang", "gaxanh", "gacham",
            "gadong", "gatien", "gavach", "gadoix", "gacuon",
            "vitcon", "vittro", "vitvan", "vitxan", "vitcha",
            "vitdon", "vittie", "vitvac", "vitdoi", "vitcuo",
            "meocon", "meotro", "meovan", "meoxan", "meocha",
            "meodon", "meotie", "meovac", "meodoi", "meocuo",
            "chocon", "chotro", "chovan", "choxan", "chocha",
            "chodon", "chotie", "chovac", "chodoi", "chocuo",
            "cacong", "cactro", "cacvan", "cacxan", "caccha",
            "cacdon", "cactie", "cacvac", "cacdoi", "caccuo",
            "tomcon", "tomtro", "tomvan", "tomxan", "tomcha",
            "tomdon", "tomtie", "tomvac", "tomdoi", "tomcuo",
            "cuacon", "cuatro", "cuavan", "cuaxan", "cuacha",
            "cuadon", "cuatie", "cuavac", "cuadoi", "cuacuo",
            "ongcon", "ongtro", "ongvan", "ongxan", "ongcha",
            "ongdon", "ongtie", "ongvac", "ongdoi", "ongcuo",
            "kiencn", "kientr", "kienva", "kienxa", "kienco",
            "kiendo", "kienti", "kienvc", "kiendi", "kiencu",
            "oconai", "ocotrn", "ocovng", "ocoxnh", "ococha",
            "ocosng", "ocotie", "ocovac", "ocodoi", "ococuo",
            "xanhla", "xanhda", "xanhdu", "xanhma", "xanhbi",
            "xanhco", "xanhti", "xanhvc", "xanhdi", "xanhcu",
            "vangla", "vangda", "vangdu", "vangma", "vangbi",
            "vangco", "vangti", "vangvc", "vangdi", "vangcu",
            "doclam", "doclan", "doclav", "doclax", "doclac",
            "doclas", "doclat", "doclau", "doclad", "doclae",
            "timcon", "timtro", "timvan", "timxan", "timcha",
            "timdon", "timtie", "timvac", "timdoi", "timcuo",
            "honcon", "hontro", "honvan", "honxan", "honcha",
            "hondon", "hontie", "honvac", "hondoi", "honcuo",
            "dencon", "dentro", "denvan", "denxan", "dencha",
            "dendon", "dentie", "denvac", "dendoi", "dencuo",
            "namcon", "namtro", "namvan", "namxan", "namcha",
            "namdon", "namtie", "namvac", "namdoi", "namcuo",
            "camcon", "camtro", "camvan", "camxan", "camcha",
            "camdon", "camtie", "camvac", "camdoi", "camcuo",
            "baccon", "bactro", "bacvan", "bacxan", "baccha",
            "bacdon", "bactie", "bacvac", "bacdoi", "baccuo",
            "dongla", "dongda", "dongdu", "dongma", "dongbi",
            "dongco", "dongti", "dongvc", "dongdi", "dongcu"
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
            for (int g = 0; g < groups.length; g++) {
                for (int i = 1; i <= 20; i++) {
                    list.addElement(new String[] { groups[g] + i, "000000" });
                }
            }
            return list;
        }

        for (int g = 0; g < groups.length; g++) {
            if (p.startsWith(groups[g])) {
                String numStr = p.substring(groups[g].length()).trim();
                int startNum = 1;
                if (numStr.length() > 0) {
                    try { startNum = Integer.parseInt(numStr); } catch (Exception e) { startNum = 1; }
                }
                for (int i = startNum; i <= 20; i++) {
                    list.addElement(new String[] { groups[g] + i, "000000" });
                }
                return list;
            }
        }

        list.addElement(new String[] { p, "000000" });
        return list;
    }
}
