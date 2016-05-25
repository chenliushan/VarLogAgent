package process;

import javassist.*;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by liushanchen on 16/5/23.
 * This program
 * 1.compile the target .java file
 * 2.overwrites current target file by adding log statements
 * to print the known field variables and local variables in runtime.
 * Note: the javassist's compiler is not support inner class
 * -get method by method name and parameters
 * -if the local variable is not initialized just log a string that the variable is not initialized
 */
public class ByteCodePMethod extends ByteCodeP {
    private CtClass cc = null;


    public byte[] transformClass(byte[] b) {
        try {
            cc = poolParent.makeClass(new java.io.ByteArrayInputStream(b));
            importLogPack(cc);
            findMethods(cc);
            findNestedClass(cc);
            b = cc.toBytecode();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if (cc != null) {
                cc.detach();
            }
        }
        return b;
    }

    /**
     * find the method in the target
     *
     * @return
     */
    private void findMethods(CtClass ctClass) {
        for (CtMethod cb : Arrays.asList(ctClass.getDeclaredMethods())) {
            try {
                if(!cb.isEmpty()){
                    logMethod(cb);
                }
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
        }
        for (CtConstructor cb : Arrays.asList(ctClass.getDeclaredConstructors())) {
            try {
                if(!cb.isEmpty()){
                    logMethod(cb);
                }
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
        }
    }

    private void findNestedClass(CtClass ctClass) {
        try {
            CtClass[] nccs = ctClass.getNestedClasses();
            for (int i = 0; i < nccs.length; i++) {
                declareLogger(nccs[i]);
                findMethods(nccs[i]);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * insert log into the method body.
     *
     * @param method
     * @throws CannotCompileException
     */
    private void logMethod(CtConstructor method) throws CannotCompileException {
        method.insertBefore(lo.getMethodName(method.getLongName()));
    }

    private void logMethod(CtMethod method) throws CannotCompileException {
        method.insertAfter(lo.getMethodName(method.getLongName()));
    }

}

