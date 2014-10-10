package eu.excitementproject.semante;

import lombok.Value;
import semante.parser.Node;

@Value
public class IdAndPos implements Node {

	private static final long serialVersionUID = -1098091256431994692L;
	
	int id;
	String label;
	
}
