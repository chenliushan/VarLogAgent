import process.MyClassFileTransformer;

import java.lang.instrument.Instrumentation;

/**
 * Created by liushanchen on 16/5/24.
 */
public class VarLogAgent {
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) throws Exception {
        instrumentation = inst;
        System.out.println("args is: " + args);
        String[] rootPackages = args.split(",");
        for (int i = 0; i < rootPackages.length; i++) {
            if (rootPackages[i].length() > 0 && rootPackages[i] != null) {
                System.out.println(rootPackages[i]);
                instrumentation.addTransformer(new MyClassFileTransformer(rootPackages[i]));

            }
        }
    }

}
