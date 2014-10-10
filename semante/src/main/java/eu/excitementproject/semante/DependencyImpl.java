package eu.excitementproject.semante;

import lombok.Value;
import semante.parser.Dependency;

@Value
public class DependencyImpl implements Dependency {

	private static final long serialVersionUID = 7516762830940405676L;
	
	String dependencyName;

}
