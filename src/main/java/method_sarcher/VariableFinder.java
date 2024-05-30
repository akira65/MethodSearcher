/*
 *  Copyright 2024
 *  Software Science and Technology Lab., Ritsumeikan University
 */

package method_sarcher;

import org.jtool.srcmodel.JavaClass;
import org.jtool.srcmodel.JavaField;
import org.jtool.srcmodel.JavaMethod;
import org.jtool.srcmodel.JavaProject;
import org.jtool.srcmodel.QualifiedName;
import org.checkerframework.checker.units.qual.radians;
import org.jtool.cfg.CFG;
import org.jtool.cfg.CFGNode;
import org.jtool.cfg.CFGStatement;
import org.jtool.cfg.CFGParameter;
import org.jtool.cfg.JVariableReference;
import org.jtool.cfg.JFieldReference;

import method_sarcher.ProjectVariable.Direction;
import method_sarcher.ProjectVariable.Sort;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class VariableFinder {
    
    private Set<ProjectVariable> inVariables = new HashSet<>();
    private Set<ProjectVariable> outVariables = new HashSet<>();
    
    public VariableFinder(JavaProject targetProject, JavaMethod targetMethod, CFG cfg) {
        collectInVariables(targetProject, targetMethod, cfg);
        collectOutVariables(targetProject, targetMethod, cfg);
    }
    
    private void collectInVariables(JavaProject targetProject, JavaMethod targetMethod, CFG cfg) {
        Set<String> names = new HashSet<>();
        if (!targetMethod.isStatic()) {
            collectParametersAndFields(targetProject, targetMethod, cfg, names);
        } else {
            collectParametersOnly(cfg, names);
        }
    }
    
    private void collectParametersAndFields(JavaProject targetProject, JavaMethod targetMethod, CFG cfg,
            Set<String> names) {
        for (CFGNode node : cfg.getNodes()) {
            if (node.isStatement()) {
                if (node.isFormal()) {
                    collectInParameters(cfg, node, names);
                } else {
                    collectInFields(cfg, node, names, targetProject, targetMethod.getDeclaringClass());
                }
            }
        }
    }
    
    private void collectParametersOnly(CFG cfg, Set<String> names) {
        for (CFGNode node : cfg.getNodes()) {
            if (node.isFormal()) {
                collectInParameters(cfg, node, names);
            }
        }
    }
    
    private void collectInParameters(CFG cfg, CFGNode node, Set<String> names) {
        CFGParameter paramNode = (CFGParameter)node;
        if (paramNode.isFormalIn()) {
            JVariableReference var = paramNode.getDefVariable();
            QualifiedName qname = new QualifiedName(var.getEnclosingClassName(), var.getEnclosingMethodName());
            String name = qname.fqn() + "!" + var.getName() + "$" + String.valueOf(paramNode.getIndex() + 1);
            names.add(name);
            
            String type = var.getType();
            boolean primitive = var.isPrimitiveType();
            int modifiers = var.getModifiers();
            String access = var.getName();
            
            ProjectVariable tvar = new ProjectVariable(cfg, paramNode, var,
                Sort.PARAMETER, Direction.IN, name, type, primitive, modifiers, access, paramNode.getIndex());
            
            if (tvar.isTargetToBeRecorded()) {
                inVariables.add(tvar);
            }
        }
    }
    
    private void collectInFields(CFG cfg, CFGNode node, Set<String> names, JavaProject targetProject, JavaClass targetClass) {
        CFGStatement statement = (CFGStatement)node;
        for (JVariableReference var : statement.getUseVariables()) {
            if (var.isFieldAccess() && var.getEnclosingClassName().equals(var.getDeclaringClassName())) {
                JFieldReference fref = (JFieldReference)var;
                if (!fref.isFinal()) {
                    String qname = var.getQualifiedName().fqn();
                    if (!names.contains(qname)) {
                        names.add(qname);
                        
                        String type = var.getType();
                        boolean primitive = var.isPrimitiveType();
                        int modifiers = var.getModifiers();
                        String accessName = var.getName();
                        ProjectVariable tvar = new ProjectVariable(cfg, statement, var,
                                Sort.FIELD, Direction.IN, qname, type, primitive, modifiers, accessName);
                        tvar.setAccessor(ProjectVariable.findSetter(targetProject, targetClass, var));
                        if (tvar.isTargetToBeRecorded() && (tvar.isPublic() || tvar.getAccessor() != null)) {
                            inVariables.add(tvar);
                        }
                    }
                }
            }
        }
    }
    
    private void collectOutVariables(JavaProject targetProject, JavaMethod targetMethod, CFG cfg) {
        Set<String> names = new HashSet<>();
        if (!targetMethod.isStatic()) {
            collectReturnsAndFields(targetProject, targetMethod, cfg, names);
        } else {
            collectReturnsOnly(targetProject, cfg, names);
        }
    }
    
    private void collectReturnsAndFields(JavaProject targetProject, JavaMethod targetMethod, CFG cfg, Set<String> names) {
        for (CFGNode node : cfg.getNodes()) {
            if (node.isReturn()) {
                collectOutReturns(cfg, node, names, targetProject);
            } else if (node.isStatementNotParameter()) {
                collectOutFields(cfg, node, names, targetProject, targetMethod.getDeclaringClass());
            }
        }
    }
    
    private void collectReturnsOnly(JavaProject targetProject, CFG cfg, Set<String> names) {
        for (CFGNode node : cfg.getNodes()) {
            if (node.isReturn()) {
                collectOutReturns(cfg, node, names, targetProject);
            }
        }
    }
    
    private void collectOutReturns(CFG cfg, CFGNode node, Set<String> names, JavaProject targetProject) {
        CFGStatement statement = (CFGStatement)node;
        if (statement.getDefVariables().size() > 0) {
            JVariableReference var = statement.getDefVariables().get(0);
            String qname = var.getQualifiedName().fqn();
            if (!names.contains(qname)) {
                names.add(qname);
                
                String type = var.getType();
                boolean primitive = var.isPrimitiveType();
                int modifiers = var.getModifiers();
                int index = qname.indexOf(QualifiedName.QualifiedNameSeparator);
                int index2 = qname.indexOf('(');
                String accessName = "$" + qname.substring(index + 1, index2);
                ProjectVariable tvar = new ProjectVariable(cfg, statement, var,
                    Sort.RETURN, Direction.OUT, qname, type, primitive, modifiers, accessName);
                
                if (tvar.isTargetToBeRecorded()) {
                    outVariables.add(tvar);
                } else {
                    setEnclosingVariables(targetProject, tvar);
                    if (tvar.getEnclosingVariables().size() > 0) {
                        outVariables.add(tvar);
                    }
                }
            }
        }
    }
    
    private void collectOutFields(CFG cfg, CFGNode node, Set<String> names, JavaProject targetProject, JavaClass targetClass) {
        CFGStatement statement = (CFGStatement)node;
        for (JVariableReference var : statement.getDefVariables()) {
            if (var.isFieldAccess() && var.getEnclosingClassName().equals(var.getDeclaringClassName())) {
                String qname = var.getQualifiedName().fqn();
                if (!names.contains(qname)) {
                    names.add(qname);
                    
                    String type = var.getType();
                    boolean primitive = var.isPrimitiveType();
                    int modifiers = var.getModifiers();
                    String accessName = var.getName();
                    
                    ProjectVariable tvar = new ProjectVariable(cfg, statement, var,
                            Sort.FIELD, Direction.OUT, qname, type, primitive, modifiers, accessName);
                    
                    tvar.setAccessor(ProjectVariable.findGetter(targetProject, targetClass, var));
                    if (tvar.isPublic() || tvar.getAccessor() != null) {
                        if (tvar.isTargetToBeRecorded()) {
                            outVariables.add(tvar);
                        } else {
                            setEnclosingVariables(targetProject, tvar);
                            if (tvar.getEnclosingVariables().size() > 0) {
                                outVariables.add(tvar);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public List<ProjectVariable> getInVariables() {
        return new ArrayList<>(inVariables);
    }
    
    public List<ProjectVariable> getOutVariables() {
        return new ArrayList<>(outVariables);
    }
    
    private void setEnclosingVariables(JavaProject targetProject, ProjectVariable tvar) {
        JavaClass jc = targetProject.getClass(tvar.getRawType());
        if (jc == null) {
            jc = targetProject.getExternalClass(tvar.getRawType());
        }
        if (jc == null) {
            return;
        }
        
        for (JavaField jf : jc.getFields()) {
            boolean primitive = jf.isPrimitiveType();
            if (jf.isPublic() && (primitive || ProjectVariable.seemsPrimitive(jf.getType()))) {
                String qname = jf.getQualifiedName().fqn();
                String type = jf.getType();
                int modifiers = jf.getModifiers();
                String access = jf.getName();
                
                Sort sort = null;
                if (tvar.isField()) {
                    sort = Sort.FFIELD;
                } else {
                    sort = Sort.RFIELD;
                }
                ProjectVariable tv = new ProjectVariable(tvar.getCFG(), tvar.getCFGNode(), tvar.getVariable(),
                        sort, Direction.OUT, qname, type, primitive, modifiers, access);
                tvar.addEnclosingVariable(tv);
            }
        }
        
        for (JavaMethod jm : jc.getMethods()) {
            boolean primitive = jm.isPrimitiveReturnType();
            if (jm.isPublic() && (primitive || ProjectVariable.seemsPrimitive(jm.getReturnType()))
                    && jm.getParameterSize() == 0 && !jm.getName().equals("toString")) {
                String qname = jm.getQualifiedName().fqn();
                String type = jm.getReturnType();
                int modifiers = jm.getModifiers();
                String access = jm.getName() + "()";
                
                Sort sort = null;
                if (tvar.isField()) {
                    sort = Sort.FMETHOD;
                } else {
                    sort = Sort.RMETHOD;
                }
                ProjectVariable tv = new ProjectVariable(tvar.getCFG(), tvar.getCFGNode(), tvar.getVariable(),
                        sort, Direction.OUT, qname, type, primitive, modifiers, access);
                tvar.addEnclosingVariable(tv);
            }
        }
    }
}
