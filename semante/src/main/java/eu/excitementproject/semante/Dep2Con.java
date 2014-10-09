package eu.excitementproject.semante;

import static org.codehaus.jparsec.Parsers.sequence;
import static org.codehaus.jparsec.Scanners.DOUBLE_QUOTE_STRING;
import static org.codehaus.jparsec.Scanners.INTEGER;
import static org.codehaus.jparsec.Scanners.WHITESPACES;
import static org.codehaus.jparsec.Scanners.isChar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.Cleanup;
import lombok.val;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.functors.Map2;
import org.codehaus.jparsec.functors.Map5;

import eu.excitementproject.eop.transformations.representation.ExtendedNode;

public final class Dep2Con {
	
	/**
	 * Path to the <tt>dep2con</tt> executable.
	 */
	private static final String DEP2CON = "/usr/local/bin/dep2con";
	
	public static final void main(String[] args) throws Dep2ConException {
		val input = 
				"(\"ROOT\"/\"ROOT\"/0 (\"led\"/\"VBD\"/3"
				+ " (\"band\"/\"NN\"/2 \"the\"/\"DT\"/1)"
				+ " (\"form\"/\"VBN\"/6 \"zeppelin\"/\"NNP\"/4 \"be\"/\"VBD\"/5"
				+ " (\"in\"/\"IN\"/7 \"1980\"/\"CD\"/8)) \".\"/\".\"/9))";
		System.err.println(input);
		val output = dep2con(input);
		System.err.println(output);
		System.err.println(pConTree.parse(output));
	}
	
	public static final ConTree dep2con(ExtendedNode tree) throws Dep2ConException {
		System.err.println(show(tree).replaceAll("\"", "\\\\\""));
		return pConTree.parse(dep2con(show(tree)));
 	}
	
	public static final String dep2con(String input) throws Dep2ConException {
		
		try {
			// create a new process calling 'dep2con'.
			val proc = Runtime.getRuntime().exec(DEP2CON);

			// create a writer for its stdin and write the input.
			@Cleanup
			val procIn = new PrintWriter(new OutputStreamWriter(
					proc.getOutputStream(), StandardCharsets.UTF_8));
			procIn.println(input);
			procIn.close();

			// wait for the process to finish.
			val exitCode = proc.waitFor();

			// if the command succeeded, read and return the output.
			if (exitCode == 0) {

				@Cleanup
				val procOut = new BufferedReader(new InputStreamReader(
						proc.getInputStream(), StandardCharsets.UTF_8));

				String line;
				val output = new StringBuilder();
				while ((line = procOut.readLine()) != null) {
					output.append(line);
				}
				return output.toString();
			}

			// otherwise read the stderr and throw an exception.
			else {

				@Cleanup
				val procErr = new BufferedReader(new InputStreamReader(
						proc.getErrorStream(), StandardCharsets.UTF_8));

				String line;
				val error = new StringBuilder();
				while ((line = procErr.readLine()) != null) {
					error.append(line);
				}
				throw new Dep2ConException(error.toString());
			}
			
		} catch (InterruptedException e) {
			throw new Dep2ConException(e);
		} catch (IOException e) {
			throw new Dep2ConException(e);
		}	
	}
	
	
	////////// PARSE TREES //////////
	
	
	private static final Parser<ConTree> pConTree = mkPConTree();
	
	private static final Parser<ConTree> mkPConTree() {
		val pRef = Parser.<ConTree> newReference();
		val pLeaf = sequence
				( DOUBLE_QUOTE_STRING
				, isChar('/')
				, DOUBLE_QUOTE_STRING
				, isChar('/')
				, INTEGER
				, new Map5<String,Void,String,Void,String,ConTree>() {
					
				@Override
				public final ConTree map(
					String word, Void _1, String pos, Void _2, String serial) {
					return new ConTree.Leaf(word, pos, Integer.valueOf(serial));
				}
		}).followedBy(WHITESPACES.skipMany());
		val pNode = sequence
				( DOUBLE_QUOTE_STRING.followedBy(WHITESPACES.skipMany1())
				, pRef.lazy().many()
				, new Map2<String, List<ConTree>, ConTree>() {

				@Override
				public ConTree map(String pos, List<ConTree> children) {
					return new ConTree.Node(pos, children);
				}
					
				}).between(isChar('('), isChar(')'))
				  .followedBy(WHITESPACES.skipMany());
		val pTree = pLeaf.or(pNode);
		pRef.set(pTree);
		return pTree;
	}
	
	
	////////// PRINT TREES ////////// 
	
	
	// print a tree in the format required by dep2con.
	private static final String show(ExtendedNode tree) {
		
		val wordInfo = tree.getInfo().getNodeInfo().getWordLemma();
		val word     = quoteString(wordInfo == null ? "ROOT" : wordInfo); 
		val posInfo  = tree.getInfo().getNodeInfo().getSyntacticInfo().getPartOfSpeech();
		val pos      = quoteString(posInfo == null ? "ROOT" : posInfo.getStringRepresentation()); 
		val serial   = tree.getInfo().getNodeInfo().getSerial();	
		val repr     = String.format("%s/%s/%d", word, pos, serial);
		
		if (tree.hasChildren()) {
			val builder = new StringBuilder();
			builder.append('(');
			builder.append(repr);
			for (val child : tree.getChildren())
			{
				builder.append(' ');
				builder.append(show(child));
			}
			builder.append(')');
			return builder.toString();
		}
		else {
			return repr;
		}
	}
	
	// wraps quotes around and escape quotes in a string.
	private static final String quoteString(String str) {
		return '"' + str.replaceAll("\"", "\\\"") + '"';
	}
	
	// prohibit construction of this class.
	private Dep2Con() {}
	
}
