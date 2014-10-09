package eu.excitementproject.semante;

import lombok.Value;

public interface ConTree {

	<X> X accept(Visitor<X> v);
	
	public interface Visitor<X> {
		
		public X node(String pos, Iterable<ConTree> children);
		public X leaf(String word, String pos, Integer serial);
		
	}
	
	@Value
	public static class Node implements ConTree {
		
		String pos;
		Iterable<ConTree> children;
		
		@Override
		public final <X> X accept(Visitor<X> v) {
			return v.node(pos, children);
		}
	}
	
	@Value
	public static class Leaf implements ConTree {
		
		String word;
		String pos;
		Integer serial;
		
		@Override
		public final <X> X accept(Visitor<X> v) {
			return v.leaf(word, pos, serial);
		}
	}
	
}
