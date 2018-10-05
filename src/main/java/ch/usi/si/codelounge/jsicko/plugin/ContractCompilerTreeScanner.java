package ch.usi.si.codelounge.jsicko.plugin;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.plugin.utils.JavacUtils;
import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree.*;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ContractCompilerTreeScanner extends TreeScanner<Void, Stack<Tree>> {

    private final JavacUtils javac;

    private boolean hasContract;

    private Optional<JCVariableDecl> currentMethodReturnVarDecl = Optional.empty();
    private Optional<JCVariableDecl> optionalOldField = Optional.empty();
    private Optional<JCMethodDecl> overriddenOldMethod = Optional.empty();
    private Optional<CompilationUnitTree> currentCompilationUnitTree = Optional.empty();
    private Optional<JCClassDecl> currentClassDecl = Optional.empty();
    private Optional<MethodTree> currentMethodTree;
    private List<ConditionClause> classInvariants;

    public ContractCompilerTreeScanner(BasicJavacTask task) {
        this.javac = new JavacUtils(task);
        this.hasContract = false;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Stack<Tree> relevantScope) {
        this.currentCompilationUnitTree = Optional.of(node);
        return super.visitCompilationUnit(node,relevantScope);
    }

    @Override
    public Void visitClass(ClassTree classTree, Stack<Tree> relevantScope) {

        this.hasContract = false;

        this.currentCompilationUnitTree.ifPresent((CompilationUnitTree currentCompilationUnitTree) -> {

            var classDecl = (JCClassDecl) classTree;

            List<Type> contracts = retrieveContractTypes(classDecl.sym.type);
            System.out.println("Contracts for " + classTree.getSimpleName() + " are " + contracts);

            this.hasContract = !classDecl.sym.type.isInterface() && !contracts.isEmpty();

            if (hasContract) {
                this.currentClassDecl = Optional.of(classDecl);
                optionalDeclareOldVariableAndMethod(classDecl);
                this.classInvariants = ConditionClause.createInvariants(javac.findInvariants(this.currentClassDecl.get()),javac);
            } else {
                this.currentClassDecl = Optional.empty();
                this.optionalOldField = Optional.empty();
                this.currentMethodReturnVarDecl = Optional.empty();
                this.overriddenOldMethod = Optional.empty();
                this.classInvariants = List.of();
            }

        });

        return super.visitClass(classTree, relevantScope);
    }

    private List<Type> retrieveContractTypes(Type t) {
        var contractTypeName = javac.nameFromString("ch.usi.si.codelounge.jsicko.Contract");
        var closure = javac.typeClosure(t);

        var contractType = closure.stream().filter((Type closureElem) -> {
            var rawType = javac.typeErasure(closureElem);
            return rawType.asElement().getQualifiedName().equals(contractTypeName);
        }).map((Type likelyContractType) -> javac.typeErasure(likelyContractType)).findFirst();


        if (contractType.isPresent()) {
            return closure.stream().filter((Type closureElem) -> {
                var rawType = javac.typeErasure(closureElem);
                return rawType.isInterface() && javac.isTypeAssignable(rawType,contractType.get());
            }).collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    @Override
    public Void visitMethod(MethodTree methodTree, Stack<Tree> relevantScope) {
        var methodDecl = (JCMethodDecl) methodTree;

        this.currentMethodTree = Optional.of(methodTree);

        List<Symbol> overriddenMethods = List.of();
        this.currentMethodReturnVarDecl = Optional.empty();

        System.out.println("****** " + methodTree.getName() + ": " + methodTree.getModifiers().getFlags());

        if (this.hasContract &&
                currentCompilationUnitTree.isPresent() &&
                !methodDecl.equals(this.overriddenOldMethod.get()) &&
                !methodTree.getModifiers().getFlags().contains(Modifier.PRIVATE)) {
            System.out.println("Visiting Declared Method in a Class w/Contract: " + methodTree.getName() + " w return type " + methodTree.getReturnType()) ;

            var methodSymbol = methodDecl.sym;

            overriddenMethods = javac.findOverriddenMethods(this.currentClassDecl.get(), methodDecl);

            if (!overriddenMethods.contains(methodSymbol))
                overriddenMethods.add(methodSymbol);


            if (overriddenMethods.size() > 0) {
                Void w = null;
                JCTry tryBlock = null;

                List<Contract.Requires> requires = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Requires.class))).collect(Collectors.toList());
                List<Contract.Ensures> ensures = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Ensures.class))).collect(Collectors.toList());

                var isMarkedPure = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Pure.class))).collect(Collectors.toList()).size() > 0;

                tryBlock = boxMethodBody(methodTree);
                optionalDeclareReturnValueCatcher(methodTree);

                if (!methodSymbol.isConstructor() && !methodSymbol.isStatic() && !isMarkedPure) {
                    optionalSaveOldState(methodTree, this.currentClassDecl.get());
                }

                addPreconditions(methodTree, methodDecl.body, requires);

                relevantScope.push(methodTree);
                w = super.visitMethod(methodTree, relevantScope);
                relevantScope.pop();

                addPostconditions(methodTree, tryBlock.finalizer, ensures);
                if (!isMarkedPure) {
                    addInvariants(methodTree, tryBlock.finalizer);
                }
                System.out.println("Code of Instrumented Method");
                System.out.println(methodTree);
                return w;
            }
        }

        return super.visitMethod(methodTree, relevantScope);
    }

    private void optionalSaveOldState(MethodTree methodTree,  JCClassDecl classDecl) {
        final var factory = javac.getFactory();

        this.optionalOldField.ifPresent((JCVariableDecl oldField) -> {
            var methodDecl = (JCMethodDecl) methodTree;

            var thisType = factory.This(classDecl.sym.type);
            var methodExpr = javac.constructExpression("ch.usi.si.codelounge.jsicko.plugin.utils.CloneUtils.kryoClone");
            var call = factory.Apply(com.sun.tools.javac.util.List.nil(),methodExpr, com.sun.tools.javac.util.List.of(thisType));
            var assignment = factory.Assignment(oldField.sym,call);
            methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(assignment);
        });
    }

    private void optionalDeclareOldVariableAndMethod(JCClassDecl classDecl) {
        final var factory = javac.getFactory();

        if (!this.optionalOldField.isPresent()) {

            var thisType = classDecl.sym.type;
            var varSymbol = new Symbol.VarSymbol(Flags.PRIVATE, javac.nameFromString(Constants.OLD_FIELD_IDENTIFIER_STRING), thisType, classDecl.sym);
            classDecl.sym.members().enter(varSymbol);
            var oldField = factory.VarDef(varSymbol, factory.Literal(TypeTag.BOT, null));
            this.optionalOldField = Optional.of(oldField);
            this.currentClassDecl.get().defs = this.currentClassDecl.get().defs.prepend(oldField);

            /* Override @old */
            var overriddenOldMethodType = new Type.MethodType(com.sun.tools.javac.util.List.nil(),
                    thisType,
                    com.sun.tools.javac.util.List.nil(),
                    classDecl.sym);
            var overriddenOldMethodSymbol = new Symbol.MethodSymbol(Flags.PUBLIC,javac.nameFromString(Constants.OLD_METHOD_IDENTIFIER_STRING),overriddenOldMethodType,classDecl.sym);
            var returnOldFieldStatement = factory.Return(factory.Ident(varSymbol));
            var overriddenOldMethodBody = factory.Block(0, com.sun.tools.javac.util.List.of(returnOldFieldStatement));
            var overriddenOldMethod = factory.MethodDef(overriddenOldMethodSymbol, overriddenOldMethodBody);

            this.currentClassDecl.get().defs = this.currentClassDecl.get().defs.prepend(overriddenOldMethod);
            this.overriddenOldMethod = Optional.of(overriddenOldMethod);
            classDecl.sym.members().enter(overriddenOldMethodSymbol);

            System.err.println(this.currentClassDecl);
        }
    }

    private JCTry boxMethodBody(MethodTree methodTree) {
        final var factory = javac.getFactory();

        JCBlock body = (JCBlock) methodTree.getBody();
        JCMethodDecl methodDecl = (JCMethodDecl) methodTree;
        var methodSymbol = methodDecl.sym;
        final JCTry tryBlock;

        var finallyBlock = factory.Block(0, com.sun.tools.javac.util.List.nil());

        if (methodSymbol.isConstructor() && javac.isSuperOrThisConstructorCall(body.stats.head)) {
            var firstStatement = body.stats.head;
            var rest = body.stats.tail;
            tryBlock = factory.Try(factory.Block(0,rest), com.sun.tools.javac.util.List.nil(), finallyBlock);
            body.stats = com.sun.tools.javac.util.List.of((JCStatement)tryBlock).prepend(firstStatement);
        } else {
            tryBlock = factory.Try(factory.Block(0,body.stats), com.sun.tools.javac.util.List.nil(), finallyBlock);
            body.stats = com.sun.tools.javac.util.List.of(tryBlock);
        }

        return tryBlock;
    }

    private void optionalDeclareReturnValueCatcher(MethodTree methodTree) {
        final var factory = javac.getFactory();

        JCBlock body = (JCBlock) methodTree.getBody();
        JCMethodDecl methodDecl = (JCMethodDecl) methodTree;
        if (!methodDecl.sym.isConstructor() && !javac.hasVoidReturnType(methodTree)) {
            JCLiteral zeroValue = javac.zeroValue(methodDecl.getReturnType().type);
            var varDef = factory.VarDef(factory.Modifiers(0), javac.nameFromString(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING), methodDecl.restype,zeroValue);
            body.stats = body.stats.prepend(varDef);

            this.currentMethodReturnVarDecl = Optional.of(varDef);
        } else {
            this.currentMethodReturnVarDecl = Optional.empty();
        }
    }

    private void addPreconditions(MethodTree method, JCBlock block, List<Contract.Requires> requireClauses) {
        System.out.println("For method " + method.getName() + " - creating precondition check " + requireClauses);
        var allEnsuresClauses = requireClauses.stream().flatMap(clauseGroup -> ConditionClause.from(clauseGroup,javac).stream());
        addConditions(method, block, allEnsuresClauses);
    }

    private void addPostconditions(MethodTree method, JCBlock block, List<Contract.Ensures> ensuresClauses) {
        System.out.println("For method " + method.getName() + " - creating postcondition check " + ensuresClauses);
        var allEnsuresClauses = ensuresClauses.stream().flatMap(clauseGroup -> ConditionClause.from(clauseGroup,javac).stream());
        addConditions(method, block, allEnsuresClauses);
    }

    private void addInvariants(MethodTree method, JCBlock block) {
        System.out.println("For method " + method.getName() + " - creating invariant checks " + this.classInvariants);
        addConditions(method, block, this.classInvariants.stream());
    }

    private void addConditions(MethodTree method, JCBlock block, Stream<ConditionClause> conditions) {
        conditions.forEach((ConditionClause ensuresClause) -> {
            ensuresClause.resolveContractMethod(currentClassDecl.get());
            JCIf check = ensuresClause.createConditionCheck(method, ensuresClause);
            if (javac.isSuperOrThisConstructorCall(block.stats.head)) {
                block.stats = block.stats.tail.prepend(check).prepend(block.stats.head);
            } else {
                block.stats = block.stats.prepend(check);
            }
        });
    }

    @Override
    public Void visitReturn(ReturnTree node, Stack<Tree> relevantScope) {
        final var factory = javac.getFactory();

        var returnNode = (JCReturn) node;

        this.currentMethodReturnVarDecl.ifPresent((JCVariableDecl varDecl) -> {
            if (relevantScope.peek().equals(this.currentMethodTree.get())) {
                var newArg = factory.Assign(factory.Ident(javac.nameFromString(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING)), ((JCReturn) node).expr);
                returnNode.expr = newArg;
            }
        });

        return super.visitReturn(node, relevantScope);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree node, Stack<Tree> relevantScope) {
        Void v;
        relevantScope.push(node);
        v = super.visitLambdaExpression(node,relevantScope);
        relevantScope.pop();
        return v;
    }

}
