package com.braintreepayments.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

import java.util.Arrays;
import java.util.List;

public class LintIssueRegistry extends IssueRegistry {

    public LintIssueRegistry() {}

    @Override
    public List<Issue> getIssues() {
        return Arrays.asList(
                BetaDetector.ISSUE
        );
    }
}
