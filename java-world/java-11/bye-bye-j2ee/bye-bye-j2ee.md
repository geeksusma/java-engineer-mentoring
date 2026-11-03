# Bye bye J2EE

Java 9 introduced the module system (JPMS) and split the monolithic JDK into modules. Java 11 used that split to finish a cleanup that had been signposted since Java 9: the Java EE and CORBA technologies that used to ship inside the JDK — `java.xml.bind` (JAXB), `java.xml.ws` (JAX-WS), `java.activation` (JAF), `java.xml.ws.annotation`, `java.corba`, and `java.transaction` (JTA) — were **removed entirely**. Code that used to compile "for free" against `javax.xml.bind.JAXBContext` or `org.omg.CORBA.ORB` now needs an explicit third-party dependency, exactly like any other library.

```java
// Compiled fine on Java 8. On Java 11+, this doesn't compile
// unless a JAXB implementation is added as a dependency.
JAXBContext context = JAXBContext.newInstance(Order.class);
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java11/j2ee`](../../../src/test/java/java11/j2ee) (classes under `src/main/java/java11/j2ee`). Run them with `mvn test`.

---

## Why these modules existed in the JDK at all

Java EE (JAXB, JAX-WS, JAF, CORBA, JTA) was bundled into the JDK back when the platform's ambition was "Java is the enterprise stack, batteries included." By the mid-2010s that made less and less sense:

- These APIs had their own independent standards and release cycles, but were stuck shipping on the JDK's schedule.
- Most of the ecosystem had already moved to actively-maintained third-party implementations (`org.glassfish.jaxb`, Apache CXF, gRPC instead of CORBA...).
- Bundling them forced every JRE/JDK download to carry technologies most projects — especially non-enterprise ones — never touched.

JPMS gave the JDK team a mechanical way to draw a hard boundary around these modules, deprecate them (Java 9), and then delete them (Java 11).

---

## How code has to adapt

If a project actually used one of these APIs, the fix is mechanical: add the equivalent artifact as a normal Maven/Gradle dependency instead of relying on the JDK to provide it.

```xml
<!-- javax.xml.bind.* is gone from the JDK — bring your own JAXB impl -->
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>2.3.9</version>
</dependency>
```

Nothing about the *API* changed — `JAXBContext`, `Marshaller`, `Service`, `DataHandler` all still exist and still work the same way once the dependency is on the classpath. What changed is *where they come from*: the JDK no longer provides them implicitly.

Projects that never touched these APIs — the overwhelming majority of Spring Boot, plain REST, or batch-processing applications — are completely unaffected. This removal was invisible to them.

---

## Proving the removal empirically

`Class.forName("fully.qualified.Name")` throws `ClassNotFoundException` when a class isn't on the classpath/module path. Since this project has no external dependency providing any of these APIs, attempting to load them on Java 25 (which inherited this removal from Java 11 onward) fails exactly as it would on a Java 11 runtime:

```java
public static boolean isClassAvailable(String fullyQualifiedName) {
    try {
        Class.forName(fullyQualifiedName);
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}
```

```java
assertFalse(isClassAvailable("javax.xml.bind.JAXBContext"));  // JAXB — gone
assertFalse(isClassAvailable("org.omg.CORBA.ORB"));            // CORBA — gone
assertTrue(isClassAvailable("java.util.List"));                 // java.base — still here
```

---

## Summary

| Module removed in Java 11 | Technology | Use case | Replacement |
|---|---|---|---|
| `java.xml.bind` | JAXB (XML binding) | Converting Java objects to/from XML (marshalling/unmarshalling), commonly for SOAP payloads and XML data interchange | `org.glassfish.jaxb:jaxb-runtime` or similar |
| `java.xml.ws` | JAX-WS (SOAP web services) | Building and consuming SOAP-based web services from a WSDL contract | Apache CXF, Metro |
| `java.activation` | JAF (JavaBeans Activation Framework) | Handling typed data by MIME type — notably used by JavaMail to manage email attachments | `com.sun.activation:javax.activation` |
| `java.xml.ws.annotation` | Common annotations for JAX-WS | Shared annotations used by JAX-WS (and other Java EE specs) to wire up resources and generated code | Bundled with the JAX-WS replacement |
| `java.corba` | CORBA | Language-agnostic distributed object communication / remote method invocation across platforms, used in legacy enterprise systems | Rarely replaced directly — usually a sign of migrating off CORBA entirely |
| `java.transaction` | JTA (distributed transactions) | Coordinating a distributed transaction across multiple resources (e.g. two databases, or a database plus a message queue) as one atomic unit of work | `javax.transaction:javax.transaction-api` |

| Before Java 11 | Java 11 onward |
|---|---|
| These APIs were part of the JDK — no extra dependency needed | These APIs must be added explicitly, like any other library |
| `java -version` bundled Java EE regardless of whether a project used it | The JDK only ships `java.base` and the modules a project actually needs |
