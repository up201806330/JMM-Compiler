
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.StringReader;
import java.util.Scanner;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {
		
		try {
		    Jmm jmm = new Jmm(new StringReader(jmmCode));
    		SimpleNode root = jmm.Program(); // returns reference to root node
            	
    		root.dump(""); // prints the tree on the screen
    	
    		return new JmmParserResult(root, new ArrayList<Report>());
		} catch(ParseException e) {
			throw new RuntimeException("Error while parsing", e);
		}
	}

    public static void main(String[] args) throws FileNotFoundException {
		File testFile = new File("./src/Test.java");
		Scanner myReader = new Scanner(testFile);
		StringBuilder data = new StringBuilder();
		while (myReader.hasNextLine()) {
			data.append(myReader.nextLine());
			data.append('\n');
		}
		myReader.close();
		Main temp = new Main();
		temp.parse(data.toString());
    }


}