package eu.excitementproject.semante;

import java.io.IOException;

import semante.parser.Node;
import semante.parser.Token;
import semante.util.ASTree;
import eu.excitementproject.eop.common.representation.parse.tree.TreeAndParentMap;
import eu.excitementproject.eop.transformations.representation.ExtendedInfo;
import eu.excitementproject.eop.transformations.representation.ExtendedNode;

public final class Prover {
	
	private ASTree<Node, Token> hypothesis = null;
	
	/**
	 * Set the hypothesis tree using the external dep2con utility to convert it
	 * to a constituency tree.
	 * 
	 * @param hypothesis the hypothesis tree
	 * @throws IOException 
	 */
	public Prover(final TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis) {
		try {
			this.hypothesis = Dep2Con.dep2con(hypothesis.getTree());
		}
		catch (Dep2ConException e) {
			System.err.println("SemAnTE: " + e.getMessage());
		}
	}
	
	/**
	 * Check if the given text allows the hypothesis this prover was initialised with.
	 * 
	 * @param text 
	 * @return
	 */
	public final boolean isHypothesisAllowedBy(final TreeAndParentMap<ExtendedInfo, ExtendedNode> text) {
		if (this.hypothesis == null) {
			System.err.println("SemAnTE: null hypothesis");
			return false;
		}
		return false;
	}
	
}
