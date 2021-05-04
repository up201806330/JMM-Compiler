import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.OllirErrorException;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

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

public class BackendStage implements JasminBackend {

    public JasminResult toJasmin(OllirResult ollirResult, int dashR, boolean dashO){
        ClassUnit ollirClass = ollirResult.getOllirClass();

        if (dashR < 1) dashR = 1;
        else if (dashR > 99) dashR = 99;

        try {
            Jasmin jasmin = new Jasmin();
            // Convert the OLLIR to a String containing the equivalent Jasmin code
            String jasminCode = jasmin.getByteCode(ollirClass, dashR, dashO); // Convert node ...
            System.out.println(jasminCode);

            // More reports from this stage
            List<Report> reports = new ArrayList<>();

            return new JasminResult(ollirResult, jasminCode, reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }
    }

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        return toJasmin(ollirResult, 99, false);
    }

}
