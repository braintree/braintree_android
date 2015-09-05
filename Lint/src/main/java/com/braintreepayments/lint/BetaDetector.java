package com.braintreepayments.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.TextFormat;

import java.util.Arrays;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ClassDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;

public class BetaDetector extends Detector implements JavaScanner {

    private static final List<String> METHODS = Arrays.asList(
            "isThreeDSecureEnabled",
            "startThreeDSecureVerification",
            "finishThreeDSecureVerification",
            "getThreeDSecureInfo",
            "isAndroidPayEnabled",
            "isMaskedWalletResponse",
            "isFullWalletResponse",
            "getAndroidPayTokenizationParameters",
            "getAndroidPayGoogleTransactionId",
            "performAndroidPayMaskedWalletRequest",
            "performAndroidPayChangeMaskedWalletRequest",
            "performAndroidPayFullWalletRequest",
            "getNonceFromAndroidPayFullWalletResponse",
            "getAndroidPay"
    );

    private static final Implementation IMPLEMENTATION = new Implementation(
            BetaDetector.class,
            Scope.JAVA_FILE_SCOPE);

    public static final Issue ISSUE = Issue.create(
            "com.braintreepayments.beta",
            "API is in beta",
            "This API is currently in beta and subject to change. Braintree makes no guarantees about future compatibility or existence of this API.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPLEMENTATION);

    public BetaDetector() {}

    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return METHODS;
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        if (METHODS.contains(node.astName().astValue())) {
            context.report(ISSUE, node, context.getLocation(node), ISSUE.getBriefDescription(TextFormat.TEXT));
        }
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return null;
    }

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return null;
    }

    @Override
    public boolean appliesToResourceRefs() {
        return false;
    }

    @Override
    public void visitResourceReference(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull Node node, @NonNull String type, @NonNull String name, boolean isFramework) {
    }

    @Override
    public List<String> applicableSuperClasses() {
        return null;
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull ClassDeclaration node,
            @NonNull ResolvedClass resolvedClass) {
    }
}
