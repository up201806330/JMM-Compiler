
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

import org.junit.Assert;
import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OptimizeTest {

    @Test
    public void testHelloWorld() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testSimple() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testOperatorPrecedence() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/OperatorPrecedence.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testFac() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Fac.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testFindMaximum() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testLazysort() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testLife() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Life.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testMonteCarloPi() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    // Doesnt have main (isnt supposed to be ran so its fine)
//    @Test
//    public void testMyClass() {
//        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/MyClass.jmm"));
//        TestUtils.noErrors(result.getReports());
//    }

    @Test
    public void testQuickSort() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testTicTacToe() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testWhileAndIf() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    // OPTIMIZATIONS //

    @Test
    public void testHelloWorldDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/HelloWorld.jmm")), false,
                1);
        TestUtils.noErrors(result.getReports());

        var output = TestUtils.backend(result).run();
        assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testSimpleDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Simple.jmm")), false,
                3);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Simple.jmm")), false,
                2);
        TestUtils.mustFail(fail.getReports());

        var output = TestUtils.backend(result).run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testOperatorPrecedenceDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/OperatorPrecedence.jmm")), false,
                3);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/OperatorPrecedence.jmm")), false,
                2);
        TestUtils.mustFail(fail.getReports());

        var output = TestUtils.backend(result).run();
        assertEquals(String.valueOf(1 * 3 - 4 / 5 * (2-5)), output.trim());
    }

    @Test
    public void testFacDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Fac.jmm")), false,
                1);
        TestUtils.noErrors(result.getReports());

        var output = TestUtils.backend(result).run();
        assertEquals("3628800", output.trim()); // 10! = 3628800
    }

    @Test
    public void testFindMaximumDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/FindMaximum.jmm")), false,
                3);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/FindMaximum.jmm")), false,
                2);
        TestUtils.mustFail(fail.getReports());

        var output = TestUtils.backend(result).run();
        assertEquals("Result: 28", output.trim());
    }

    @Test
    public void testLazysortDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Lazysort.jmm")), false,
                4);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Lazysort.jmm")), false,
                3);
        TestUtils.mustFail(fail.getReports());

        // Val always different
    }

    @Test
    public void testLifeDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Life.jmm")), false,
                8);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Life.jmm")), false,
                7);
        TestUtils.mustFail(fail.getReports());

        // Infinite loop is breaking
    }

    @Test
    public void testMonteCarloPiDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm")), false,
                4);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm")), false,
                3);
        TestUtils.mustFail(fail.getReports());

        var output = TestUtils.backend(result).run("999999").trim(); // Max iterations allowed
        assertTrue(output.equals("Insert number: Result: 314") || output.equals("Insert number: Result: 315"));
    }

    @Test
    public void testQuickSortDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/QuickSort.jmm")), false,
                4);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/QuickSort.jmm")), false,
                3);
        TestUtils.mustFail(fail.getReports());

        var output = TestUtils.backend(result).run();
        assertEquals(("1\n2\n3\n4\n5\n6\n7\n8\n9\n10").replaceAll("\\n|\\r\\n", System.getProperty("line.separator")),
                output.trim());
    }

    @Test
    public void testTicTacToeDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/TicTacToe.jmm")), false,
                7);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/TicTacToe.jmm")), false,
                6);
        TestUtils.mustFail(fail.getReports());

        // Infinite loop is breaking
    }

    @Test
    public void testWhileAndIfDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm")), false,
                5);
        TestUtils.noErrors(result.getReports());
        var fail = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm")), false,
                4);
        TestUtils.mustFail(fail.getReports());

        var output = TestUtils.backend(result).run();
        assertEquals("10\n10\n10\n10\n10\n10\n10\n10\n10\n10".replaceAll("\\n|\\r\\n", System.getProperty("line.separator")),
                output.trim());
    }

    @Test
    public void testHelloWorldDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/HelloWorld.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testSimpleDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Simple.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testOperatorPrecedenceDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/OperatorPrecedence.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testFacDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Fac.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testFindMaximumDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/FindMaximum.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testLazysortDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Lazysort.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testLifeDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Life.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testMonteCarloPiDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testQuickSortDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/QuickSort.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testTicTacToeDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/TicTacToe.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testWhileAndIfDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }
}
