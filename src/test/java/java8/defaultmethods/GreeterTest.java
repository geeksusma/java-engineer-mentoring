package java8.defaultmethods;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreeterTest {

    @Test
    void should_useTheInterfaceDefault_when_theImplementationNeverOverridesGreet() {
        assertEquals("Hello, Ana!", new SimpleGreeter("Ana").greet());
    }

    @Test
    void should_reuseTheDefaultViaSuper_when_theOverrideAdaptsItsResult() {
        assertEquals("HELLO, ANA!", new LoudGreeter("Ana").greet());
    }

    @Test
    void should_beUsableAsALambda_when_createdThroughTheStaticFactoryMethod() {
        Greeter shouting = Greeter.uppercase("bob");

        assertEquals("BOB", shouting.name());
        assertEquals("Hello, BOB!", shouting.greet());
    }
}
