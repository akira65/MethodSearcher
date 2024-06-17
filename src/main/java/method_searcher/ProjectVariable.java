/*
 *  Copyright 2024
 *  Software Science and Technology Lab., Ritsumeikan University
 */

 package method_searcher;

 import org.jtool.srcmodel.JavaClass;
 import org.jtool.srcmodel.JavaMethod;
 import org.jtool.srcmodel.JavaProject;
 import org.jtool.srcmodel.QualifiedName;
 import org.jtool.cfg.CFG;
 import org.jtool.cfg.CFGMethodEntry;
 import org.jtool.cfg.CFGNode;
 import org.jtool.cfg.CFGStatement;
 import org.jtool.cfg.JVariableReference;
 import org.eclipse.jdt.core.dom.Modifier;
 import java.util.Set;
import java.util.HashSet;
 import java.util.List;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 
 public class ProjectVariable {
     
     private final static String simpleType[] = {
        "java.lang.String",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double"
     };
     
     public enum Sort {
         FIELD, PARAMETER, RETURN, FFIELD, RFIELD, FMETHOD, RMETHOD,
     }
     
     public enum Direction {
         IN, OUT,
     }
     
     private CFG cfg;
     private CFGNode cfgNode;
     private JVariableReference variable;
     
     private Sort sort;
     private Direction dir = null;
     private String qname;
     private String type;
     private boolean isPrimitiveType;
     protected int modifiers;
     private String access;
     private int index;
     
     private ProjectVariable parent = null;
     private Set<ProjectVariable> enclosingVariables = new HashSet<>();
     
     private JavaMethod accessor;
     
     public ProjectVariable(CFG cfg, CFGNode node, JVariableReference var,
             Sort sort, Direction dir, String qname, String type, 
             boolean primitive, int modifiers, String access) {
         this(cfg, node, var, sort, dir, qname, type, primitive, modifiers, access, -1);
     }
     
     public ProjectVariable(CFG cfg, CFGNode node, JVariableReference var,
             Sort sort, Direction dir, String qname, String type,
             boolean primitive, int modifiers, String access, int index) {
         this.cfg = cfg;
         this.cfgNode = node;
         this.variable = var;
         
         this.sort = sort;
         this.dir = dir;
         this.qname = qname;
         this.type = type;
         this.isPrimitiveType = primitive;
         this.modifiers = modifiers;
         this.access = access;
         this.index = index;
     }
     
     public CFG getCFG() {
         return cfg;
     }
     
     public CFGNode getCFGNode() {
         return cfgNode;
     }
     
     public JVariableReference getVariable() {
         return variable;
     }
     
     public Sort getSort() {
         return sort;
     }
     
     public boolean isField() {
         return sort == Sort.FIELD;
     }
     
     public boolean isParameter() {
         return sort == Sort.PARAMETER;
     }
     
     public boolean isReturn() {
         return sort == Sort.RETURN;
     }
     
     public boolean isFieldByFieldAccess() {
         return sort == Sort.FFIELD;
     }
     
     public boolean isFieldByMethodCall() {
         return sort == Sort.FMETHOD;
     }
     
     public boolean isReturnByFieldAccess() {
         return sort == Sort.RFIELD;
     }
     
     public boolean isReturnByMethodCall() {
         return sort == Sort.RMETHOD;
     }
     
     public Direction getDirection() {
         return dir;
     }
     
     public boolean isIn() {
         return dir == Direction.IN;
     }
     
     public boolean isOut() {
         return dir == Direction.OUT;
     }
     
     public String getEnclosingClassName() {
         return variable.getEnclosingClassName();
     }
     
     public String getDeclaringClassName() {
         return variable.getDeclaringClassName();
     }
     
     public String getQualifiedName() {
         if (isParameter()) {
             int index = qname.lastIndexOf('$');
             return qname.substring(0, index);
         } else {
             return qname;
         }
     }
     
     public String getName() {
         if (isParameter()) {
             int index = qname.indexOf('!');
             String name = qname.substring(index + 1);
             index = name.lastIndexOf('$');
             return name.substring(0, index);
         } else {
             int index = qname.lastIndexOf(QualifiedName.QualifiedNameSeparator);
             if (index != -1) {
                 return qname.substring(index + 1);
             } else {
                 return qname;
             }
         }
     }
     
     public String getType() {
         return type;
     }
     
     public String getRawType() {
         int index = type.indexOf('<');
         if (index != -1) {
             return type.substring(0, index);
         } else {
             return type;
         }
     }
     
     public boolean isTargetToBeRecorded() {
         if (isPrimitiveType) {
             return true;
         }
         return ProjectVariable.seemsPrimitive(type);
     }
     
     public static boolean seemsPrimitive(String type) {
         for (String ptype : simpleType) {
             if (ptype.equals(type)) {
                 return true;
             }
         }
         return false;
     }
     
     public boolean isPublic() {
         return Modifier.isPublic(modifiers);
     }
     
     public boolean isProtected() {
         return Modifier.isProtected(modifiers);
     }
     
     public boolean isDefault() {
         return Modifier.isDefault(modifiers);
     }
     
     public boolean isPrivate() {
         return Modifier.isPrivate(modifiers);
     }
     
     public String getAccess() {
         return access;
     }
     
     public ProjectVariable getParent() {
         return parent;
     }
     
     public int getIndex() {
         return index;
     }
     
     public void setAccessor(JavaMethod accessor) {
         this.accessor = accessor;
     }
     
     public JavaMethod getAccessor() {
         return accessor;
     }
     
     public void addEnclosingVariable(ProjectVariable var) {
         var.parent = this;
         enclosingVariables.add(var);
     }
     
     public Set<ProjectVariable> getEnclosingVariables() {
         return enclosingVariables;
     }
     
     @Override
     public boolean equals(Object obj) {
         if (obj instanceof ProjectVariable) {
             return equals((ProjectVariable)obj);
         }
         return false;
     }
     
     public boolean equals(ProjectVariable var) {
         if (var == null) {
             return false;
         }
         return this == var || qname.equals(var.getQualifiedName());
     }
     
     @Override
     public int hashCode() {
         return qname.hashCode();
     }
     
     @Override
     public String toString() {
         return qname + "@" + type;
     }
     
     public static JavaMethod findSetter(JavaProject targetProject, JavaClass targetClass, JVariableReference var) {
         for (JavaMethod jm : targetClass.getMethods()) {
             if (jm.isPublic() && jm.getParameterSize() == 1 && jm.getParameter(0).getType().equals(var.getType())) {
                 CFG cfg = targetProject.getModelBuilder().getCFG(jm);
                 CFGMethodEntry entry = (CFGMethodEntry)cfg.getEntryNode();
                 JVariableReference param = entry.getFormalIn(0).getDefVariable();
                 if (containsOnlyOneAssignment(cfg, var.getQualifiedName().fqn(), param.getQualifiedName().fqn())) {
                     return jm;
                 }
             }
         }
         return null;
     }
     
     private static boolean containsOnlyOneAssignment(CFG cfg, String defVarName, String useVarName) {
         int num = 0;
         for (CFGNode node : cfg.getNodes()) {
             if (node.isAssignment()) {
                 CFGStatement stnode = (CFGStatement)node;
                 if (containsSingleVarible(stnode.getDefVariables(), defVarName) &&
                         containsSingleVarible(stnode.getUseVariables(), useVarName)) {
                     num++;
                 }
             }
         }
         return num == 1;
     }
     
     public static JavaMethod findGetter(JavaProject targetProject, JavaClass targetClass, JVariableReference var) {
         for (JavaMethod jm : targetClass.getMethods()) {
             if (jm.isPublic() && jm.getParameterSize() == 0 && jm.getReturnType().equals(var.getType())) {
                 CFG cfg = targetProject.getModelBuilder().getCFG(jm);
                 if (containsOnlyOneReturnStatement(cfg, var.getQualifiedName().fqn())) {
                     return jm;
                 }
             }
         }
         return null;
     }
     
     private static boolean containsOnlyOneReturnStatement(CFG cfg, String useVarName) {
         int num = 0;
         for (CFGNode node : cfg.getNodes()) {
             if (node.isReturn()) {
                 CFGStatement stnode = (CFGStatement)node;
                 if (containsSingleVarible(stnode.getUseVariables(), useVarName)) {
                     num++;
                 }
             }
         }
         return num == 1;
     }
     
     private static boolean containsSingleVarible(List<JVariableReference> vars, String name) {
         List<JVariableReference> vs = new ArrayList<>();
         for (JVariableReference var : vars) {
             if (!var.getName().equals("this")) {
                 vs.add(var);
             }
         }
         return vs.size() == 1 && vs.get(0).getQualifiedName().fqn().equals(name);
     }
     
     public static void sort(List<? extends ProjectVariable> vars) {
         Collections.sort(vars, new Comparator<ProjectVariable>() {
             
             @Override
             public int compare(ProjectVariable v1, ProjectVariable v2) {
                 return v1.getQualifiedName().compareTo(v2.getQualifiedName());
             }
         });
     }
     
     public String getNameForPrint() {
         if (isParameter()) {
             int index1 = qname.lastIndexOf('!');
             int index2 = qname.lastIndexOf('$');
             return qname.substring(index1 + 1, index2) + " : " + type;
         } else if (isReturn()) {
             return "$_ : " + type;
         } else {
             if (getParent() != null) {
                 return parent.getNameForPrint() + "." + access + " : " + type;
             } else {
                 return access + " : " + type;
             }
         }
     }
 }
 