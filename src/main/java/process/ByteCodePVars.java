package process;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import polyu_af.models.*;
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
        /*read the accessible vars from the json file*/
        this.tfList = FileUtils.json2TfList();
        System.out.println("First step tmp output is read ...");
    }

    /**
     * get the accessible vars of the class to be transform
     * and set the TargetFile tf accroding to the current className
     */
    private TargetFile getCurrentTF(String className) {
        if (tfList != null) {
            for (TargetFile tf : tfList) {
                if (tf.getDirAndFileName().equals(className)) {
                    System.out.println("className:" + tf.getDirAndFileName() + " is modified ...");
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
                forMethods(tf.getMyMethodAccessVars());
                b = cc.toBytecode();
//                cc.writeFile();
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
//            ctClass.writeFile();

        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    private void forMethods(List<MyMethod> accessVar4MethodList) {
        for (MyMethod methodAccessVar : accessVar4MethodList) {
            isNestedMethod = false;
            //find the method in the target bytecode file
            CtBehavior mainMethod = findTMethod(methodAccessVar);
            //insert log to get the value of every accessible variable
            if (mainMethod == null) {
                System.err.println("mainMethod==null  did not get the method name: " + methodAccessVar.getMethodName() + "!!!!!!");
                continue;
            } else {
                if (!isNestedMethod) {
                    logVarValue(methodAccessVar.getLineVarsList(), mainMethod);

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
    private CtBehavior findTMethod(MyMethod methodAccessVar) {
        CtBehavior mainMethod = null;
//        System.out.println("inside findTMethod ...");

        List<CtClass> ps = methodAccessVar.getParams(poolParent);
        CtClass[] prams = ps.toArray(new CtClass[ps.size()]);
//        System.out.println(" getParams ...");

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
                System.out.println(" method: " + methodAccessVar.getMethodName() + "is not found. May be in the nested class.");
//                processNestedMethod(methodAccessVar,prams);
            }
        }
//        System.out.println(" end find method ...");

        return mainMethod;
    }

    /**
     * find the target method in the nested class and insert log in that method
     *
     * @param methodAccessVar
     * @param prams
     */
    private void processNestedMethod(MyMethod methodAccessVar, CtClass[] prams) {
        CtBehavior mainMethod = null;
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
                    logNestedCVarValue(methodAccessVar.getLineVarsList(), mainMethod);
                    rewrite(nccs[i]);
                    break;
                }
            }
        } catch (NotFoundException e1) {
            e1.printStackTrace();
        }

    }


    /**
     * insert log to get the value of every accessible variable
     *  @param varsList   cluster of vars of line
     * @param mainMethod the method get from bytecode file
     */
    private void logVarValue(List<LineVars> varsList, CtBehavior mainMethod) {
        String qname = getMethodQualifyName(mainMethod);
        for (LineVars accessVars : varsList) {
//            System.out.println("for LineVars ..."+accessVars);
            int location = accessVars.getLocation();
            try {
                mainMethod.insertAt(location, lo.endLine(location));
                for (MyExp var : accessVars.getVarsList()) {
//                    System.out.println("for MyExpString" );

                    String log = lo.logValNotNullStatement(var.getExpVar());
//                    System.out.println("log ..." + log);

                    try {
                        mainMethod.insertAt(location, log);
                    } catch (CannotCompileException e) {
                        System.err.println("CannotCompileException: location:" + location + "var:" + log);
                        log = lo.logNInitStatement(var.getExpVar());
                        try {
                            mainMethod.insertAt(location, log);
                        } catch (CannotCompileException e1) {
                            System.err.println("CannotCompileException: location:" + location + "log:" + log);
                            e1.printStackTrace();
                        }
                    }
                }
//                System.out.println("end for MyExpString ..."+accessVars);
                mainMethod.insertAt(location, lo.startLine(location, qname));
            } catch (CannotCompileException e) {
                System.err.println("location:" + location + "log:" + lo.startLine(location, qname) + "||" + lo.endLine(location));
                e.printStackTrace();
            }
        }
//        System.out.println("end logVarValue...");

    }


    /**
     * insert log to get the value of every accessible variable
     *
     * @param varsList   cluster of vars of line
     * @param mainMethod the method get from bytecode file
     */
    private void logNestedCVarValue(List<LineVars> varsList, CtBehavior mainMethod) {
        //for very 'line' in the method
        for (LineVars accessVars : varsList) {
//            System.out.println("for nested LineVars ...");
            int location = accessVars.getLocation();
            try {
                mainMethod.insertAt(location, lo.endLine(location));
                //for every var that is accessible in the line
                for (MyExp var : accessVars.getVarsList()) {
//                    System.out.println("for nested MyExpString ...");
                    String log = lo.logConStatement(var.getExpVar(), tf.getQualifyFileName());
                    try {
                        mainMethod.insertAt(location, log);
                    } catch (CannotCompileException e) {
                        System.err.println("Nested CannotCompileException: location:" + location + "var:" + log);
                        log = lo.logValStatement(var.getExpVar());
                        try {
                            mainMethod.insertAt(location, log);
                        } catch (CannotCompileException e1) {

                            System.err.println(" Nested CannotCompileException: location:"
                                    + location + "var:" + "." + log);
                            log = lo.logNInitStatement(var.getExpVar());
                            mainMethod.insertAt(location, log);
                        }
                    }
                }
            } catch (CannotCompileException e) {
                System.err.println("Nested location:" + location);
                e.printStackTrace();
            }
        }
//        System.out.println("end logNestedCVarValue...");

    }

    /**
     * getMethodQualifyName
     *
     * @param method
     * @return
     */
    private String getMethodQualifyName(CtBehavior method) {
        StringBuilder sb = new StringBuilder(method.getDeclaringClass().getName());
        sb.append("#");
        sb.append(method.getName());
        sb.append("(");
        try {
            CtClass[] methodParameterTypes = method.getParameterTypes();
            for (int i = 0; i < methodParameterTypes.length; i++) {
                sb.append(methodParameterTypes[i].getName());
                sb.append(",");
            }
            if (sb.toString().endsWith(",")) {
                sb.deleteCharAt(sb.length() - 1);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        sb.append(")");
        return sb.toString();
    }

}

