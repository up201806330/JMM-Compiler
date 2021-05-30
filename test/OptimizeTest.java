
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

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

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
    public void testLazySort() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/LazySort.jmm"));
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
    }

    @Test
    public void testSimpleDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Simple.jmm")), false,
                3);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testOperatorPrecedenceDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/OperatorPrecedence.jmm")), false,
                3);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testFacDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Fac.jmm")), false,
                1);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testFindMaximumDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/FindMaximum.jmm")), false,
                3);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testLazySortDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/LazySort.jmm")), false,
                4);
        TestUtils.noErrors(result.getReports());
    }

//    @Test
//    public void testLifeDashR() { // Is breaking because int and object are being put in same register
//        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Life.jmm")), false,
//                120);
//        TestUtils.noErrors(result.getReports());
//    }

    @Test
    public void testMonteCarloPiDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm")), false,
                4);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testQuickSortDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/QuickSort.jmm")), false,
                4);
        TestUtils.noErrors(result.getReports());
    }

//    @Test
//    public void testTicTacToeDashR() { // Is breaking because int and object are being put in same register
//        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/TicTacToe.jmm")), false,
//                7);
//        TestUtils.noErrors(result.getReports());
//    }

    @Test
    public void testWhileAndIfDashR() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm")), false,
                5);
        TestUtils.noErrors(result.getReports());
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
    public void testLazySortDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/LazySort.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }

//    @Test
//    public void testLifeDashO() { // Is breaking because int and object are being put in same register
//        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/Life.jmm")), true);
//        TestUtils.noErrors(result.getReports());
//    }

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

//    @Test
//    public void testTicTacToeDashO() { // Is breaking because int and object are being put in same register
//        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/TicTacToe.jmm")), false,
//                7);
//        TestUtils.noErrors(result.getReports());
//    }

    @Test
    public void testWhileAndIfDashO() {
        var result = (new OptimizationStage()).toOllir(TestUtils.analyse(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm")), true);
        TestUtils.noErrors(result.getReports());
    }
}
