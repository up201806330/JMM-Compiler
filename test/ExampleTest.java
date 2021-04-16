import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {
    // Os testes aqui feitos testam se o programa dá error
    // e se o número de erros é o correto (número correto de erros foi calculado manualmente).

    @Test
    public void testSimple() {
        String file = SpecsIo.read("test/fixtures/public/Simple.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testFindMaximum() {
        String file = SpecsIo.read("test/fixtures/public/FindMaximum.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testHelloWorld() {
        String file = SpecsIo.read("test/fixtures/public/HelloWorld.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testLazySort() {
        String file = SpecsIo.read("test/fixtures/public/Lazysort.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testLife() {
        String file = SpecsIo.read("test/fixtures/public/Life.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testMonteCarloPi() {
        String file = SpecsIo.read("test/fixtures/public/MonteCarloPi.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testQuickSort() {
        String file = SpecsIo.read("test/fixtures/public/QuickSort.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testTicTacToe() {
        String file = SpecsIo.read("test/fixtures/public/TicTacToe.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testWhileAndIF() {
        String file = SpecsIo.read("test/fixtures/public/WhileAndIF.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testBlowUp() {
        String file = SpecsIo.read("test/fixtures/public/fail/syntactical/BlowUp.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.mustFail(result.getReports());
            assertEquals(4, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testCompleteWhileTest() {
        String file = SpecsIo.read("test/fixtures/public/fail/syntactical/CompleteWhileTest.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.mustFail(result.getReports());
            assertEquals(11, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testLengthError() {
        String file = SpecsIo.read("test/fixtures/public/fail/syntactical/LengthError.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testMissingRightPar() {
        String file = SpecsIo.read("test/fixtures/public/fail/syntactical/MissingRightPar.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testMultipleSequential() {
        String file = SpecsIo.read("test/fixtures/public/fail/syntactical/MultipleSequential.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.mustFail(result.getReports());
            assertEquals(2, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testNesteLoop() {
        String file = SpecsIo.read("test/fixtures/public/fail/syntactical/NestedLoop.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.mustFail(result.getReports());
            assertEquals(2, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void testOperatorPrecedence() {
        String file = SpecsIo.read("test/fixtures/public/OperatorPrecedence.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }

    // SEMANTIC //

    @Test
    public void testArrIndexNotInt() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/arr_index_not_int.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 error, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    @Test
    public void testBadArguments() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/badArguments.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(2, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 2 error, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    @Test
    public void testArrSizeNotInt() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/arr_size_not_int.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 error, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    @Test
    public void testBinopIncomp() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/binop_incomp.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 error, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    @Test
    public void testFuncNotFound() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/funcNotFound.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 error, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    @Test
    public void testSimpleLength() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/simple_length.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(2, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 2 errors, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    @Test
    public void testVarExpIncomp() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/var_exp_incomp.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 error, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    @Test
    public void testVarLitIncomp() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/var_lit_incomp.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.mustFail(result.getReports());
            assertEquals(1, TestUtils.getNumErrors(result.getReports()));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 error, got " + TestUtils.getNumErrors(result.getReports()));
        }
    }

    // EXTRAS //

    @Test
    public void testUndef() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/var_undef.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.noErrors(result.getReports());
            assertEquals(1, TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 warning, got " + TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        }
    }

    @Test
    public void testNotInit() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/varNotInit.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.noErrors(result.getReports());
            assertEquals(1, TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 warning, got " + TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        }
    }

    @Test
    public void testMissType() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/extra/miss_type.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertEquals(1, TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 warning, got " + TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        }
    }

    @Test
    public void testRepeatName() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/extra/repeatName.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.noErrors(result.getReports());
            assertEquals(1, TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 warning, got " + TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        }
    }

    @Test
    public void testStatic() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/extra/static.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.noErrors(result.getReports());
            assertEquals(1, TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 warning, got " + TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        }
    }

    @Test
    public void testOverloads() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/extra/overloads.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.noErrors(result.getReports());
            assertEquals(0, TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected no warnings or errors, got " + result.getReports().size());
        }
    }

    @Test
    public void testReturnTypeMismatch() {
        String file = SpecsIo.read("test/fixtures/public/fail/semantic/extra/returns.jmm");
        JmmSemanticsResult result = TestUtils.analyse(file);
        try{
            TestUtils.noErrors(result.getReports());
            assertEquals(1, TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        } catch (RuntimeException | AssertionError e) {
            fail("Expected 1 warning, got " + TestUtils.getNumReports(result.getReports(), ReportType.WARNING));
        }
    }
}
