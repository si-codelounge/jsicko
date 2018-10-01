package ch.usi.si.codelounge.jsicko.plugin.utils;

import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JavacUtils {

    private final Types types;
    private final Names symbolsTable;
    private final TreeMaker factory;
    private final Enter enter;
    private final MemberEnter memberEnter;

    public JavacUtils(BasicJavacTask task) {
        this.symbolsTable = Names.instance(task.getContext());
        this.types = Types.instance(task.getContext());
        this.factory = TreeMaker.instance(task.getContext());
        this.enter = Enter.instance(task.getContext());
        this.memberEnter = MemberEnter.instance(task.getContext());
    }

    public JCTree.JCExpression constructExpression(String... identifiers) {
        var nameList = Arrays.stream(identifiers).map((String string) -> symbolsTable.fromString(string));

        var expr = nameList.reduce(null, (JCTree.JCExpression firstElem, Name name) -> {
            if (firstElem == null)
                return factory.Ident(name);
            else
                return (JCTree.JCExpression) factory.Select(firstElem, name);
        }, (JCTree.JCExpression firstElem, JCTree.JCExpression name) -> name);

        System.out.println(" *** " + expr);

        return expr;
    }

    public TreeMaker getFactory() {
        return this.factory;
    }

    public JCTree.JCExpression constructExpression(String qualifiedName) {
        String[] splitQualifiedName = qualifiedName.split("\\.");
        return constructExpression(splitQualifiedName);
    }

    public List<Type> typeClosure(Type t) {
        return this.types.closure(t);
    }

    public Type typeErasure(Type t) {
        return t.asElement().erasure(types);
    }

    public Name nameFromString(String name) {
        return this.symbolsTable.fromString(name);
    }

    public boolean isTypeAssignable(Type t, Type s) {
        return types.isAssignable(t, s);
    }

    public java.util.List<Symbol> findOverriddenMethods(JCTree.JCClassDecl classDecl, JCTree.JCMethodDecl methodDecl) {
        var methodSymbol = methodDecl.sym;
        var thisTypeClosure = this.typeClosure(classDecl.sym.type);

        return thisTypeClosure.stream().flatMap((Type contractType) -> {
            Stream<Symbol> contractOverriddenSymbols = contractType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> {
                return methodSymbol.getQualifiedName().equals(contractElement.name) &&
                        methodSymbol.overrides(contractElement, contractType.tsym, types, true, true);
            });
            return contractOverriddenSymbols;
        }).collect(Collectors.toList());
    }

    public boolean hasVoidReturnType(MethodTree methodTree) {
        var methodReturnType = methodTree.getReturnType();
        return (methodReturnType instanceof JCTree.JCPrimitiveTypeTree) && ((JCTree.JCPrimitiveTypeTree) methodReturnType).typetag.equals(TypeTag.VOID);
    }

    public JCTree.JCLiteral zeroValue(Type t) {
        return t.isPrimitive() ?
                factory.Literal(t.getTag(),0)
                : factory.Literal(TypeTag.BOT,null);
    }

}
