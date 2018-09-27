package ch.usi.si.codelounge.jsicko.plugin;

import ch.usi.si.codelounge.jsicko.Contract;
import ch.usi.si.codelounge.jsicko.utils.JavacUtils;
import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.tools.javac.util.List.nil;

class ContractCollectorTreeScanner extends TreeScanner<Void, Stack<Tree>> {

    private final Types types;
    private final Names symbolsTable;
    private final TreeMaker factory;
    private final Enter enter;
    private final MemberEnter memberEnter;

    private boolean hasContract;

    private Optional<JCVariableDecl> currentMethodReturnVarDecl = Optional.empty();
    private Optional<JCVariableDecl> optionalOldField = Optional.empty();
    private Optional<JCMethodDecl> overriddenOldMethod = Optional.empty();
    private Optional<CompilationUnitTree> currentCompilationUnitTree = Optional.empty();
    private Optional<JCClassDecl> currentClassTree = Optional.empty();
    private Optional<MethodTree> currentMethodTree;


    public ContractCollectorTreeScanner(BasicJavacTask task) {
        this.symbolsTable = Names.instance(task.getContext());
        this.types =  Types.instance(task.getContext());
        this.factory = TreeMaker.instance(task.getContext());
        this.enter = Enter.instance(task.getContext());
        this.memberEnter = MemberEnter.instance(task.getContext());
        this.hasContract = false;
        this.overriddenOldMethod = Optional.empty();

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
                this.currentClassTree = Optional.of(classDecl);
                optionalDeclareOldValueVariable(classDecl);
            } else {
                this.currentClassTree = Optional.empty();
                this.optionalOldField = Optional.empty();
                this.currentMethodReturnVarDecl = Optional.empty();
                this.overriddenOldMethod = Optional.empty();
            }

        });

        return super.visitClass(classTree, relevantScope);
    }



    private List<Type> retrieveContractTypes(Type t) {
        var contractTypeName = this.symbolsTable.fromString("ch.usi.si.codelounge.jsicko.Contract");
        var closure = types.closure(t);

        var contractType = closure.stream().filter((Type closureElem) -> {
            var rawType = closureElem.asElement().erasure(types);
            return rawType.asElement().getQualifiedName().equals(contractTypeName);
        }).map((Type likelyContractType) -> likelyContractType.asElement().erasure(types)).findFirst();


        if (contractType.isPresent()) {
            return closure.stream().filter((Type closureElem) -> {
                var rawType = closureElem.asElement().erasure(types);
                return rawType.isInterface() && types.isAssignable(rawType,contractType.get());
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

        if (this.hasContract && currentCompilationUnitTree.isPresent() && !methodDecl.equals(this.overriddenOldMethod.get())) {
            System.out.println("Visiting Declared Method in a Class w/Contract: " + methodTree.getName() + " w return type " + methodTree.getReturnType()) ;

            var methodSymbol = methodDecl.sym;
            var thisTypeClosure = types.closure(this.currentClassTree.get().sym.type);

            overriddenMethods = thisTypeClosure.stream().flatMap((Type contractType) -> {
              Stream<Symbol> contractOverriddenSymbols = contractType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> {
                    return methodSymbol.getQualifiedName().equals(contractElement.name) &&
                            methodSymbol.overrides(contractElement, contractType.tsym, types, true, true);
                });
              return contractOverriddenSymbols;
            }).collect(Collectors.toList());

            if (!overriddenMethods.contains(methodSymbol))
                overriddenMethods.add(methodSymbol);


            if (overriddenMethods.size() > 0) {
                Void w = null;
                JCBlock finallyBlock = null;

                List<Contract.Requires> requires = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Requires.class))).collect(Collectors.toList());
                List<Contract.Ensures> ensures = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Ensures.class))).collect(Collectors.toList());

                var isMarkedPure = overriddenMethods.stream().flatMap((Symbol overriddenMethod) -> Arrays.stream(overriddenMethod.getAnnotationsByType(Contract.Pure.class))).collect(Collectors.toList()).size() > 0;

                finallyBlock = boxMethodBody(methodTree);
                optionalDeclareReturnValueCatcher(methodTree);

                if (!methodSymbol.isConstructor() && !methodSymbol.isStatic() && !isMarkedPure) {
                    optionalSaveOldState(methodTree, (JCClassDecl) this.currentClassTree.get());
                }

                addPreconditions(methodTree, requires);

                relevantScope.push(methodTree);
                w = super.visitMethod(methodTree, relevantScope);
                relevantScope.pop();

                addPostconditions(methodTree, finallyBlock, ensures);

                System.out.println("Code of Instrumented Method");
                System.out.println(methodTree);
                return w;
            } else {
                this.currentMethodReturnVarDecl = Optional.empty();
            }
        }

        return super.visitMethod(methodTree, relevantScope);
    }

    private void optionalSaveOldState(MethodTree methodTree,  JCClassDecl classDecl) {
        this.optionalOldField.ifPresent((JCVariableDecl oldField) -> {
            var methodDecl = (JCMethodDecl) methodTree;

            var thisType = factory.This(classDecl.sym.type);
            var methodExpr = JavacUtils.constructExpression(factory, symbolsTable,"ch.usi.si.codelounge.jsicko.utils.CloneUtils.kryoClone");
            var call = factory.Apply(com.sun.tools.javac.util.List.nil(),methodExpr, com.sun.tools.javac.util.List.of(thisType));
            var assignment = factory.Assignment(oldField.sym,call);
            methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(assignment);
        });
    }

    private void optionalDeclareOldValueVariable(JCClassDecl classDecl) {
        if (!this.optionalOldField.isPresent()) {

            var thisType = classDecl.sym.type;
            var varSymbol = new Symbol.VarSymbol(Flags.PRIVATE, symbolsTable.fromString(Constants.OLD_FIELD_IDENTIFIER_STRING), thisType, classDecl.sym);
            classDecl.sym.members().enter(varSymbol);
            var oldField = factory.VarDef(varSymbol, factory.Literal(TypeTag.BOT, null));
            this.optionalOldField = Optional.of(oldField);
            ((JCClassDecl)this.currentClassTree.get()).defs = ((JCClassDecl)this.currentClassTree.get()).defs.prepend(oldField);

            /* Override @old */
            var overriddenOldMethodType = new Type.MethodType(com.sun.tools.javac.util.List.nil(),
                    thisType,
                    com.sun.tools.javac.util.List.nil(),
                    classDecl.sym);
            var overriddenOldMethodSymbol = new Symbol.MethodSymbol(Flags.PUBLIC,symbolsTable.fromString(Constants.OLD_METHOD_IDENTIFIER_STRING),overriddenOldMethodType,classDecl.sym);
            var returnOldFieldStatement = factory.Return(factory.Ident(varSymbol));
            var overriddenOldMethodBody = factory.Block(0, com.sun.tools.javac.util.List.of(returnOldFieldStatement));
            var overriddenOldMethod = factory.MethodDef(overriddenOldMethodSymbol, overriddenOldMethodBody);

            ((JCClassDecl)this.currentClassTree.get()).defs = ((JCClassDecl)this.currentClassTree.get()).defs.prepend(overriddenOldMethod);

            this.overriddenOldMethod = Optional.of(overriddenOldMethod);

            classDecl.sym.members().enter(overriddenOldMethodSymbol);

            System.err.println(this.currentClassTree);
        }
    }

    private JCBlock boxMethodBody(MethodTree methodTree) {
        JCBlock body = (JCBlock) methodTree.getBody();
        JCMethodDecl methodDecl = (JCMethodDecl) methodTree;
        var methodSymbol = methodDecl.sym;

        if (methodSymbol.isConstructor())
            return body;

        var finallyBlock = factory.Block(0, com.sun.tools.javac.util.List.nil());

        var tryBlock = factory.Try(factory.Block(0,body.stats), com.sun.tools.javac.util.List.nil(), finallyBlock);
        body.stats = com.sun.tools.javac.util.List.of(tryBlock);
        return finallyBlock;
    }

    private void optionalDeclareReturnValueCatcher(MethodTree methodTree) {
        JCBlock body = (JCBlock) methodTree.getBody();
        JCMethodDecl methodDecl = (JCMethodDecl) methodTree;
        if (!methodDecl.sym.isConstructor() && !hasVoidReturnType(methodTree)) {
            JCLiteral zeroValue = (methodDecl.sym.getReturnType().isPrimitive() ?
                    factory.Literal(methodDecl.sym.getReturnType().getTag(),0)
                    : factory.Literal(TypeTag.BOT,null));

            var varDef = factory.VarDef(factory.Modifiers(0), symbolsTable.fromString(Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING), methodDecl.restype,zeroValue);
            body.stats = body.stats.prepend(varDef);

            this.currentMethodReturnVarDecl = Optional.of(varDef);
        } else {
            this.currentMethodReturnVarDecl = Optional.empty();
        }
    }

    private boolean hasVoidReturnType(MethodTree methodTree) {
        return methodTree.getReturnType().toString().equals("void");
    }

    private void addPreconditions(MethodTree specifiedMethodTree, List<Contract.Requires> requires) {
        JCBlock body = (JCBlock) specifiedMethodTree.getBody();
        JCMethodDecl methodDecl = (JCMethodDecl) specifiedMethodTree;

        System.out.println("For method " + specifiedMethodTree.getName() + " - creating precondition check " + requires);

        var allRequiresClauses = requires.stream().flatMap(clauseGroup -> Arrays.stream(clauseGroup.value())).collect(Collectors.toList());

        allRequiresClauses.forEach((String requiresClause) -> {
            JCIf check = createPreconditionCheck(specifiedMethodTree, requiresClause, ContractConditionEnum.PRECONDITION);
            body.stats = body.stats.prepend(check);
        });
    }

    private void addPostconditions(MethodTree method, JCBlock finallyBlock, List<Contract.Ensures> ensures) {
        System.out.println("For method " + method.getName() + " - creating postcondition check " + ensures);

        var allEnsuresClauses = ensures.stream().flatMap(clauseGroup -> Arrays.stream(clauseGroup.value())).collect(Collectors.toList());

        allEnsuresClauses.forEach((String ensuresClause) -> {
            JCIf check = createPreconditionCheck(method, ensuresClause, ContractConditionEnum.POSTCONDITION);
            finallyBlock.stats = finallyBlock.stats.prepend(check);
        });

    }

    @Override
    public Void visitReturn(ReturnTree node, Stack<Tree> relevantScope) {
        var returnNode = (JCReturn) node;

        this.currentMethodReturnVarDecl.ifPresent((JCVariableDecl varDecl) -> {
            var scopePeek = relevantScope.peek();
            if (scopePeek.equals(this.currentMethodTree.get())) {
                var newArg = factory.Assign(factory.Ident(symbolsTable.fromString("$returns")), ((JCReturn) node).expr);
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


    private JCIf createPreconditionCheck(MethodTree method, String requiresClause, ContractConditionEnum conditionType) {
        return factory.at(((JCTree) method).pos).If(factory.Parens(createPreconditionCheckCondition(method, requiresClause)),
                        createPreconditionCheckBlock( method, requiresClause, conditionType),
                        null);
    }

    private JCExpression createPreconditionCheckCondition(MethodTree method, String requiresClause) {

        var isNegated = (requiresClause.startsWith("!"));
        var methodIdentifier = (isNegated ? requiresClause.substring(1) : requiresClause);
        System.out.println(methodIdentifier + " : " + symbolsTable.fromString(methodIdentifier));

        Name methodName = symbolsTable.fromString(methodIdentifier);
        Optional<Symbol.MethodSymbol> methodSymbol = findContractMethod(methodName);

        if (!methodSymbol.isPresent()) {
            return this.factory.Erroneous(com.sun.tools.javac.util.List.of((JCTree)method));
        }

        var clauseSymbol = methodSymbol.get();
            List<JCExpression> args = clauseSymbol.params().stream().map((Symbol.VarSymbol clauseParamSymbol) -> {
                var clauseParamName = clauseParamSymbol.name.toString();
                if (clauseParamName.equals(Constants.RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                    clauseParamName = Constants.RETURNS_SYNTHETIC_IDENTIFIER_STRING;
                }
                return factory.Ident(symbolsTable.fromString(clauseParamName));
            }).collect(Collectors.toList());

        var call = factory.App(factory.Ident(methodSymbol.get()), com.sun.tools.javac.util.List.from(args.toArray( new JCExpression[] {})));
        return (isNegated ? call: factory.Unary(Tag.NOT,call) );

    }

    private Optional<Symbol.MethodSymbol> findContractMethod(Name methodIdentifier) {
        var classType = ((JCClassDecl) this.currentClassTree.get()).sym.type;
        var closure = types.closure(classType);

        var contractMethod =  closure.stream().flatMap((Type closureElem) ->
                closureElem.asElement().getEnclosedElements().stream()
                .filter((Symbol contractElement) -> contractElement.name.equals(methodIdentifier))
                .map((Symbol contractElement) -> (Symbol.MethodSymbol) contractElement))
                .findFirst();

        return contractMethod;
    }

    private JCBlock createPreconditionCheckBlock(MethodTree method, String requiresClause, ContractConditionEnum conditionType) {

        String errorMessagePrefix = String.format("%s %s violated on method %s", conditionType, requiresClause, method.getName().toString());

        return factory.Block(0, com.sun.tools.javac.util.List.of(
                factory.Throw(
                        factory.NewClass(null, nil(),
                                factory.Ident(symbolsTable.fromString(conditionType.getAssertionErrorSpecificClass().getSimpleName())),
                                com.sun.tools.javac.util.List.of(factory.Literal(TypeTag.CLASS, errorMessagePrefix)), null))));
    }
}
