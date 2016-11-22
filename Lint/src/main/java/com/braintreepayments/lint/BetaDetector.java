package com.braintreepayments.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import java.util.Collections;
import java.util.List;

public class BetaDetector extends Detector implements Detector.JavaPsiScanner {
    // This would normally be private final, but has exposed here for testing purposes, since there
    // seems to be no way to inject it via the Detector constructor using LintDetectorTest
    static List<String> METHODS = Collections.emptyList();
    private static final Implementation IMPLEMENTATION = new Implementation(BetaDetector.class,
            Scope.JAVA_FILE_SCOPE);
    static final Issue ISSUE = Issue.create(
            "com.braintreepayments.beta",
            "API is in beta",
            "This API is currently in beta and subject to change. Braintree makes no guarantees about " +
                    "future compatibility or existence of this API.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPLEMENTATION);

    public BetaDetector() {
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return METHODS;
    }

    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor,
            PsiMethodCallExpression node, PsiMethod method) {
        if (METHODS.contains(method.getName())) {
            context.report(ISSUE, node, context.getLocation(node), ISSUE.getBriefDescription(
                    TextFormat.TEXT));
        }
    }
}
