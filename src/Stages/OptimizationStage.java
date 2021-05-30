import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

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

public class OptimizationStage implements JmmOptimization {
    private int dashR;

    public OllirResult toOllir(JmmSemanticsResult semanticsResult, boolean optimize, int dashR) {
        this.dashR = dashR;
        if (optimize) return optimize(toOllir(optimize(semanticsResult)));
        else return optimize(toOllir(semanticsResult));
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        JmmNode node = semanticsResult.getRootNode();

        List<Report> reports = semanticsResult.getReports();
        // Convert the AST to a String containing the equivalent OLLIR code
        Ollir ollir = new Ollir();
        String ollirCode = ollir.getCode(node);
        // More reports from this stage
        System.out.println(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, reports);
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult, boolean optimize) {
        if (optimize) return toOllir(optimize(semanticsResult));
        else return toOllir(semanticsResult);
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        // dashO
        //System.out.println("Constant propagation...");
        var node = semanticsResult.getRootNode();
        var constantPropagationVisitor = new ConstantPropagationVisitor();
        List<List<JmmNode>> nodesToDeleteAndAdd = new ArrayList<>();

        do {
            constantPropagationVisitor.atLeastOneChange = false;
            constantPropagationVisitor.visit(node, nodesToDeleteAndAdd);
            var assignmentsToRemove = new HashSet<>(constantPropagationVisitor.tryDeleting(node, nodesToDeleteAndAdd));
            assignmentsToRemove.forEach(JmmNode::delete);
        } while(constantPropagationVisitor.atLeastOneChange);

        var anotherVisitor = new OurVisitor();
        System.out.println(anotherVisitor.visit(node, ""));

        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // dashR
        if (!(new Liveness(dashR).run(ollirResult.getOllirClass()))){
            ollirResult.getReports().add(new Report(
                    ReportType.ERROR,
                    Stage.OPTIMIZATION,
                    -1, -1,
                    "Can't generate code with '" + dashR + "' registers"
            ));
        }
        return ollirResult;
    }

}
