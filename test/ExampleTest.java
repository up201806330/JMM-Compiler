import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
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
        String file = SpecsIo.read("test/fixtures/public/fail/syntactical/OperatorPrecedence.jmm");
        JmmParserResult result = TestUtils.parse(file);
        try {
            TestUtils.noErrors(result.getReports());
            assertTrue(true);
        } catch (RuntimeException e) {
            fail();
        }
    }
}
