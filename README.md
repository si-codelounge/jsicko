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
    @Ensures("approximate_returns")
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

jSicko supports contract inheritance (meaning that overridden methods inherit contracts from superclasses and interfaces), old values (by using the static `old` method in the `Contract` class), exceptional behaviors, and class invariants.

For more examples and description of features, please check the official [jSicko Tutorials](https://github.com/si-codelounge/jsicko-tutorials) project.

## Current version and usage with maven

The last version of jSicko is `1.0.0-M4`, and it is published in bintray.com. If you are using maven, you must add the bintray repository into your `pom.xml`:

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
    <version>1.0.0-M4</version>
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

## Common Errors and Pitfalls

Here's a description of common errors and pitfalls that you may encounter while using jSicko. In general, 
jSicko fails at the first major error encountered, and reports only the first one.

### Compiler Notes

jSicko generates a lot of compiler notes that can be useful to debug specifications and to report bugs. 
The most important compiler note is the `Code of Instrumented Method`: If compilation is working as expected, this note will show
the code of instrumented method containing the synthetic jSicko variables and logic.

### Non-Public methods
Remember: jSicko instruments *only public methods*. If the method is not public, jSicko will ignore it.
Clauses may have any visibility, but remember that they must be accessible in the scope of the method where they are used.

### Missing Clause

This error is generated when you specify a clause that has not been declared, like in the snippet below:

```java
  @Pure boolean clause() { ... }

  @Pure
  @Ensures("clause_typo")
  public void someMethod() { }
```

In this case, jSicko generates the following error:

```
MissingClauseMethod.java:33: error: [jsicko] Missing clause clause_typo in MissingClauseMethod#someMethod()
  public void someMethod() {
              ^]
```

### Invariant/Clause Is not boolean    

This error is generated when the clause or invariant method does not return a boolean.

For example:

```java
  @Invariant
  @Pure
  public String non_boolean_invariant() { ... }
```

In this case, jSicko generates the following error:

```
InvariantIsNotBoolean.java:33: error: [jsicko] Invariant non_boolean_invariant return type is not boolean, declared as String.
        public String non_boolean_invariant() {
                      ^
```

A similar error is generated for `@Requires`/`@Ensures` clauses.


### Incompatible Clause

This error is generated when an instance clause is used in a contract of a static method.

For example:

```java
  @Pure
  boolean instance_clause(int i) { ... }

  @Ensures("instance_clause")
  public static void staticMethod(int i) { ... }
```

jSicko generates the following error:

```
InstanceClauseOnStaticMethod.java:33: error: [jsicko] non-static clause Postcondition clause instance_clause in InstanceClauseOnStaticMethod#staticMethod(int) is not compatible with static method staticMethod.
    public static void staticMethod(int i) {
                       ^
```

Note that the opposite is generally allowed: A static clause can be used on an instance method, even if the clause method cannot reference `this`.

### Returns on void Method

This error is generated when a clause mentioning returns is used on a void method:

```java
  @Pure
  boolean returns_something(Object returns) { ... }

  @Ensures("returns_something")
  public void voidMethod() { ... }
```

This is the corresponding error message:

```
ReturnsParamOnVoidMethod.java:33: error: [jsicko] Use of returns param on void method for clause returns_something(java.lang.Object returns) in ReturnsParamOnVoidMethod#voidMethod()
    public void voidMethod() {
                ^
```

### Returns/Raises on Precondition

`returns` and `raises` variables are meant to be used in postconditions.

If a clause mentioning such variables is used in a precondition, it generates an error:

```java
  @Pure
  boolean raises_something(RuntimeException raises) { ... }

  @Requires("raises_something")
  public Object someMethod() { ... }
```

```
RaisesParamOnPrecondition.java:33: error: [jsicko] Use of raises param on precondition for clause raises_something(java.lang.RuntimeException raises) in RaisesParamOnPrecondition#voidMethod()
        public void voidMethod() {
                      ^
```
       
### Missing Param Name and Wrong Param Type

These errors are generated when the clause mentions a parameter that does not exist in the method where it is used, or when the types are not compatible.

For example, this snippet:
```java
  boolean clause(double i) {
    return i > 0.0;
  }

  @Pure
  @Ensures("clause")
  public void method(double f) { ... }
```

generates the following error:

```
MissingClauseParameter.java:33: error: [jsicko] Missing param name i for clause clause(double i) in MissingClauseParameter#method(double f)
    public void method(double f) {
                ^
```

### Invariant with Parameters

Invariant clauses must have no parameter. If by mistake you annotate a method with parameters as a class invariant, jSicko generates a compiler error. In the following case:

```java 
  @Invariant
  @Pure
  public boolean invariant_with_param(int value) {
    return value < 3;
  }
```

jSicko generates the following error:
```
InvariantWithParameter.java:29: error: [jsicko] Invariant invariant_with_param has parameters, should have none.
    public boolean invariant_with_param(int value) {
                   ^
```

### Static Invariants

Invariants in jSicko cannot be static, as they represent invariants related to instance fields (also called representation invariants).

### StackOverflow Errors

This is probably the most complicated error that can appear when invoking  methods instrumented with jSicko.
The issue is related to the mechanisms used to save the state of `old` variables, and in particular `old(this)`. jSicko uses a binary serialization framework, [kryo](https://github.com/EsotericSoftware/kryo), which is able to
serialize arbitrary objects in Java.

For some data types, like Java collections, it provides custom serialization and deserialization methods that invoke instance methods on the collection. A StackOverflow error may be thrown when some of the observer methods (*queries*) used in serialization are not declared as `@Pure`, and thus they are in turn instrumented by jSicko. This typically causes a non-trivial infinite recursion.

In such cases, look at the stack trace and try to find such instance methods that are called recursively, and ensure that all observer methods are `@Pure`.


