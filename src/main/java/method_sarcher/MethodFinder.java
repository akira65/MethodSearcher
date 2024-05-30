package method_sarcher;

import org.jtool.srcmodel.JavaProject;
import org.jtool.srcmodel.JavaClass;
import org.jtool.srcmodel.JavaMethod;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Name;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.*;

public class MethodFinder {

    private static final boolean nonPublicFlag = true;

    private List<JavaMethod> allMethods = new ArrayList<JavaMethod>();
    private List<MethodSeq> methodSeqList = new ArrayList<MethodSeq>();
    private List<MethodSeq> targetMethodSeqList = new ArrayList<MethodSeq>();

    int i = 0;
    public void run(JavaProject jproject, int loc, int callChainNum) {
        for (JavaClass jc : jproject.getClasses()) {
            for (JavaMethod jm : jc.getMethods()) {
                if (!MethodFinder.isTestMethod(jm)){
                    allMethods.add(jm);
                }
            }
        }

        List<JavaMethod> methodList = new ArrayList<JavaMethod>();
        for (JavaMethod jm : allMethods) {
            collectTargetCandidates(jm, methodList, callChainNum - 1);
        }

        for (JavaMethod jmethod : methodList) {
            collectCallingMethodSeqList(jmethod, new ArrayList<JavaMethod>(), callChainNum);
        }
        
        List<MethodSeq> seqList = getListOfUniqueMethodSeq(methodSeqList);

        System.out.println(i);
        System.out.println(seqList.size());
        for (MethodSeq seq : seqList) {
            int loc1 = MethodFinder.getLoc(seq.caller());
            if (loc1 > loc) {
                targetMethodSeqList.add(seq);
            }
        }
    }

    public List<JavaMethod> getAllMethods() {
        return allMethods;
    }
    
    public List<MethodSeq> getTargetMethodSeqList() {
        return targetMethodSeqList;
    }

    private boolean isTarget(JavaMethod jmethod) {
        return !MethodFinder.isTestMethod(jmethod) &&
                !jmethod.isConstructor() &&
                jmethod.getDeclaringClass().getTypeBinding().isTopLevel() &&
                jmethod.isInProject() &&
                (jmethod.isPublic() || nonPublicFlag);
    }


    private void collectTargetCandidates(JavaMethod jmethod, List<JavaMethod> calledMethods, int count) {
        for (JavaMethod jm : jmethod.getCalledMethodsInProject()) {
            if (count == 0) {  
                if (isTarget(jm)) {
                    if (!calledMethods.contains(jm)) {
                        calledMethods.add(jm);
                    }
                }
            } else {
                collectTargetCandidates(jm, calledMethods, count - 1);
            }
        }
    }

    private static boolean isTestMethod(JavaMethod jmethod) {
        if (jmethod.getDeclaringClass() == null) {
            return false;
        }

        Pattern p = Pattern.compile("Test");
        Pattern q = Pattern.compile("test");
    
        if (p.matcher(jmethod.getQualifiedName().fqn()).find() || q.matcher(jmethod.getQualifiedName().fqn()).find()) {
            return true;
        }

        JavaClass jclass = jmethod.getDeclaringClass().getSuperClass();
        if (jclass != null) {
            if (jclass.getQualifiedName().fqn().equals("junit.framework.TestCase")) {
                return true;
            }
        }

        String anno = MethodFinder.getAnnotation(jmethod);
        return "Test".equals(anno);
    }
    // private void collectCallingMethodSeqList(JavaMethod jmethod, List<JavaMethod> callingMethods, int count) {
    //     if (callingMethods.contains(jmethod)) {
    //         return;
    //     }
        
    //     callingMethods.add(jmethod);

    //     for (JavaMethod jm : jmethod.getCallingMethodsInProject()) {
    //         System.out.println("lkdgja;kjgdlakjgkljldsakjglkja;l");
    //         System.out.println(jmethod.getQualifiedName().fqn() + ":    " + jm.getQualifiedName().fqn());
    //         if (count == 0) {
    //             MethodSeq seq = new MethodSeq(callingMethods);
    //             methodSeqList.add(seq);
    //         }
    //         collectCallingMethodSeqList(jm, new ArrayList<JavaMethod>(callingMethods), count - 1);
    //     }
    // }

    private void collectCallingMethodSeqList(JavaMethod jmethod, List<JavaMethod> callingMethods, int count) {
        i++;
        if (callingMethods.contains(jmethod) || MethodFinder.isTestMethod(jmethod)) {
            return;
        }
        
        callingMethods.add(jmethod);
        
        if (count == 0) {
            MethodSeq seq = new MethodSeq(callingMethods);
            methodSeqList.add(seq);
            return;
        }
        
        for (JavaMethod jm : jmethod.getCallingMethodsInProject()) {
            collectCallingMethodSeqList(jm, new ArrayList<JavaMethod>(callingMethods), count - 1);
        }
    }

    public static String getAnnotation(JavaMethod jmethod) {
        ASTNode node = jmethod.getASTNode();
        if (node instanceof MethodDeclaration) {
            MethodDeclaration methodDecl = (MethodDeclaration)node;
            for (Object obj : methodDecl.modifiers()) {
                IExtendedModifier mod = (IExtendedModifier)obj;
                if (mod.isAnnotation()) {
                    Annotation anno = (Annotation)mod;
                    Name name = anno.getTypeName();
                    return name.getFullyQualifiedName();
                    
                }
            }
        }
        return "";
    }

    private static int getLoc(JavaMethod jmethod) {
        if (jmethod.getASTNode() instanceof MethodDeclaration) {
            MethodDeclaration methodDecl = (MethodDeclaration)jmethod.getASTNode();
            CompilationUnit cu = (CompilationUnit)methodDecl.getRoot();
            
            int startPosition = methodDecl.getStartPosition();
            int endPosition = methodDecl.getStartPosition() + methodDecl.getLength() - 1;
            int upperLineNumber = cu.getLineNumber(startPosition);
            int bottomLineNumber = cu.getLineNumber(endPosition);
            
            int docupperLineNumber = 0;
            int docbottomLineNumber = 0;
            Javadoc javadoc = methodDecl.getJavadoc();
            if (javadoc != null) {
                int docstartPosition = javadoc.getStartPosition();
                int docendPosition = javadoc.getStartPosition() + javadoc.getLength() - 1;
                docupperLineNumber = cu.getLineNumber(docstartPosition);
                docbottomLineNumber = cu.getLineNumber(docendPosition);
            }
            
            int loc = (bottomLineNumber - upperLineNumber) - (docbottomLineNumber - docupperLineNumber);
            
            return loc;
        }
        return 0;
    }

    private List<MethodSeq> getListOfUniqueMethodSeq(List<MethodSeq> seqList) {
        Map<String, MethodSeq> methods = new HashMap<String, MethodSeq>();
        for (MethodSeq seq: seqList) {
            methods.put(seq.getName(), seq);
        }
        
        List<MethodSeq> ret = new ArrayList<MethodSeq>(methods.values());
        MethodSeq.sort(ret);
        return ret;
    }
}
