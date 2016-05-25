package process;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class MyClassFileTransformer implements ClassFileTransformer {

    private String filterString;
    private ByteCodePVars byteCodePVars;

    public MyClassFileTransformer(String fs) {
        this.filterString = fs;
        this.byteCodePVars = new ByteCodePVars();

    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (filterString != null && className.contains(filterString)) {
            System.out.println("className:" + className + " is modified ...");
            return byteCodePVars.transformClass(classfileBuffer,className);
        }
        return classfileBuffer;

    }
}