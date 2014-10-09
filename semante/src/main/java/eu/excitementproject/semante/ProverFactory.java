package eu.excitementproject.semante;

import eu.excitementproject.eop.common.representation.parse.tree.TreeAndParentMap;
import eu.excitementproject.eop.transformations.representation.ExtendedInfo;
import eu.excitementproject.eop.transformations.representation.ExtendedNode;

public final class ProverFactory {
	
	public static final Prover getProver(TreeAndParentMap<ExtendedInfo, ExtendedNode> hypothesis) {
		return new Prover(hypothesis);
	}
	
	private ProverFactory() {}
	
}
