package eu.excitementproject.semante;

import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import semante.parser.Dependency;
import semante.parser.Token;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class TokenImpl implements Token {

	int id;
	String lemma;
	String NER;
	String POS;
	String syntacticCategory;
	String text;
	Map<Token, Dependency> dependencies;

	private static final long serialVersionUID = -2616788126391260344L;
	
}
