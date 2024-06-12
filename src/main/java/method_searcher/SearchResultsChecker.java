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
        "java.lang.Integer",
        "integer",
        "int",
        "java.lang.Long",
        "long",
        "java.lang.Float",
        "float",
        "java.lang.Double",
        "double",
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
            int num = 1;
            for (MethodSeq seq : targetMethodSeqList) {
            
                CalleeMethod calleeMethod = new CalleeMethod(seq);
                
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

            Path p = Paths.get(target, "../../" + jproject.getName() + "_methods.txt");
        
            try {
                // ファイルを作成（存在しない場合のみ）
                if (!Files.exists(p)) {
                    Files.createFile(p);
                }

                // File オブジェクトを Path オブジェクトから取得
                File file = p.toFile();
                FileWriter filewriter = new FileWriter(file);

                filewriter.write("# Valid Callee Methods = " + targets.size());
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
    
    private boolean check(JavaProject jproject, CalleeMethod testTarget) {
        String targetClassName = testTarget.getTargetMethod().getDeclaringClass().getQualifiedName().fqn();
        JavaClass targetClass = jproject.getClass(targetClassName);
        if (targetClass == null) {
            System.err.println("**** Not found target class: " + targetClassName);
            return false;
        }
        
        String targetMethodSig = testTarget.getTargetMethod().getSignature();
        JavaMethod targetMethod = targetClass.getMethod(targetMethodSig);
        if (targetMethod == null) {
            System.err.println("**** Not found target method: " + targetMethodSig + " in " + targetClassName);
            return false;
        }
        
        if (testTarget.getInVariables().isEmpty()) {
            System.err.println("**** Not found input variable: " + targetMethodSig + " in " + targetClassName);
            return false;
        }
        
        if (testTarget.getOutVariables().isEmpty()) {
            System.err.println("**** Not found output variable: " + targetMethodSig + " in " + targetClassName);
            return false;
        }

        if (!isObjectInput(testTarget)) {
            System.err.println("**** Input variable is not Object: " + targetMethodSig + " in " + targetClassName);
            return false;
        }

        if (!isPrimitiveOutput(testTarget)) {
            System.err.println("**** Output variable is not Primitive: " + targetMethodSig + " in " + targetClassName);
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
        Pattern pattern = Pattern.compile("\\(\\s*(.*?)\\s*\\)");

        for (ProjectVariable var : testTarget.getInVariables()) {
            Matcher matcher = pattern.matcher(var.toString());
            if (matcher.find()){
                String argumentTypes = matcher.group(1);
                for (String primitiveType : primitiveTypes) {
                    if (argumentTypes.contains(primitiveType)) {
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
