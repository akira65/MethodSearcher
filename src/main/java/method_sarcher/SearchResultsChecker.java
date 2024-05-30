/*
 *  Copyright 2024
 *  Software Science and Technology Lab., Ritsumeikan University
 */

package method_sarcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jtool.jxplatform.builder.ModelBuilder;
import org.jtool.jxplatform.builder.ModelBuilderBatch;
import org.jtool.srcmodel.JavaClass;
import org.jtool.srcmodel.JavaMethod;
import org.jtool.srcmodel.JavaProject;

public class SearchResultsChecker {
    
    private final static int DEFAULT_MIN_LOC = 1;
    private final static int CALLING_LIMIT = 1;
    
    private void run(String name, String target) {
        run(name, target, DEFAULT_MIN_LOC);
    }
    
    private void run(String name, String target, int loc) {
        ModelBuilder builder = new ModelBuilderBatch();
        builder.analyzeBytecode(false);
        builder.useCache(true);
        builder.setConsoleVisible(true);
        
        List<JavaProject> targetProjects = builder.build(name, target);
        for (JavaProject jproject : targetProjects) {
            System.out.println("PROJECT: " + jproject.getName());
            
            MethodFinder methodFinder = new MethodFinder();
            methodFinder.run(jproject, loc, CALLING_LIMIT);
            List<JavaMethod> allMethods = methodFinder.getAllMethods();
            List<MethodSeq> targetMethodSeqList = methodFinder.getTargetMethodSeqList();
            
            List<CalleeMethod> targets = new ArrayList<>();
            int num = 1;
            for (MethodSeq seq : targetMethodSeqList) {
            
                CalleeMethod calleeMethod = new CalleeMethod(seq);
                
                // System.out.println("-Checking(" + num + "/" + targetMethodSeqList.size() + ") " +
                // calleeMethod.getTargetMethod().getQualifiedName().fqn());
                boolean result = check(jproject, calleeMethod);
                if (result) {
                    targets.add(calleeMethod);
                }
                num++;
            }
            
            System.out.println();
            System.out.println("# Found Method Call Sequences = " + allMethods.size());
            System.out.println("# Valid Target Methods = " + targets.size());
            System.out.println();
            
            // System.out.println("# Test Target Method List:");
            // for (CalleeMethod testTarget : testTargets) {
            //     System.out.println("  -TEST METHOD = " + testTarget.getTestMethod().getQualifiedName().fqn());
            //     System.out.println("  -CALLING METHOD = " + testTarget.getCallingMethod().getQualifiedName().fqn());
            //     System.out.println("  -TARGET METHOD = " + testTarget.getTargetMethod().getQualifiedName().fqn());
            //     List<ProjectVariable> inVariables = testTarget.getInVariables();
            //     List<ProjectVariable> outVariables = testTarget.getOutVariables();
            //     inVariables.forEach(v -> System.out.println("    IN = " + v.getNameForPrint()));
            //     outVariables.forEach(v -> System.out.println("    OUT = " + v.getNameForPrint()));
            // }
            try {
                File file = new File("../" + jproject.getName() + "_methods.txt");
                FileWriter filewriter = new FileWriter(file);
                for (CalleeMethod method : targets) {
                    filewriter.write("[\ncallingMethod: " + method.getCallingMethod().getQualifiedName().fqn() + "\n");
                    filewriter.write("calleeMethod: " + method.getTargetMethod().getQualifiedName().fqn() + "\n]\n");
                }

                filewriter.close();
            } catch (IOException e) {
                System.out.println(e);
            }
            
        }
        
        builder.unbuild();
    }
    
    private boolean check(JavaProject jproject, CalleeMethod testTarget) {
        String targetClassName = testTarget.getTargetMethod().getDeclaringClass().getQualifiedName().fqn();
        JavaClass targetClass = jproject.getClass(targetClassName);
        if (targetClass == null) {
            // System.err.println("**** Not found target class: " + targetClassName);
            return false;
        }
        
        String targetMethodSig = testTarget.getTargetMethod().getSignature();
        JavaMethod targetMethod = targetClass.getMethod(targetMethodSig);
        if (targetMethod == null) {
            // System.err.println("**** Not found target method: " + targetMethodSig + " in " + targetClassName);
            return false;
        }
        
        if (testTarget.getInVariables().size() == 0) {
            // System.err.println("**** Not found input variable: " + targetMethodSig + " in " + targetClassName);
            return false;
        }
        
        if (testTarget.getOutVariables().size() == 0) {
            // System.err.println("**** Not found output variable: " + targetMethodSig + " in " + targetClassName);
            return false;
        }
        
        return true;
    }
    
    public static void main(String[] args) {
        SearchResultsChecker checker = new SearchResultsChecker();
        checker.run(args[0], args[1]);
    }
}
