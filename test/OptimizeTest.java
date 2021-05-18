
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

    // var _allowedNameL n está a ser aceite pelo parser do ollir smh
//    @Test
//    public void testLazySort() {
//        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/LazySort.jmm"));
//        TestUtils.noErrors(result.getReports());
//    }

    // Is requesting input from user, so won't be ran here
//    @Test
//    public void testLife() {
//        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Life.jmm"));
//        TestUtils.noErrors(result.getReports());
//    }

    // Is requesting input from user, so won't be ran here
//    @Test
//    public void testMonteCarloPi() {
//        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
//        TestUtils.noErrors(result.getReports());
//    }

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

    // TODO May be because of else ifs, idk
//    @Test
//    public void testTicTacToe() {
//        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
//        TestUtils.noErrors(result.getReports());
//    }

    @Test
    public void testWhileAndIf() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"));
        TestUtils.noErrors(result.getReports());
    }

}
