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
package org.openrewrite.java.testing.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class AssertTrueComparisonToAssertEqualsTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("junit-jupiter-api")
        .build()

    override val recipe: Recipe
        get() = AssertTrueComparisonToAssertEquals()

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/204")
    @Suppress("ConstantConditions", "SimplifiableAssertion")
    @Test
    fun assertTrueComparisonToAssertEqualsTest() = assertChanged(
        before = """
            import static org.junit.jupiter.api.Assertions.assertTrue;
            
            public class Test {
                void test() {
                    int a = 1;
                    int b = 1;
                    assertTrue(a == b);
                }
            }
        """,
        after = """
            import static org.junit.jupiter.api.Assertions.assertEquals;
            
            public class Test {
                void test() {
                    int a = 1;
                    int b = 1;
                    assertEquals(a, b);
                }
            }
        """,
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/204")
    @Suppress("ConstantConditions", "SimplifiableAssertion")
    @Test
    fun preserveStyleOfStaticImportOrNot() = assertChanged(
        before = """
            import org.junit.jupiter.api.Assertions;
            
            public class Test {
                void test() {
                    int a = 1;
                    int b = 1;
                    Assertions.assertTrue(a == b);
                }
            }
        """,
        after = """
            import org.junit.jupiter.api.Assertions;
            
            public class Test {
                void test() {
                    int a = 1;
                    int b = 1;
                    Assertions.assertEquals(a, b);
                }
            }
        """,
    )
}
