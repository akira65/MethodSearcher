/*
 *  Copyright 2024
 *  Software Science and Technology Lab., Ritsumeikan University
 */

 package method_sarcher;

 import org.jtool.srcmodel.JavaMethod;
 
 import java.util.List;
 import java.util.stream.Collectors;
 import java.util.Comparator;
 
 class MethodSeq {
     private List<JavaMethod> methodChain;
     
     MethodSeq(List<JavaMethod> methodChain) {
         this.methodChain = methodChain;
     }
     
     public List<JavaMethod> methodChain1() {
         return methodChain;
     }
     
     public JavaMethod caller() {
         return methodChain.get(methodChain.size() - 1);
     }
     
     public JavaMethod callee() {
         return methodChain.get(0);
     }
     
     public String getName() {
         return callee().getQualifiedName().fqn();
     }
     
     public String getClassName() {
         return callee().getDeclaringClass().getQualifiedName().fqn();
     }
     
     public String getMethodName() {
         return callee().getName();
     }
     
     public String getMethodSig() {
         return callee().getSignature();
     }
     
     public static void sort(List<MethodSeq> methods) {
         methods = methods.stream().sorted(Comparator.comparing(m -> m.getName())).collect(Collectors.toList());
     }
 }
 