
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;


public class BackendTest {

    @Test
    public void testHelloWorld() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testSimple() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testOperatorPrecedence() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/OperatorPrecedence.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals(String.valueOf(1 * 3 - 4 / 5 * (2-5)), output.trim());
    }

    @Test
    public void testFac() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Fac.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("3628800", output.trim()); // 10! = 3628800
    }

    @Test
    public void testFindMaximum() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 28", output.trim());
    }

    // Is always different, not worth checking values
    @Test
    public void testLazySort() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/LazySort.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    // Infinite loop is breaking
//    @Test
//    public void testLife() {
//        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Life.jmm"));
//        TestUtils.noErrors(result.getReports());
//
//        var output = result.run("1");
//        assertEquals(SpecsStrings.normalizeFileContents("0010000000\n1010000000\n0110000000\n0000000000\n0000000000\n0000000000\n0000000000\n0000000000\n0000000000\n0000000000\n\n1\n1\n0100000000\n0011000000\n0110000000\n0000000000\n0000000000\n0000000000\n0000000000\n0000000000\n0000000000\n0000000000\n").replaceAll("\\n|\\r\\n", System.getProperty("line.separator")), output.trim());
//    }

    @Test
    public void testMonteCarloPi() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run("999999").trim(); // Max iterations allowed
        assertTrue(output.equals("Insert number: Result: 314") || output.equals("Insert number: Result: 315"));
    }

    @Test
    public void testQuickSort() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals(("1\n2\n3\n4\n5\n6\n7\n8\n9\n10").replaceAll("\\n|\\r\\n", System.getProperty("line.separator")),
                output.trim());
    }

    // Infinite loop is breaking
//    @Test
//    public void testTicTacToe() {
//        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
//        TestUtils.noErrors(result.getReports());
//
//        var output = result.run(SpecsIo.getResource("test/fixtures/public/TicTacToe.input"));
//        assertEquals(("0|0|0\n" +
//                        "- - -\n" +
//                        "0|0|0\n" +
//                        "- - -\n" +
//                        "0|0|0\n" +
//                        "\n" +
//                        "Player 1 turn! Enter the row(0-2):" +
//                        "Enter the column(0-2):" +
//                        "\n" +
//                        "1|0|0\n" +
//                        "- - -\n" +
//                        "0|0|0\n" +
//                        "- - -\n" +
//                        "0|0|0\n" +
//                        "\n" +
//                        "Player 2 turn! Enter the row(0-2):\n").replaceAll("\\n|\\r\\n", System.getProperty("line.separator")),
//                output.trim());
//    }

    @Test
    public void testWhileAndIf() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("10\n10\n10\n10\n10\n10\n10\n10\n10\n10".replaceAll("\\n|\\r\\n", System.getProperty("line.separator")),
                output.trim());
    }

    @Test
    public void testPrime() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/custom/Prime.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run("10");
        assertEquals("2\n3\n5\n7".replaceAll("\\n|\\r\\n", System.getProperty("line.separator")),
                output.trim());
    }

    @Test
    public void testArrayAccess() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/custom/ArrayAccess.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("2", output.trim());
    }
}
