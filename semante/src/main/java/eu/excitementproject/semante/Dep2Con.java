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
import java.util.Map;

import lombok.Cleanup;
import lombok.val;
import lombok.experimental.NonFinal;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.functors.Map2;
import org.codehaus.jparsec.functors.Map5;

import semante.parser.Dependency;
import semante.parser.Node;
import semante.parser.Token;
import semante.util.ASTree;
import semante.util.ASTree.Visitor;
import semante.util.Pair;
import semante.util.impl.IPair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import eu.excitementproject.eop.transformations.representation.ExtendedNode;

public final class Dep2Con {
	
	/**
	 * Path to the <tt>dep2con</tt> executable.
	 */
	private static final String DEP2CON = "/usr/local/bin/dep2con";
	
	public static final ASTree<Node, Token> dep2con(ExtendedNode depTree) throws Dep2ConException {
		
		val stringAndInfo = showAndGatherInfo(depTree);
		val string        = stringAndInfo.getFirst();
		val infoMap       = stringAndInfo.getSecond();
				
		return parse(dep2con(string)).accept(
			new Visitor<String, String, ASTree<Node, Token>>() {

				private @NonFinal int index = 0;
				private static final long serialVersionUID = -2885768485341951281L;

				@Override
				public final ASTree<Node, Token> leaf(String id) {
					val info = (TokenImpl) infoMap.get(id);
					info.setId(index++);
					return new ASTreeImpl.ASLeafImpl<Node, Token>(info);
				}

				@Override
				public final ASTree<Node, Token> node(
						String pos, List<ASTree<String, String>> children) {
					
					val builder = ImmutableList.<ASTree<Node, Token>> builder();
					for (val child : children) {
						builder.add(child.accept(this));
					}					
					return new ASTreeImpl.ASNodeImpl<Node, Token>(
						new IdAndPos(index++, pos), builder.build()
					);
				}
				
			}
		);
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
	
	
	private static final ASTree<String,String> parse(String tree) {
		return pASTree.parse(tree);
	}
	
	private static final Parser<ASTree<String,String>> pASTree = mkPASTree();
	
	private static final Parser<ASTree<String,String>> mkPASTree() {
		val pRef = Parser.<ASTree<String,String>> newReference();
		val pLeaf = sequence
				( DOUBLE_QUOTE_STRING
				, isChar('/')
				, DOUBLE_QUOTE_STRING
				, isChar('/')
				, INTEGER
				, new Map5<String,Void,String,Void,String,ASTree<String,String>>() {
					
				@Override
				public final ASTree<String,String> map(
					String id, Void _1, String pos, Void _2, String serial) {
					return new ASTreeImpl.ASLeafImpl<String,String>(id);
				}
		}).followedBy(WHITESPACES.skipMany());
		val pNode = sequence
				( DOUBLE_QUOTE_STRING.followedBy(WHITESPACES.skipMany1())
				, pRef.lazy().many()
				, new Map2<String, List<ASTree<String,String>>, ASTree<String,String>>() {

				@Override
				public ASTree<String,String> map(String pos, List<ASTree<String,String>> children) {
					return new ASTreeImpl.ASNodeImpl<String,String>(pos, children);
				}
					
				}).between(isChar('('), isChar(')'))
				  .followedBy(WHITESPACES.skipMany());
		val pTree = pLeaf.or(pNode);
		pRef.set(pTree);
		return pTree;
	}
	
	
	////////// PRINT TREES ////////// 

	private static final Pair<String,Map<String,Token>> showAndGatherInfo(ExtendedNode tree) {
		
		// CREATE STRING REPRESENTATION
		
		val id       = quoteString(tree.getInfo().getId()); 
		val posInfo  = tree.getInfo().getNodeInfo().getSyntacticInfo().getPartOfSpeech();
		val pos      = quoteString(posInfo == null ? "ROOT" : posInfo.getStringRepresentation()); 
		val serial   = tree.getInfo().getNodeInfo().getSerial();	
		val string   = String.format("%s/%s/%d", id, pos, serial);
		
		// CREATE ID-MAP
		
		val infoMap  = ImmutableMap.<String, Token> builder();
		val nodeInfo = tree.getInfo().getNodeInfo();
		val nerTag   = nodeInfo.getNamedEntityAnnotation();
		val posTag   = nodeInfo.getSyntacticInfo().getPartOfSpeech();
		val depMap   = ImmutableMap.<Token,Dependency> builder();
		val current  = new TokenImpl
			( -1
			, nodeInfo.getWordLemma()
			, nerTag == null ? "" : nerTag.name()
			, posTag == null ? "" : posTag.getStringRepresentation()
			, ""
			, nodeInfo.getWord()
			, null
			);
		infoMap.put(id, current);
		
		// RECURSIVE CALLS
		
		if (tree.hasChildren()) {
			val builder = new StringBuilder();
			builder.append('(');
			builder.append(string);
			for (val child : tree.getChildren())
			{
				val stringAndInfo = showAndGatherInfo(child);
				builder.append(' ');
				builder.append(stringAndInfo.getFirst());
				
				// update the info map.
				infoMap.putAll(stringAndInfo.getSecond());
				
				// update the dependency map.
				depMap.put(
					stringAndInfo.getSecond().get(child.getInfo().getId()),
					new DependencyImpl(child.getInfo().getEdgeInfo().getDependencyRelation().getStringRepresentation())
				);
			}
			builder.append(')');
			
			// insert the dependency map.
			current.setDependencies(depMap.build());
			
			return pair(builder.toString(), (Map<String,Token>) infoMap.build());
		}
		else {
			return pair(string, (Map<String,Token>) infoMap.build());
		}
	}
	
	private static final String quoteString(String str) {
		return '"' + str.replaceAll("\"", "\\\"") + '"';
	}
	
	private static final <A,B> Pair<A,B> pair(A fst, B snd) {
		return new IPair<A,B>(fst, snd);
	}
	
	private Dep2Con() {}
	
}
