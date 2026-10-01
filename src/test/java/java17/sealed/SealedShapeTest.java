package java17.sealed;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SealedShapeTest {

    @Test
    void should_exposeTheClosedSetOfSubtypes_when_inspectingASealedInterface() {
        assertTrue(Shape.class.isSealed());
        assertEquals(
                Set.of(Circle.class, Square.class, Polygon.class),
                Set.of(Shape.class.getPermittedSubclasses()));
    }

    @Test
    void should_notBeSealed_when_subtypeIsDeclaredNonSealed() {
        assertFalse(Polygon.class.isSealed());
    }

    @Test
    void should_stillBeAShape_when_extendingANonSealedSubtype() {
        Shape triangle = new Triangle(4, 3);

        assertInstanceOf(Polygon.class, triangle);
        assertFalse(Set.of(Shape.class.getPermittedSubclasses()).contains(Triangle.class));
    }

    @Test
    void should_computeAreaForEveryPermittedSubtype_when_switchHasNoDefault() {
        assertEquals(Math.PI, Areas.of(new Circle(1)), 1e-9);
        assertEquals(9.0, Areas.of(new Square(3)), 1e-9);
        assertEquals(6.0, Areas.of(new Triangle(4, 3)), 1e-9);
    }
}
