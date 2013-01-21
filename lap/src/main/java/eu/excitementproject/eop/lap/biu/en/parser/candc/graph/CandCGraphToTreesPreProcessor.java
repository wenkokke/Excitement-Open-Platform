package eu.excitementproject.eop.lap.biu.en.parser.candc.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import eu.excitementproject.eop.common.datastructures.SimpleValueSetMap;
import eu.excitementproject.eop.common.datastructures.ValueSetMap;
import eu.excitementproject.eop.common.datastructures.dgraph.DirectedGraph;
import eu.excitementproject.eop.common.datastructures.dgraph.DirectedGraphException;
import eu.excitementproject.eop.common.datastructures.dgraph.EdgeAndNode;
import eu.excitementproject.eop.common.datastructures.immutable.ImmutableSet;


/**
 * Pre-processing on the C&C graph - before converting it to a set of trees.
 * 
 * @author Asher Stern
 *
 */
public class CandCGraphToTreesPreProcessor
{
	
	///////////////////// PUBLIC PART //////////////////////////////
	
	
	/**
	 * Constructs the {@link CandCGraphToTreesPreProcessor} object.
	 * Later, the method {@link #preprocess()} should be called.
	 * <P>
	 * Note: the graph that is given by this constructor will be modified.
	 * The pre-processing is done on the graph itself, not on a copy of it.
	 * 
	 * @param graph the graph as was given by C&C output (C&C output as is).
	 * This graph is generated by {@link CandCOutputToGraph}.
	 * 
	 * @throws CandCTreesPreProcessorException indicates bad graph
	 */
	public CandCGraphToTreesPreProcessor(DirectedGraph<CCNode, CCEdgeInfo> graph) throws CandCTreesPreProcessorException
	{
		if (null==graph)
			throw new CandCTreesPreProcessorException("null graph accepted.");
		
		this.graph = graph;
	}
	
	/**
	 * Makes the pre-processing
	 * 
	 * @throws CandCTreesPreProcessorException any error
	 */
	public void preprocess() throws CandCTreesPreProcessorException
	{
		try
		{
			if (null==graph.getAllNodes()) ;
			else
			{
				boolean nothingToDo = false;
				while (!nothingToDo)
				{
					nothingToDo = true;
					for (CCNode graphNode : graph.getAllNodes())
					{
						if (graph.getDirectSuccessorsOf(graphNode)!=null)
						{
							// I make the copy because the set may be changed, since I am changing the graph
							// during the operation.
							Set<EdgeAndNode<CCNode, CCEdgeInfo>> successorsEdges = graph.getDirectSuccessorsOf(graphNode).getMutableSetCopy();

							if (successorsEdges!=null)
							{
								for (EdgeAndNode<CCNode, CCEdgeInfo> successorEdge : successorsEdges)
								{
									boolean somethingWasDone = processOneEdge(graphNode,successorEdge);
									if (somethingWasDone)
										nothingToDo = false;
								}
							}
						}
					}
				} // end of "while" loop
			} // end of else
		}
		catch(DirectedGraphException e)
		{
			throw new CandCTreesPreProcessorException("Graph problem. See nested exception.",e);
		}

	}
	
	
	
	//////////////////////////////////////////////////////////////////////////
	//////////////////////// PROTECTED & PRIVATE PART ////////////////////////
	//////////////////////////////////////////////////////////////////////////
	
	
	// constants
	protected static Set<String> AUX_RELATIONS;
	static
	{
		AUX_RELATIONS = new HashSet<String>();
		AUX_RELATIONS.add("aux");
	}
	
	////////////////////// PRIVATE & PROTECTED METHODS //////////////////////////////
	
	/**
	 * If the successor has "parent" (i.e. predecessor) that that successor is "aux" of it (i.e.
	 * edge from that parent to that successor is labeled as "aux") - then "our" node is connected directly
	 * to the "parent", and the edge from "our" node to the successor is removed.
	 * 
	 * @param node a node in the graph
	 * @param edge an edge to one of its successors.
	 * @return <tt> true </tt> is something was done
	 * @throws DirectedGraphException
	 */
	private boolean processOneEdge(CCNode node, EdgeAndNode<CCNode, CCEdgeInfo> edge) throws DirectedGraphException
	{
		boolean ret = false;
		CCNode successorNode = edge.getNode();
		
		boolean alreadyDone = false;
		if (processed.containsKey(node)) if (processed.get(node).contains(successorNode)) alreadyDone = true;
		
		if (alreadyDone)
			ret = false;
		else
		{
			processed.put(node, successorNode);
			
			ImmutableSet<EdgeAndNode<CCNode, CCEdgeInfo>> predecessorsEdges = graph.getDirectPredecessorsOf(successorNode);
			boolean processOperationDone = false;
			if (predecessorsEdges!=null)
			{
				Iterator<EdgeAndNode<CCNode, CCEdgeInfo>> predecessorsEdgesIter = predecessorsEdges.iterator();
				while (predecessorsEdgesIter.hasNext() && !processOperationDone)
				{
					EdgeAndNode<CCNode, CCEdgeInfo> predecessorsEdge = predecessorsEdgesIter.next();
					if (AUX_RELATIONS.contains(predecessorsEdge.getEdgeInfo().getGrType()))
					{
						graph.removeEdge(node, successorNode);
						if (graph.isExistEdge(node, predecessorsEdge.getNode()))
							; // do nothing
						else
							graph.addEdge(node, predecessorsEdge.getNode(), edge.getEdgeInfo());

						processOperationDone = true;
					}
				}
			}
			if (processOperationDone)
				ret = true;
		}
		
		return ret;
	}
	
	
	///////////////// PRIVATE & PROTECTED FIELDS ////////////////////////////////

	/**
	 * Stores any v--->u edge that was processed.
	 * This is required for abnormal graphs in which there is a loop (cycle) of "aux" edges.
	 * Such a loop may lead to infinite loop in run time. By storing the history of any
	 * edge that was done - it is never done twice, and the loop is stopped.
	 */
	private ValueSetMap<CCNode, CCNode> processed = new SimpleValueSetMap<CCNode, CCNode>();

	protected DirectedGraph<CCNode, CCEdgeInfo> graph;
	

}