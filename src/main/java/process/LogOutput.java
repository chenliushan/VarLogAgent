package process;

/**
 * Created by liushanchen on 16/5/23.
 */
public class LogOutput {

    private String loggerName = "afToolLogger";

    public LogOutput() {
    }

    public LogOutput(String loggerName) {
        this.loggerName = loggerName;
    }

    public String[] getImportPackages() {
        return new String[]{"org.apache.logging.log4j.LogManager", "org.apache.logging.log4j.Logger"};
    }

    public String getDeclaration() {
        return "public static Logger " + loggerName + " =org.apache.logging.log4j.LogManager#getLogger(\"" + VarLogConstants.VarLogName + "\");";
    }

    public String startLine(int lineNum, String qName) {
        return loggerName + ".info(\"" + VarLogConstants.lineStart + qName + ":" + lineNum + "\");";
    }

    public String endLine(int lineNum) {
        return loggerName + ".info(\"" + VarLogConstants.lineEnd + lineNum + "\");";
    }

    public String getMethodName(String methodName) {
        return loggerName + ".info(\"" + methodName + "\");";
    }

    public String logValStatement(String varName) {
        String varNameString=varName;
        if(varName.contains("\"")){
            varNameString= varName.replace("\"","");
        }
        StringBuilder sb=new StringBuilder(loggerName);
        sb.append(".info(\"" );
        sb.append(varNameString);
        sb.append(":\"+(");
        sb.append(varName);
        sb.append( "));");
        return sb.toString();
//        return loggerName + ".info(\"" + varName + ":\"+(" + varName + "));";
    }

    public String logValNotNullStatement(String varName) {
        StringBuilder sb = new StringBuilder("if(n!=null){");
        sb.append(logValStatement(varName));
        sb.append("}");
        return sb.toString();
//        return loggerName+".info(\"" + varName + ":\"+(" + getFirstVar(varName) + "==null? \"null\" :"+varName+"));";
    }

    private String getFirstVar(String varName) {
        int index = varName.indexOf(".");
        if (index > 0) {
            return varName.substring(0, index);
        }
        return varName;
    }

    public String logNInitStatement(String varName) {
        return loggerName + ".info(\"" + varName + ":\"+" + "\": may not initialized.\");";
    }

    public String logConStatement(String varName, String targetClass) {
        return loggerName + ".info(\"" + targetClass + "." + varName + ":\"+(" + targetClass + "." + varName + "));";
    }
}
