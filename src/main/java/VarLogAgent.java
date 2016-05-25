import process.MyClassFileTransformer;

import java.lang.instrument.Instrumentation;

/**
 * Created by liushanchen on 16/5/24.
 */
public class VarLogAgent {
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) throws Exception {

        System.out.println("args is: "+args);

        instrumentation = inst;
        instrumentation.addTransformer(new MyClassFileTransformer(args));

    }

}
