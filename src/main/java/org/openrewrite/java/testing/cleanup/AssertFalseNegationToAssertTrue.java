/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.testing.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.J;

import java.util.function.Supplier;

public class AssertFalseNegationToAssertTrue extends Recipe {
    private static final MethodMatcher ASSERT_FALSE = new MethodMatcher(
            "org.junit.jupiter.api.Assertions assertFalse(..)");

    @Override
    public String getDisplayName() {
        return "Replace JUnit `assertFalse(!<boolean>)` to `assertTrue(<boolean>)`";
    }

    @Override
    public String getDescription() {
        return "Using `assertTrue` is simpler and more clear.";
    }


    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        Supplier<JavaParser> javaParser = () -> JavaParser.fromJavaVersion()
                //language=java
                .dependsOn("" +
                        "package org.junit.jupiter.api;" +
                        "public class Assertions {" +
                        "public static void assertTrue(boolean condition) {}" +
                        "}")
                .build();

        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate assertTrue = JavaTemplate.builder(this::getCursor, "assertTrue(#{any(java.lang.Boolean)})")
                    .staticImports("org.junit.jupiter.api.Assertions.assertTrue")
                    .javaParser(javaParser)
                    .build();

            private final JavaTemplate assertTrueNoStaticImport = JavaTemplate.builder(this::getCursor, "Assertions.assertTrue(#{any(java.lang.Boolean)})")
                    .imports("org.junit.jupiter.api.Assertions")
                    .javaParser(javaParser)
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {

                if (ASSERT_FALSE.matches(method) && isUnaryOperatorNot(method)) {
                    J.Unary unary = (J.Unary) method.getArguments().get(0);

                    if (method.getSelect() == null) {
                        maybeRemoveImport("org.junit.jupiter.api.Assertions");
                        maybeAddImport("org.junit.jupiter.api.Assertions", "assertTrue");

                        return method.withTemplate(assertTrue, method.getCoordinates().replace(), unary.getExpression());
                    } else {
                        return method.withTemplate(assertTrueNoStaticImport, method.getCoordinates().replace(), unary.getExpression());
                    }
                }
                return super.visitMethodInvocation(method, ctx);
            }

            private boolean isUnaryOperatorNot(J.MethodInvocation method) {
                if (!method.getArguments().isEmpty() && method.getArguments().get(0) instanceof J.Unary) {
                    J.Unary unary = (J.Unary) method.getArguments().get(0);
                    return unary.getOperator().equals(J.Unary.Type.Not);
                }

                return false;
            }
        };
    }
}
