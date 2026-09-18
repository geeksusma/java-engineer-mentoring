package java11.j2ee;

public final class ClasspathProbe {

    private ClasspathProbe() {
    }

    public static boolean isClassAvailable(String fullyQualifiedName) {
        try {
            Class.forName(fullyQualifiedName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
