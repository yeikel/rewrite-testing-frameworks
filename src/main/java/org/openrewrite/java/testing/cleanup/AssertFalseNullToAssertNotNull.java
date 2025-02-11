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
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.function.Supplier;

public class AssertFalseNullToAssertNotNull extends Recipe {
    private static final MethodMatcher ASSERT_FALSE = new MethodMatcher(
            "org.junit.jupiter.api.Assertions assertFalse(..)");

    @Override
    public String getDisplayName() {
        return "Replace JUnit `assertFalse(a == null)` to `assertNotNull(a)`";
    }

    @Override
    public String getDescription() {
        return "Using `assertNotNull(a)` is simpler and more clear.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(ASSERT_FALSE);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        Supplier<JavaParser> javaParser = () -> JavaParser.fromJavaVersion()
                //language=java
                .dependsOn("" +
                        "package org.junit.jupiter.api;" +
                        "public class Assertions {" +
                        "public static void assertNotNull(Object actual) {}" +
                        "}")
                .build();

        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate assertNotNull = JavaTemplate.builder(this::getCursor, "assertNotNull(#{any(java.lang.Object)})")
                    .staticImports("org.junit.jupiter.api.Assertions.assertNotNull")
                    .javaParser(javaParser)
                    .build();

            private final JavaTemplate assertNotNullStaticImport = JavaTemplate.builder(this::getCursor, "Assertions.assertNotNull(#{any(java.lang.Object)})")
                    .imports("org.junit.jupiter.api.Assertions")
                    .javaParser(javaParser)
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (ASSERT_FALSE.matches(method) && isEqualBinary(method)) {

                    J.Binary binary = (J.Binary) method.getArguments().get(0);

                    Expression nonNullExpression = getNonNullExpression(binary);

                    if (method.getSelect() == null) {
                        maybeRemoveImport("org.junit.jupiter.api.Assertions");
                        maybeAddImport("org.junit.jupiter.api.Assertions", "assertNotNull");
                        return method.withTemplate(assertNotNull, method.getCoordinates().replace(), nonNullExpression);
                    } else {
                        return method.withTemplate(assertNotNullStaticImport, method.getCoordinates().replace(), nonNullExpression);
                    }
                }
                return super.visitMethodInvocation(method, ctx);
            }


            private Expression getNonNullExpression(J.Binary binary) {

                if (binary.getRight() instanceof J.Literal){
                    boolean isNull = ((J.Literal) binary.getRight()).getValue() == null;
                    if (isNull){
                        return binary.getLeft();
                    }
                }

                return binary.getRight();
            }

            private boolean isEqualBinary(J.MethodInvocation method) {

                if (method.getArguments().isEmpty()) {
                    return false;
                }

                J.Binary binary = (J.Binary) method.getArguments().get(0);
                J.Binary.Type operator = binary.getOperator();
                return operator.equals(J.Binary.Type.Equal);


            }
        };
    }

}
