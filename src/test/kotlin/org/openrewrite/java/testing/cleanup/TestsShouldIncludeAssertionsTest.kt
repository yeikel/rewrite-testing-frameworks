/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class TestsShouldIncludeAssertionsTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .classpath("junit", "mockito-all", "hamcrest", "assertj-core")
        .dependsOn(
            listOf(
                Parser.Input.fromString(
                    """
                        package org.learning.math;
                        public interface MyMathService {
                            Integer addIntegers(String i1, String i2);
                        }
                    """
                )
            )
            )
        .build()

    override val recipe: Recipe
        get() = TestsShouldIncludeAssertions()

    @Test
    fun noAssertions() = assertChanged(
        before = """
            import org.junit.jupiter.api.Test;
            public class AaTest {
                @Test
                public void methodTest() {
                    Integer it = Integer.valueOf("2");
                    System.out.println(it);
                }
            }
        """,
        after = """
            import org.junit.jupiter.api.Test;
            
            import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
            
            public class AaTest {
                @Test
                public void methodTest() {
                    assertDoesNotThrow(() -> {
                        Integer it = Integer.valueOf("2");
                        System.out.println(it);
                    });
                }
            }
        """
    )

    @Test
    fun hasAssertDoesNotThrowAssertion() = assertUnchanged(
        before = """
            import org.junit.jupiter.api.Test;
            
            import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
            
            public class AaTest {
            
                @Test
                public void methodTest() {
                    assertDoesNotThrow(() -> {
                        Integer it = Integer.valueOf("2");
                        System.out.println(it);
                    });
                }
            }
        """
    )
    @Test
    fun assertJAssertion() = assertUnchanged(
        before = """
            import org.junit.jupiter.api.Test;

            import static org.assertj.core.api.Assertions.assertThat;

            public class A {

                @Test
                public void test() {
                    assertThat(notification()).isEqualTo(1);
                }
                private Integer notification() {
                    return 1;
                }
            }
        """
    )
    @Test
    fun hamcrestAssertion() = assertUnchanged(
            before = """
            import org.junit.jupiter.api.Test;

            import static org.hamcrest.MatcherAssert.assertThat;
            import static org.hamcrest.Matchers.*;
            
            public  class ATest {
                @Test
                public void methodTest() {
                    Integer i1 = Integer.valueOf("1");
                    Integer i2 = Integer.valueOf("1");
                    assertThat(i1, equalTo(i2));
                }
            }
        """
    )

    @Test
    fun hasAssertion() = assertUnchanged(
        before = """
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;
            public class AaTest {
                @Test
                public void methodTest() {
                    assertEquals(1,1);
                }
            }
        """
    )

    @Test
    fun hasMockitoVerify() = assertUnchanged(
        before = """
            import org.junit.jupiter.api.Test;
            import org.mockito.Mock;
            import org.learning.math.MyMathService;
            import static org.mockito.Mockito.when;
            import static org.mockito.Mockito.times;
            import static org.mockito.Mockito.verify;
            
            class AaTest {
                @Mock
                MyMathService myMathService;
                
                @Test
                public void verifyTest() {
                    when(myMathService.addIntegers("1", "2")).thenReturn(3);
                    Integer i = myMathService.addIntegers("1", "2");
                    verify(myMathService, times(1));
                }
            }
        """
    )

    @Test
    fun hasMockitoDoesNotValidate() = assertChanged(
        before = """
            import org.junit.jupiter.api.Test;
            import org.mockito.Mock;
            import org.learning.math.MyMathService;
            import static org.mockito.Mockito.when;
            import org.learning.math.Stuff;

            class AaTest {
                @Mock
                MyMathService myMathService;

                @Test
                public void methodTest() {
                    when(myMathService.addIntegers("1", "2")).thenReturn(3);
                    myMathService.addIntegers("1", "2");
                }
            }
        """,
        after = """
            import org.junit.jupiter.api.Test;
            import org.mockito.Mock;
            import org.learning.math.MyMathService;
            
            import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
            import static org.mockito.Mockito.when;
            import org.learning.math.Stuff;

            class AaTest {
                @Mock
                MyMathService myMathService;

                @Test
                public void methodTest() {
                    assertDoesNotThrow(() -> {
                        when(myMathService.addIntegers("1", "2")).thenReturn(3);
                        myMathService.addIntegers("1", "2");
                    });
                }
            }
        """
    )
}
