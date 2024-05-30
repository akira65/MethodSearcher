/*
 *  Copyright 2024
 *  Software Science and Technology Lab., Ritsumeikan University
 */

 package method_sarcher;

 import org.jtool.jxplatform.builder.ModelBuilder;
 import org.jtool.srcmodel.JavaProject;
 import org.jtool.srcmodel.JavaMethod;
 import org.jtool.cfg.CCFG;
 import org.jtool.cfg.CFG;
 
 import java.util.List;
 
 public class CalleeMethod {
     private JavaMethod callingMethod;
     private JavaMethod targetMethod;
     
     private List<ProjectVariable> inVariables;
     private List<ProjectVariable> outVariables;
     
     public CalleeMethod(MethodSeq methodSeq) {
         callingMethod = methodSeq.caller();
         targetMethod = methodSeq.callee();
         
         JavaProject targetProject = targetMethod.getJavaProject();
         ModelBuilder builder = targetProject.getModelBuilder();
         CCFG ccfg = builder.getCCFG(targetMethod.getDeclaringClass());
         CFG cfg = ccfg.getCFG(targetMethod.getQualifiedName().fqn());
         
         VariableFinder variableFinder = new VariableFinder(targetProject, targetMethod, cfg);
         this.inVariables = variableFinder.getInVariables();
         this.outVariables = variableFinder.getOutVariables();
     }
     
     public JavaMethod getCallingMethod() {
         return callingMethod;
     }
     
     public JavaMethod getTargetMethod() {
         return targetMethod;
     }
     
     public List<ProjectVariable> getInVariables() {
         return inVariables;
     }
     
     public List<ProjectVariable> getOutVariables() {
         return outVariables;
     }
 }
 