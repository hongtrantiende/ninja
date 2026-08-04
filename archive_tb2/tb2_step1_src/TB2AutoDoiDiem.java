public final class TB2AutoDoiDiem implements Runnable {
    public static volatile boolean enabled;

    public static void toggle() {
        enabled = !enabled;
        Class_ds.c(enabled ? "B\u1eadt Auto \u0110\u1ed5i \u0110i\u1ec3m (3356-240)!" : "T\u1eaft Auto \u0110\u1ed5i \u0110i\u1ec3m!");
        if (enabled) new Thread(new TB2AutoDoiDiem()).start();
    }

    public void run() {
        try {
            while (enabled) {
                Class_dk player = Class_dk.g();
                if (player == null) {
                    Thread.sleep(10L);
                    continue;
                }
                if (player.k != 3356 || player.l != 240) {
                    Class_dk.e(3356, 240);
                }
                Class_cx.m();
                Class_dz.a();
                Class_di.a().h(63);
                Thread.sleep(10L);
                if (!enabled) break;
                Class_di.a().c(63, 1, 0);
                Class_di.a().c(63, 1);
                Thread.sleep(10L);
                Class_cx.m();
                Class_dz.a();
                Thread.sleep(10L);
            }
        } catch (Exception ignored) {
        }
    }
}
