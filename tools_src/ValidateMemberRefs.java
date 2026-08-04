import java.io.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.*;

public final class ValidateMemberRefs {
    static final class Def {
        String parent; String[] interfaces;
        final Set<String> fields=new HashSet<String>(), methods=new HashSet<String>();
    }
    static final Map<String,Def> defs=new HashMap<String,Def>();
    static final List<String[]> refs=new ArrayList<String[]>();
    static String key(String n,String d){return n+'\n'+d;}
    static boolean exists(String owner,String name,String desc,boolean method,Set<String> seen){
        if(owner==null||!seen.add(owner))return false; Def d=defs.get(owner);if(d==null)return true;
        if((method?d.methods:d.fields).contains(key(name,desc)))return true;
        if(exists(d.parent,name,desc,method,seen))return true;
        if(d.interfaces!=null)for(String i:d.interfaces)if(exists(i,name,desc,method,seen))return true;
        return false;
    }
    public static void main(String[] args)throws Exception{
        JarFile jar=new JarFile(args[0]);Enumeration<JarEntry> en=jar.entries();
        while(en.hasMoreElements()){JarEntry e=en.nextElement();if(!e.getName().endsWith(".class"))continue;ClassReader cr=new ClassReader(jar.getInputStream(e));Def d=new Def();defs.put(cr.getClassName(),d);cr.accept(new ClassVisitor(Opcodes.ASM8){
            String current;
            public void visit(int v,int a,String n,String s,String p,String[] is){current=n;d.parent=p;d.interfaces=is;}
            public FieldVisitor visitField(int a,String n,String ds,String s,Object v){d.fields.add(key(n,ds));return null;}
            public MethodVisitor visitMethod(int a,String n,String ds,String s,String[] ex){d.methods.add(key(n,ds));return new MethodVisitor(Opcodes.ASM8){
                public void visitFieldInsn(int op,String o,String n,String de){refs.add(new String[]{current,o,n,de,"F"});}
                public void visitMethodInsn(int op,String o,String n,String de,boolean itf){refs.add(new String[]{current,o,n,de,"M"});}
            };}
        },0);}
        Set<String> missing=new TreeSet<String>();
        for(String[] r:refs)if(defs.containsKey(r[1])&&!exists(r[1],r[2],r[3],r[4].equals("M"),new HashSet<String>()))missing.add(r[0]+" -> "+r[1]+"."+r[2]+r[3]);
        System.out.println("Internal classes="+defs.size()+" refs="+refs.size()+" missing="+missing.size());for(String s:missing)System.out.println(s);if(!missing.isEmpty())System.exit(1);
    }
}
