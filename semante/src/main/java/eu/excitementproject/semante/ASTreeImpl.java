package eu.excitementproject.semante;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Value;
import semante.util.ASTree;

public abstract class ASTreeImpl<A,B> implements ASTree<A,B> {

	private static final long serialVersionUID = -6722461418304347457L;

	@Value
	@EqualsAndHashCode(callSuper=false)
	public static class ASNodeImpl<A,B> extends ASTreeImpl<A,B> {
		
		private static final long serialVersionUID = -3359274256896600964L;
		
		A pos;
		List<ASTree<A,B>> children;
		
		@Override
		public final <X> X accept(Visitor<A,B,X> v) {
			return v.node(pos, children);
		}
	}
	
	@Value
	@EqualsAndHashCode(callSuper=false)
	public static class ASLeafImpl<A,B> extends ASTreeImpl<A,B> {
		
		private static final long serialVersionUID = -5474429147314494335L;
		
		B value;
		
		@Override
		public final <X> X accept(Visitor<A,B,X> v) {
			return v.leaf(value);
		}
	}
	
}
