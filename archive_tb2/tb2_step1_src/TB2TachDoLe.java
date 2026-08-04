/** STEP 2: tach mon dang chon trong hanh trang thanh tung mon le. */
public final class TB2TachDoLe implements Runnable {
    private final int index;
    private final int count;

    private TB2TachDoLe(int index, int count) {
        this.index = index;
        this.count = count;
    }

    public static void openInput() {
        Class_cx.ak.a("Nhap so luong tach le", new Class_db("Tach", new Class_bn() {
            public void a(int id, Object parameter) {
                try {
                    int count = Integer.parseInt(Class_cx.ak.d.d().trim());
                    if (count <= 0) throw new Exception();
                    int index = Class_ds.ak;
                    new Thread(new TB2TachDoLe(index, count)).start();
                } catch (Exception error) {
                    Class_ds.c("So luong khong hop le!");
                }
                Class_cx.m();
            }
        }, 120112, null), 1);
        Class_cx.ak.d.a("3");
    }

    public void run() {
        try {
            Class_ds.c("Dang tach le " + count + " mon...");
            for (int i = 0; i < count; i++) {
                Class_di.a().k(index, 1);
                Thread.sleep(150L);
            }
            Class_ds.c("Da tach xong " + count + " mon le!");
        } catch (Exception error) {
            Class_ds.c("Loi khi tach do!");
        }
    }
}
