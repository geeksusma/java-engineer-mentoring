package java11.varinference;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class VarShowcaseTest {

    @Test
    void should_sumWordLengths_when_iteratingWithVarInEnhancedFor() {
        assertEquals(11, VarShowcase.sumLengthsWithEnhancedFor(List.of("Ana", "Carl", "Dana")));
    }

    @Test
    void should_keepConcreteLinkedHashMapType_when_declaredWithVar() {
        Map<String, Integer> lengths = VarShowcase.lengthsByWord(List.of("Ana", "Bob"));

        assertEquals(Map.of("Ana", 3, "Bob", 3), lengths);
        // var infers the right-hand side's real type (LinkedHashMap), not
        // just the declared interface — proof that it isn't "loose" typing.
        assertInstanceOf(LinkedHashMap.class, lengths);
    }

    @Test
    void should_upperCaseEveryWord_when_lambdaParameterUsesVar() {
        assertEquals(List.of("ANA", "BOB"), VarShowcase.upperCaseAll(List.of("Ana", "Bob")));
    }

    @Test
    void should_exposeAnonymousClassExtraMember_when_localVariableIsDeclaredWithVar() {
        assertEquals("dlrow", VarShowcase.describeUsingAnonymousClassExtraMember("world"));
    }
}
