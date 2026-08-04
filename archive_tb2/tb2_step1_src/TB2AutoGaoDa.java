public final class TB2AutoGaoDa implements Runnable {
    public static volatile boolean enabled;

    public static void toggle() {
        enabled = !enabled;
        Class_ds.c(enabled ? "B\u1eadt Auto G\u1ea1o \u0110\u00e1!" : "T\u1eaft Auto G\u1ea1o \u0110\u00e1!");
        if (enabled) new Thread(new TB2AutoGaoDa()).start();
    }

    public void run() {
        try {
            while (enabled) {
                Class_di.a().h(62);
                Thread.sleep(10L);
                Class_di.a().c(62, 0, 0);
                Class_di.a().c(62, 0);
                Class_cx.m();
                Class_dz.a();
                Thread.sleep(10L);
                if (!enabled) break;

                Class_di.a().h(63);
                Thread.sleep(10L);
                Class_di.a().c(63, 0, 0);
                Class_di.a().c(63, 0);
                Class_cx.m();
                Class_dz.a();
                Thread.sleep(10L);
            }
        } catch (Exception ignored) {
        }
    }
}
