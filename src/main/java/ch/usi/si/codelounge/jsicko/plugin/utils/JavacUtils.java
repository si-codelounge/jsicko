package ch.usi.si.codelounge.jsicko.plugin.utils;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.Arrays;

public final class JavacUtils {

    private JavacUtils() {
        throw new RuntimeException("This is an utility class that is supposed to have no instances.");
    }

    public static JCTree.JCExpression constructExpression(TreeMaker factory, Names symbolsTable, String... identifiers) {
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

    public static JCTree.JCExpression constructExpression(TreeMaker factory, Names symbolsTable, String qualifiedName) {
        String[] splitQualifiedName = qualifiedName.split("\\.");
        return constructExpression(factory,symbolsTable,splitQualifiedName);
    }

}
