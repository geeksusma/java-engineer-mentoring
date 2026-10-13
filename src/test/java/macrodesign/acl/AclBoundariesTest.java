package macrodesign.acl;

import org.junit.jupiter.api.Test;

import static support.DependencyRule.classesIn;

class AclBoundariesTest {

    @Test
    void should_neverSeeLegacyTypes_when_inTheDomain() {
        classesIn("macrodesign.acl.payments").shouldNotDependOn("macrodesign.acl.legacy", "macrodesign.acl.anticorruption");
    }

    @Test
    void should_notKnowAboutUs_when_beingTheLegacySystem() {
        classesIn("macrodesign.acl.legacy").shouldNotDependOn("macrodesign.acl.payments", "macrodesign.acl.anticorruption");
    }
}
