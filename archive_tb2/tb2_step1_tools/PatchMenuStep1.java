import javassist.*;

public final class PatchMenuStep1 {
    public static void main(String[] args) throws Exception {
        ClassPool pool = new ClassPool(true);
        pool.insertClassPath(args[0]);
        pool.insertClassPath(args[1]);
        CtClass menu = pool.get("Class_fi");
        menu.getDeclaredMethod("a", new CtClass[]{pool.get("Class_du")})
            .insertBefore("{ NamModMenu.inject($1); }");
        menu.writeFile(args[2]);
        CtClass game = pool.get("Class_ds");
        game.getDeclaredMethod("a", new CtClass[]{pool.get("java.lang.String"), pool.get("java.lang.String")})
            .insertBefore("{ if (TB2EventCommands.handle($1)) return; }");
        game.getDeclaredMethod("a", new CtClass[]{pool.get("Class_ae")})
            .insertAfter("{ TB2ThongTinBoss.paint($1); TB2ThongKe.paint($1); }");
        game.writeFile(args[2]);
        CtClass code = pool.get("Class_am");
        code.getDeclaredMethod("d", new CtClass[]{pool.get("java.lang.String"), pool.get("java.lang.String")})
            .insertBefore("{ if (TB2AutoSanBoss.handlePartyCommand($1, $2)) return; }");
        code.writeFile(args[2]);
    }
}
