/*
 *  Copyright 2024
 *  Software Science and Technology Lab., Ritsumeikan University
 */

package method_searcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jtool.jxplatform.builder.ModelBuilder;
import org.jtool.jxplatform.builder.ModelBuilderBatch;
import org.jtool.srcmodel.JavaClass;
import org.jtool.srcmodel.JavaMethod;
import org.jtool.srcmodel.JavaProject;

public class SearchResultsChecker {
    
    private final static int DEFAULT_MIN_LOC = 1;
    private final static int CALLING_LIMIT = 1;
    private final static String[] primitiveTypes = {
        "java.lang.String",
        "string",
        "char",
        "java.lang.Byte",
        "byte[]",
        "byte",
        "java.lang.Short",
        "short",
        "java.lang.Number",
        "java.lang.Integer",
        "integer",
        "int",
        "java.lang.Long",
        "long",
        "java.lang.Float",
        "float",
        "java.lang.Double",
        "double",
        "java.lang.Boolean",
        "boolean",

     };
    
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
            FailedMethods failedMethods = new FailedMethods(); 

            for (MethodSeq seq : targetMethodSeqList) {
                CalleeMethod calleeMethod = new CalleeMethod(seq);
                boolean result = check(jproject, calleeMethod, failedMethods);
                if (result) {
                    targets.add(calleeMethod);
                }
            }
            
            System.out.println();
            System.out.println("# Found Method Call Sequences = " + allMethods.size());
            System.out.println("# Found Method Pair Of Caller And Callee = " + targetMethodSeqList.size());
            System.out.println("# Valid Method Pairs Of Caller And Callee = " + targets.size());
            System.out.println();

            Path outputFile = Paths.get(target, "../../" + jproject.getName() + "_methods.txt");
        
            try {
                // ファイルを作成（存在しない場合のみ）
                if (!Files.exists(outputFile)) {
                    Files.createFile(outputFile);
                }

                // File オブジェクトを Path オブジェクトから取得
                File file = outputFile.toFile();
                FileWriter filewriter = new FileWriter(file);
                
                List<String> targetMethods = new ArrayList<String>();
                for (CalleeMethod method : targets) {
                    if (!targetMethods.contains(method.getTargetMethod().getQualifiedName().fqn())) {
                        targetMethods.add(method.getTargetMethod().getQualifiedName().fqn());
                    }
                }
                
                filewriter.write("# Number Of Methods = " + allMethods.size() + "\n");
                filewriter.write("# Number Of Classes = " + methodFinder.getClassNum(jproject) + "\n");
                filewriter.write("# Line Of Code = " + methodFinder.getAllMethodLoc() + "\n");
                filewriter.write("# Target Methods = " + targetMethods.size() + "\n");
                filewriter.write("# All Method Pairs = " + targetMethodSeqList.size() + "\n");
                filewriter.write("# Valid Method Pairs = " + targets.size() + "\n");
                filewriter.write("# Not found target class = " + failedMethods.getNotFoundClasses().size() + "\n");
                filewriter.write("# Not found target method = " + failedMethods.getNotFoundMethods().size() + "\n");
                filewriter.write("# Not found input variable = " + failedMethods.getNotFoundInputVariables().size() + "\n");
                filewriter.write("# Not found output variable = " + failedMethods.getNotFoundOutputVariables().size() + "\n");
                filewriter.write("# Input variable is not Object = " + failedMethods.getIsNotObjectInput().size() + "\n");
                filewriter.write("# Output variable is not Primitive = " + failedMethods.getIsNotPrimitiveOutput().size() + "\n\n");
                // ファイルにメソッド情報を書き込む
                for (CalleeMethod method : targets) {
                    filewriter.write("[\ncallerMethod: " + method.getCallerMethod().getQualifiedName().fqn() + "\n");
                    filewriter.write("calleeMethod: " + method.getTargetMethod().getQualifiedName().fqn() + "\n]\n");
                }

                filewriter.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        
        builder.unbuild();
    }
    
    private boolean check(JavaProject jproject, CalleeMethod testTarget, FailedMethods failedMethods) {
        String targetClassName = testTarget.getTargetMethod().getDeclaringClass().getQualifiedName().fqn();
        JavaClass targetClass = jproject.getClass(targetClassName);
        if (targetClass == null) {
            System.err.println("**** Not found target class: " + targetClassName);
            failedMethods.addNotFoundClasse(targetClassName);
            return false;
        }
        
        String targetMethodSig = testTarget.getTargetMethod().getSignature();
        JavaMethod targetMethod = targetClass.getMethod(targetMethodSig);
        if (targetMethod == null) {
            // System.err.println("**** Not found target method: " + targetMethodSig + " in " + targetClassName);
            failedMethods.addNotFoundMethod(targetMethodSig);
            return false;
        }
        
        if (testTarget.getInVariables().isEmpty()) {
            // System.err.println("**** Not found input variable: " + targetMethodSig + " in " + targetClassName);
            failedMethods.addNotFoundInputVariable(targetClassName);
            return false;
        }
        
        if (testTarget.getOutVariables().isEmpty()) {
            // System.err.println("**** Not found output variable: " + targetMethodSig + " in " + targetClassName);
            failedMethods.addNotFoundOutputVariable(targetClassName);
            return false;
        }

        if (!isObjectInput(testTarget)) {
            // System.err.println("**** Input variable is not Object: " + targetMethodSig + " in " + targetClassName);
            failedMethods.addIsNotObjectInput(targetMethodSig);
            return false;
        }

        if (!isPrimitiveOutput(testTarget)) {
            // System.err.println("**** Output variable is not Primitive: " + targetMethodSig + " in " + targetClassName);
            failedMethods.addIsNotPrimitiveOutput(targetMethodSig);
            return false;
        }

        return true;
    }

    private boolean isObjectInput(CalleeMethod testTarget) {
        Pattern pattern = Pattern.compile("\\((.*?)\\)");

        boolean flag = true;

        for (ProjectVariable var : testTarget.getInVariables()) {
            
            Matcher matcher = pattern.matcher(var.toString());
            if (matcher.find()){
                String argumentTypes = matcher.group(1);
                String[] types = argumentTypes.split("\\s+");
                
                for (String type : types) {
                    for (String primitiveType : primitiveTypes) {
                        if (primitiveType.contains(type)) {
                            flag = false;
                        }
                    } 
                    if (flag == true) {
                        return true;
                    }else {
                        flag = true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean isPrimitiveOutput(CalleeMethod testTarget) {
        Pattern pattern = Pattern.compile("@(\\S+)");

        for (ProjectVariable var : testTarget.getOutVariables()) {
            Matcher matcher = pattern.matcher(var.toString());
            if (matcher.find()){
                String argumentType = matcher.group(1);
                for (String primitiveType : primitiveTypes) {
                    if (argumentType.contains(primitiveType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public static void main(String[] args) {
        SearchResultsChecker checker = new SearchResultsChecker();
        checker.run(args[0], args[1]);
    }
}
