package process;

import javassist.*;
import javassist.bytecode.analysis.ControlFlow;

/**
 * Created by liushanchen on 16/5/23.
 */
public abstract class ByteCodeP {

    protected ClassPool poolParent = null;
    protected LogOutput lo = null;

    public ByteCodeP() {
        this.poolParent = getClassPool();
        this.lo = new LogOutput();
    }

    public abstract byte[] transformClass( byte[] b);

    /**
     * get the targetClassPool that contain the target classpath
     *
     * @return targetClassPool
     */
    private ClassPool getClassPool() {
        ClassPool pool = ClassPool.getDefault();

        return pool;
    }


    /**
     * import the Log package and declare the log variable
     *
     * @param cc the target bytecode file
     */
    protected void importLogPack(CtClass cc) {
        String[] importP = lo.getImportPackages();
        for (int i = 0; i < importP.length; i++) {
            poolParent.importPackage(importP[i]);

        }
        declareLogger(cc);
    }

    protected void declareLogger(CtClass cc) {
        try {
            CtField field = CtField.make(lo.getDeclaration(), cc);
            cc.addField(field);
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

//    protected void declareControlFlow(CtMethod cm) {
//        try {
//            ControlFlow cf = new ControlFlow(cm);
//
//            cc.addField(field);
//        } catch (CannotCompileException e) {
//            e.printStackTrace();
//        }
//    }


}

