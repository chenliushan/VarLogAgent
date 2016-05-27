package process;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import polyu_af.models.LineAccessVars;
import polyu_af.models.MethodAccessVars;
import polyu_af.models.MyExp;
import polyu_af.models.TargetFile;
import polyu_af.utils.FileUtils;

import java.io.IOException;
import java.util.List;

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
public class ByteCodePVars extends ByteCodeP {
    private boolean isNestedMethod = false;
    private List<TargetFile> tfList = null;
    private TargetFile tf = null;
    private CtClass cc = null;

    /**
     * get all the needed data at once
     */
    public ByteCodePVars() {
        super();
        getTargetFiles();
    }

    /**
     * get all the needed data at once
     */
    private void getTargetFiles() {
        this.tfList = FileUtils.json2Obj();
        System.out.println("First step tmp output is read ...");
    }

    /**
     * get all the needed data at once
     */
    private TargetFile getCurrentTF(String className) {
        if (tfList != null) {
            for (TargetFile tf : tfList) {
                System.out.println("getDirAndFileName:"+tf.getDirAndFileName());
                if (tf.getDirAndFileName().equals(className)) {
                    return tf;
                }
            }
        } else {
            System.err.println("First step tmp output is not found!");
        }
        return null;
    }


    public byte[] transformClass(byte[] b, String className) {

        this.tf = getCurrentTF(className);
        return transformClass(b);
    }

    public byte[] transformClass(byte[] b) {

        if (tf != null) {
            try {
                cc = poolParent.makeClass(new java.io.ByteArrayInputStream(b));
                importLogPack(cc);
                forMethods(tf.getMethodAccessVars());
                b = cc.toBytecode();

            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (cc != null) {
                    cc.detach();
                }
            }
        }
        return b;
    }


    /**
     * apply the modification to byte code in the loader
     * since the inner class will not invoke the .tobytecode() method
     * it should use this instead.
     */
    private void rewrite(CtClass ctClass) {
        try {
            ctClass.toClass();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    private void forMethods(List<MethodAccessVars> accessVar4MethodList) {
        for (MethodAccessVars methodAccessVar : accessVar4MethodList) {
            isNestedMethod = false;
            //find the method in the target bytecode file
            CtBehavior mainMethod = findTMethod(methodAccessVar);
            //insert log to get the value of every accessible variable
            if (mainMethod == null) {
                System.err.println("mainMethod==null  did not get the method name: " + methodAccessVar.getMethodName() + "!!!!!!");
                continue;
            } else {
                if (!isNestedMethod) {
                    logVarValue(methodAccessVar.getVarsList(), mainMethod);

                }
            }
        }
    }

    /**
     * find the method in the target
     *
     * @param methodAccessVar
     * @return
     */
    private CtBehavior findTMethod(MethodAccessVars methodAccessVar) {
        CtBehavior mainMethod = null;
        List<CtClass> ps = methodAccessVar.getParams(poolParent);
        CtClass[] prams = ps.toArray(new CtClass[ps.size()]);
        if (tf.getFileName().equals(methodAccessVar.getMethodName())) {
            try {
                mainMethod = cc.getDeclaredConstructor(prams);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                mainMethod = cc.getDeclaredMethod(methodAccessVar.getMethodName(), prams);
            } catch (NotFoundException e) {
                try {
                    CtClass[] nccs = cc.getNestedClasses();
                    //find the first method has that name and paras in nested classes
                    for (int i = 0; i < nccs.length; i++) {
                        try {
                            mainMethod = nccs[i].getDeclaredMethod(methodAccessVar.getMethodName(), prams);
                        } catch (NotFoundException e1) {
                            continue;
                        }
                        if (mainMethod != null) {
                            isNestedMethod = true;
                            declareLogger(nccs[i]);
                            logNestedCVarValue(methodAccessVar.getVarsList(), mainMethod);
                            rewrite(nccs[i]);
                            break;
                        }
                    }
                } catch (NotFoundException e1) {
                    e1.printStackTrace();
                }

            }
        }
        return mainMethod;
    }


    /**
     * insert log to get the value of every accessible variable
     *
     * @param varsList   cluster of vars of line
     * @param mainMethod the method get from bytecode file
     */
    private void logVarValue(List<LineAccessVars> varsList, CtBehavior mainMethod) {

        for (LineAccessVars accessVars : varsList) {
            try {
                mainMethod.insertAt(accessVars.getLocation(), lo.getLineDivider());
                for (MyExp var : accessVars.getVarsList()) {
                    try {
                        mainMethod.insertAt(accessVars.getLocation(), lo.logValStatement(var.getExpVar()));
                    } catch (CannotCompileException e) {
                        mainMethod.insertAt(accessVars.getLocation(), lo.logNInitStatement(var.getExpVar()));
                        System.err.println("CannotCompileException: location:" + accessVars.getLocation() + "var:" + var);
                    }

                }
            } catch (CannotCompileException e) {
                System.err.println("location:" + accessVars.getLocation() + "vars:" + accessVars.getVarsList());
                e.printStackTrace();
            }
        }
    }

    /**
     * insert log to get the value of every accessible variable
     *
     * @param varsList   cluster of vars of line
     * @param mainMethod the method get from bytecode file
     */
    private void logNestedCVarValue(List<LineAccessVars> varsList, CtBehavior mainMethod) {
        //for very 'line' in the method
        for (LineAccessVars accessVars : varsList) {
            try {
                mainMethod.insertAt(accessVars.getLocation(), lo.getLineDivider());
                //for every var that is accessible in the line
                for (MyExp var : accessVars.getVarsList()) {

                    try {
                        mainMethod.insertAt(accessVars.getLocation(), lo.logConStatement(var.getExpVar(), tf.getQualifyFileName()));
                    } catch (CannotCompileException e) {
                        try {
                            mainMethod.insertAt(accessVars.getLocation(), lo.logValStatement(var.getExpVar()));
                        } catch (CannotCompileException e1) {
                            mainMethod.insertAt(accessVars.getLocation(), lo.logNInitStatement(var.getExpVar()));
                            System.err.println(" Nested CannotCompileException: location:"
                                    + accessVars.getLocation() + "var:"
                                    + "." + var.getExpVar());
                        }
                    }
                }
            } catch (CannotCompileException e) {
                System.err.println("Nested location:" + accessVars.getLocation());
                e.printStackTrace();
            }
        }
    }


}

