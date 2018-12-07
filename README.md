# jSicko

jSicko is a Java SImple Contract checKer. It works as a Java compiler plugin that processes some simple annotations representing contracts and produces runtime checks. 

One of the peculiarities of jSicko is that contract clauses are expressed in valid Java with some conventions, and not with a specific DSL (like in the case of JML). Here's a simple example.

## Simple Example
Imagine you want to write a simple contract for a square root function. What you first have to do is define two helper methods that represent the conditions themselves. These are specification clause methods, so we use a different naming condition than usual Java methods.

```java
public abstract class Math implements Contract {

    // ...

    @Pure
    private static boolean non_negative_arg(double arg) {
        return arg >= 0;
    }

    @Pure
    private static boolean approximate_returns(double returns, double arg) {
        return java.lang.Math.abs((returns * returns) - arg) < 0.001;
    }

}
```

Then, in the `sqrt` method, you can simply bind the first spec clause method (`non_negative_arg`) as the precondition of `sqrt`, and `approximate_returns` as its postconditon as follows:

```java
    @Requires("non_negative_arg")
    @Ensures({"approximate_returns"})
    public static double sqrt(double arg) {
        return java.lang.Math.sqrt(arg);
    }
```

How does the binding to parameters happen? In general, the binding is by name. However, if you call a parameter `returns`, that gets bound to the return value of the method. 

Even if you do not have to care about it, jSicko rewrites the method as follows:

```java
  public static double sqrt(double arg) {
      if (!non_negative_arg(arg)) {
          throw new PreconditionViolation("Precondition non_negative_arg violated on method sqrt");
      }
      // some more instrumentation ...
      double $returns = 0;
      try {
          return $returns = java.lang.Math.sqrt(arg);
      } finally {
          if (!returns_approximately_equal_to_square_of_arg($returns, arg)) {
              throw new PostconditionViolation("Postcondition returns_approximately_equal_to_square_of_arg violated on method sqrt");
          }
          // some more instrumentation ...
      }
  }
```

Essentially, jSicko checks first the preconditions, then boxes the body of the method into a `try/finally` block, storing the return value into a synthetic `$returns` variable.

## Features

jSicko supports contract inheritance (meaning that overridden methods inherit contracts from superclasses and interfaces), old values (by using the static `old` method in the `Contract` class), and class invariants.

For more examples and description of features, please check the official [jSicko Tutorials](https://github.com/si-codelounge/jsicko-tutorials) project.

## Current version and usage with maven

The last version of jSicko is `1.0.0-M2`, and it is published in bintray.com. If you are using maven, you must add the bintray repository into your `pom.xml`:

```xml
<repository>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
    <id>bintray-codelounge-ch.usi.si.codelounge</id>
    <name>bintray</name>
    <url>https://dl.bintray.com/codelounge/ch.usi.si.codelounge</url>
</repository>
```  

Thus, you can add the following dependency:

```xml
<dependency>
    <groupId>ch.usi.si.codelounge</groupId>
    <artifactId>jSicko</artifactId>
    <version>1.0.0-M2</version>
</dependency>
```      

Finally, you need to enable the compiler plugin as an option of the maven compiler plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.0</version>
    <executions>
        <execution>
            <id>compile</id>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <compilerArgs>
                    <arg>-Xplugin:JSickoContractCompiler</arg>
                </compilerArgs>
            </configuration>
        </execution>
        <execution>
            <id>test-compile</id>
            <goals>
                <goal>testCompile</goal>
            </goals>
            <configuration>
                <compilerArgs>
                    <arg>-Xplugin:JSickoContractCompiler</arg>
                </compilerArgs>
            </configuration>
        </execution>
    </executions>
</plugin>
```                

For instructions on how to run it with your IDE, please check the official [jSicko Tutorials](https://github.com/si-codelounge/jsicko-tutorials) project.
