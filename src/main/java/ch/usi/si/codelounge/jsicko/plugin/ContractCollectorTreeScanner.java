package ch.usi.si.codelounge.jsicko.plugin;

import ch.usi.si.codelounge.jsicko.Contract;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.comp.Check;

import javax.lang.model.type.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.sun.tools.javac.util.List.nil;

public class ContractCollectorTreeScanner extends TreeScanner<Void, Void> {

    private final BasicJavacTask task;
    private final Trees trees;
    private final Types types;
    private final String OLD_METHOD_IDENTIFIER_STRING = "old";
    private Optional<CompilationUnitTree> currentCompilationUnitTree = Optional.empty();
    private Optional<ClassTree> currentClassTree = Optional.empty();
    private final Names symbolsTable;
    private final TreeMaker factory;
    private boolean hasContract;
    private List<Tree> contracts;
    private Optional<JCTree.JCVariableDecl> returnVarDecl = Optional.empty();
    private Optional<JCTree.JCVariableDecl> optionalOldField = Optional.empty();
    private static final String RETURNS_SYNTHETIC_IDENTIFIER_STRING = "$returns";
    private final String RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING = "returns";
    private final String OLD_FIELD_IDENTIFIER_STRING = "$old";
    private final Check chk;
    private final Log log;
    private final Resolve rs;


    public ContractCollectorTreeScanner(BasicJavacTask task) {
        this.task = task;
        this.symbolsTable = Names.instance(task.getContext());
        this.trees = Trees.instance(task);
        this.types =  Types.instance(task.getContext());
        this.factory = TreeMaker.instance(task.getContext());
        this.hasContract = false;
        this.chk = Check.instance(task.getContext());
        this.log = Log.instance(task.getContext());
        this.rs = Resolve.instance(task.getContext());
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Void v) {
        this.currentCompilationUnitTree = Optional.of(node);
        return super.visitCompilationUnit(node,v);
    }

    @Override
    public Void visitClass(ClassTree classTree, Void v) {

        this.hasContract = false;

        this.currentCompilationUnitTree.ifPresent((CompilationUnitTree currentCompilationUnitTree) -> {

            var implementsClauses = classTree.getImplementsClause().stream();

            var contracts = implementsClauses.filter((Tree interfaceTree) -> {

                var interfaceType =  trees.getTypeMirror(TreePath.getPath(currentCompilationUnitTree,interfaceTree));

                if (interfaceType instanceof DeclaredType) {
                    var element = ((DeclaredType) interfaceType).asElement();
                    if (element instanceof Symbol.ClassSymbol) {
                        var classSymbol = (Symbol.ClassSymbol) element;
                        var contractInterfaces = classSymbol.getInterfaces().stream().filter(this::extendsJsickoContract).collect(Collectors.toList());

                        return !contractInterfaces.isEmpty();
                    }

                }
                return false;
            }).collect(Collectors.toList());

            this.hasContract = !contracts.isEmpty();
            this.contracts = new ArrayList<>(contracts);
            if (hasContract) {
                System.out.println("Found class " + classTree.getSimpleName() + " extending following contracts: " + this.contracts);
                this.currentClassTree = Optional.of(classTree);
                optionalDeclareOldValueVariable(this.contracts.get(0), (JCTree.JCClassDecl) classTree);
            } else {
                this.currentClassTree = Optional.empty();
                this.optionalOldField = Optional.empty();
                this.returnVarDecl = Optional.empty();
            }

        });

        return super.visitClass(classTree, v);
    }

    private boolean extendsJsickoContract(Type t) {
        var contractTypeName = this.symbolsTable.fromString("ch.usi.si.codelounge.jsicko.Contract");
        var rawType = t.asElement().erasure(types);
        return t.isInterface() && rawType.asElement().getQualifiedName().equals(contractTypeName);
    }

    @Override
    public Void visitMethod(MethodTree methodTree, Void v) {
        var trees = Trees.instance(task);

        Optional<Symbol> optionalOverriddenMethod = Optional.empty();

        if (this.hasContract && currentCompilationUnitTree.isPresent()) {
            System.out.println("Visiting Declared Method in a Class w/Contract: " + methodTree.getName() + " w return type " + methodTree.getReturnType()) ;

            var methodSymbol = ((JCTree.JCMethodDecl)methodTree).sym;
            var contract = this.contracts.get(0);

            var contractType = trees.getTypeMirror(TreePath.getPath(currentCompilationUnitTree.get(), contract));
            var contractClassType = (Type.ClassType) contractType;

            optionalOverriddenMethod = contractClassType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> {
                return methodSymbol.getQualifiedName().equals(contractElement.name) &&
                        methodSymbol.overrides(contractElement, contractClassType.tsym, types, true, true);
            }).findFirst();

            if (optionalOverriddenMethod.isPresent()) {
                Void w = null;
                JCTree.JCBlock finallyBlock = null;

                var overriddenMethod = optionalOverriddenMethod.get();
//                System.out.println("Overridden method: " + overriddenMethod);
                List<Contract.Requires> requires = Arrays.asList(overriddenMethod.getAnnotationsByType(Contract.Requires.class));
//                System.out.println("Overridden method requires:" + requires);
                List<Contract.Ensures> ensures = Arrays.asList(overriddenMethod.getAnnotationsByType(Contract.Ensures.class));
//                System.out.println("Overridden method ensures:" + ensures);

                var isMarkedPure = overriddenMethod.getAnnotation(Contract.Pure.class) != null;

                finallyBlock = boxMethodBody(methodTree);
                optionalDeclareReturnValueCatcher(methodTree);

                if (!isMarkedPure) {
                    optionalSaveOldState(methodTree, (JCTree.JCClassDecl) this.currentClassTree.get());
                }

                System.out.println(this.optionalOldField);

                addPreconditions(methodTree, requires);

                w = super.visitMethod(methodTree, v);

                addPostconditions(methodTree, finallyBlock, ensures);

                System.out.println("Code of Instrumented Method");
                System.out.println(methodTree);
                return w;
            } else {
                this.returnVarDecl = Optional.empty();
            }
        }

        return super.visitMethod(methodTree, v);
    }

    private void optionalSaveOldState(MethodTree methodTree,  JCTree.JCClassDecl classDecl) {
        this.optionalOldField.ifPresent((JCTree.JCVariableDecl oldField) -> {
            var methodDecl = (JCTree.JCMethodDecl) methodTree;

            JCTree.JCExpression first = factory.Ident(symbolsTable.fromString("ch"));
            var nameList = List.of("usi","si","codelounge","jsicko","utils","CloneUtils","kryoClone").stream().map((String string) ->{
                return symbolsTable.fromString(string);
            });

            var cur = first;
            var methodExpr = nameList.reduce(first, (JCTree.JCExpression firstElem, Name name) -> {
                return (JCTree.JCExpression) factory.Select(firstElem, name);
            }, (JCTree.JCExpression firstElem, JCTree.JCExpression name) -> {
                return  name;
            });

            var thisType = factory.This(classDecl.sym.type);
            var call = factory.Apply(com.sun.tools.javac.util.List.nil(),methodExpr, com.sun.tools.javac.util.List.of(thisType));
            var assignment = factory.Assignment(oldField.sym,call);
            methodDecl.getBody().stats = methodDecl.getBody().stats.prepend(assignment);
        });
    }

    private void optionalDeclareOldValueVariable(Tree contractTypeTree, JCTree.JCClassDecl classDecl) {
        if (!this.optionalOldField.isPresent()) {

            var thisType = classDecl.sym.type;
            var varSymbol = new Symbol.VarSymbol(Flags.PRIVATE, symbolsTable.fromString(OLD_FIELD_IDENTIFIER_STRING), thisType, classDecl.sym);
            classDecl.sym.members().enter(varSymbol);
            var oldField = factory.VarDef(varSymbol, factory.Literal(TypeTag.BOT, null));
            this.optionalOldField = Optional.of(oldField);
            ((JCTree.JCClassDecl)this.currentClassTree.get()).defs = ((JCTree.JCClassDecl)this.currentClassTree.get()).defs.prepend(oldField);

            /* Override @old */
            var overriddenOldMethodType = new Type.MethodType(com.sun.tools.javac.util.List.nil(),
                    thisType,
                    com.sun.tools.javac.util.List.nil(),
                    classDecl.sym);
            var overriddenOldMethodSymbol = new Symbol.MethodSymbol(Flags.PUBLIC,symbolsTable.fromString(OLD_METHOD_IDENTIFIER_STRING),overriddenOldMethodType,classDecl.sym);
            var returnOldFieldStatement = factory.Return(factory.Ident(varSymbol));
            var overriddenOldMethodBody = factory.Block(0, com.sun.tools.javac.util.List.of(returnOldFieldStatement));
            var overriddenOldMethod = factory.MethodDef(overriddenOldMethodSymbol, overriddenOldMethodBody);
            System.err.println(overriddenOldMethod);

            ((JCTree.JCClassDecl)this.currentClassTree.get()).defs = ((JCTree.JCClassDecl)this.currentClassTree.get()).defs.prepend(overriddenOldMethod);

            classDecl.sym.members().enter(overriddenOldMethodSymbol);

            System.err.println(this.currentClassTree);
        }
    }

    @Override
    public Void visitVariable(VariableTree node, Void v) {
        if (node.toString().contains(" baseObject ")) {
            System.err.println(node.getModifiers());
        }
        return super.visitVariable(node, v);
    }


    private JCTree.JCBlock boxMethodBody(MethodTree methodTree) {
        JCTree.JCBlock body = (JCTree.JCBlock) methodTree.getBody();
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) methodTree;

        var finallyBlock = factory.Block(0, com.sun.tools.javac.util.List.nil());

        var tryBlock = factory.Try(factory.Block(0,body.stats), com.sun.tools.javac.util.List.nil(),finallyBlock);
        body.stats = com.sun.tools.javac.util.List.of(tryBlock);
        return finallyBlock;
    }

    private void optionalDeclareReturnValueCatcher(MethodTree methodTree) {
        JCTree.JCBlock body = (JCTree.JCBlock) methodTree.getBody();
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) methodTree;
        if (!hasVoidReturnType(methodTree)) {
            JCTree.JCLiteral zeroValue = (methodDecl.sym.getReturnType().isPrimitive() ?
                    factory.Literal(methodDecl.sym.getReturnType().getTag(),0)
                    : factory.Literal(TypeTag.BOT,null));

            var varDef = factory.VarDef(factory.Modifiers(0), symbolsTable.fromString(RETURNS_SYNTHETIC_IDENTIFIER_STRING), methodDecl.restype,zeroValue);
            body.stats = body.stats.prepend(varDef);

            this.returnVarDecl = Optional.of(varDef);
        } else {
            this.returnVarDecl = Optional.empty();
        }
    }

    private boolean hasVoidReturnType(MethodTree methodTree) {
        return methodTree.getReturnType().toString().equals("void");
    }



    private void addPreconditions(MethodTree specifiedMethodTree, List<Contract.Requires> requires) {
        JCTree.JCBlock body = (JCTree.JCBlock) specifiedMethodTree.getBody();
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) specifiedMethodTree;

        System.out.println("For method " + specifiedMethodTree.getName() + " - creating precondition check " + requires);

        var allRequiresClauses = requires.stream().flatMap(clauseGroup -> Arrays.stream(clauseGroup.value())).collect(Collectors.toList());

        allRequiresClauses.forEach((String requiresClause) -> {
            JCTree.JCIf check = createPreconditionCheck(specifiedMethodTree, requiresClause, ContractConditionEnum.PRECONDITION);
            body.stats = body.stats.prepend(check);

        });

    }

    private void addPostconditions(MethodTree method, JCTree.JCBlock finallyBlock, List<Contract.Ensures> ensures) {

        JCTree.JCBlock body = (JCTree.JCBlock) method.getBody();
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) method;

        System.out.println("For method " + method.getName() + " - creating postcondition check " + ensures);

        var allEnsuresClauses = ensures.stream().flatMap(clauseGroup -> Arrays.stream(clauseGroup.value())).collect(Collectors.toList());

        allEnsuresClauses.forEach((String ensuresClause) -> {
            JCTree.JCIf check = createPreconditionCheck(method, ensuresClause, ContractConditionEnum.POSTCONDITION);
            finallyBlock.stats = finallyBlock.stats.prepend(check);
        });

    }

    @Override
    public Void visitReturn(ReturnTree node, Void v) {
        var returnNode = (JCTree.JCReturn) node;

        this.returnVarDecl.ifPresent((JCTree.JCVariableDecl varDecl) -> {
            //System.err.println(varDecl.sym);
            var newArg = factory.Assign(factory.Ident(symbolsTable.fromString("$returns")),((JCTree.JCReturn) node).expr);
            System.err.println(newArg);

            returnNode.expr = newArg;
        });

        return super.visitReturn(node, v);
    }

    private JCTree.JCIf createPreconditionCheck(MethodTree method, String requiresClause, ContractConditionEnum conditionType) {
        return factory.at(((JCTree) method).pos).If(factory.Parens(createPreconditionCheckCondition(method, requiresClause)),
                        createPreconditionCheckBlock( method, requiresClause, conditionType),
                        null);
    }

    private JCTree.JCExpression createPreconditionCheckCondition(MethodTree method, String requiresClause) {

        var isNegated = (requiresClause.startsWith("!"));
        var methodIdentifier = (isNegated ? requiresClause.substring(1) : requiresClause);
        System.out.println(methodIdentifier + " : " + symbolsTable.fromString(methodIdentifier));

        Name methodName = symbolsTable.fromString(methodIdentifier);
        Optional<Symbol.MethodSymbol> methodSymbol = findContractMethod(methodName);

        if (!methodSymbol.isPresent()) {
            return this.factory.Erroneous(com.sun.tools.javac.util.List.of((JCTree)method));
        }

        var clauseSymbol = methodSymbol.get();
            List<JCTree.JCExpression> args = clauseSymbol.params().stream().map((Symbol.VarSymbol clauseParamSymbol) -> {
                var clauseParamName = clauseParamSymbol.name.toString();
                if (clauseParamName.equals(RETURNS_CLAUSE_PARAMETER_IDENTIFIER_STRING)) {
                    clauseParamName = RETURNS_SYNTHETIC_IDENTIFIER_STRING;
                }
                return factory.Ident(symbolsTable.fromString(clauseParamName));
            }).collect(Collectors.toList());

        var call = factory.App(factory.Ident(methodSymbol.get()), com.sun.tools.javac.util.List.from(args.toArray( new JCTree.JCExpression[] {})));
        return (isNegated ? call: factory.Unary(JCTree.Tag.NOT,call) );

    }

    private Optional<Symbol.MethodSymbol> findContractMethod(Name methodIdentifier) {
        var contract = this.contracts.get(0);

        var contractType = trees.getTypeMirror(TreePath.getPath(currentCompilationUnitTree.get(), contract));
        var contractClassType = (Type.ClassType) contractType;

        var contractMethod =  contractClassType.tsym.getEnclosedElements().stream().filter((Symbol contractElement) -> {
            return contractElement.name.equals(methodIdentifier);
        }).map((Symbol contractElement) -> (Symbol.MethodSymbol) contractElement).findFirst();

        return contractMethod;
    }

    private JCTree.JCBlock createPreconditionCheckBlock(MethodTree method, String requiresClause, ContractConditionEnum conditionType) {

        String errorMessagePrefix = String.format("%s %s violated on method %s", conditionType, requiresClause, method.getName().toString());

        return factory.Block(0, com.sun.tools.javac.util.List.of(
                factory.Throw(
                        factory.NewClass(null, nil(),
                                factory.Ident(symbolsTable.fromString(conditionType.getAssertionErrorSpecificClass().getSimpleName())),
                                com.sun.tools.javac.util.List.of(factory.Literal(TypeTag.CLASS, errorMessagePrefix)), null))));
    }
}
