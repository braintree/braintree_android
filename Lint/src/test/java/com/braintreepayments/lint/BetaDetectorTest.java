package com.braintreepayments.lint;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BetaDetectorTest extends LintDetectorTest {
    @Override
    protected Detector getDetector() {
        return new BetaDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(BetaDetector.ISSUE);
    }

    public void testBetaMethod() throws Exception {
        BetaDetector.METHODS = Collections.singletonList("fooBar");
        String result = lintProject(
                java("src/com/example/Foo.java", "package com.example;\n" +
                        "public class Foo {\n" +
                        "  public void someMethod() {\n" +
                        "    Baz baz = new Baz();\n" +
                        "    baz.fooBar();" +
                        "  }\n" +
                        "}"),
                java("src/com/example/Baz.java", "package com.example;\n" +
                        "public class Baz {\n" +
                        "  public void fooBar() {\n" +
                        "  }\n" +
                        "}"));

        assertThat(result).isEqualTo(
                "src/com/example/Foo.java:5: Error: API is in beta [com.braintreepayments.beta]\n" +
                        "    baz.fooBar();  }\n" +
                        "    ~~~~~~~~~~~~\n" +
                        "1 errors, 0 warnings\n");
    }

    public void testNormalMethod() throws Exception {
        BetaDetector.METHODS = Collections.singletonList("fooBar");
        String result = lintProject(
                java("src/com/example/Foo.java", "package com.example;\n" +
                        "public class Foo {\n" +
                        "  public void someMethod() {\n" +
                        "    Baz baz = new Baz();\n" +
                        "    baz.fooBarBaz();" +
                        "  }\n" +
                        "}"),
                java("src/com/example/Baz.java", "package com.example;\n" +
                        "public class Baz {\n" +
                        "  public void fooBarBaz() {\n" +
                        "  }\n" +
                        "}"));

        assertThat(result).isEqualTo("No warnings.");
    }
}