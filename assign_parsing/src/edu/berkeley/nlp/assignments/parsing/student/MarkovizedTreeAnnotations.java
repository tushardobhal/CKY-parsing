package edu.berkeley.nlp.assignments.parsing.student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.ling.Trees;
import edu.berkeley.nlp.util.Filter;

public class MarkovizedTreeAnnotations {
	private int verticalLevel;
	private int horizontalLevel;

	public MarkovizedTreeAnnotations(int verticalLevel, int horizontalLevel) {
		this.verticalLevel = verticalLevel;
		this.horizontalLevel = horizontalLevel;
	}

	public Tree<String> annotateTreeLosslessBinarization(Tree<String> unAnnotatedTree) {

		return binarizeTree(unAnnotatedTree);
	}

	private Tree<String> binarizeTree(Tree<String> tree) {
		String label = tree.getLabel();
		if (tree.isLeaf())
			return new Tree<String>(label);
		
		tree = verticalMarkovizeTree(tree);
		
		if (tree.getChildren().size() == 1) {
			return new Tree<String>(label, Collections.singletonList(binarizeTree(tree.getChildren().get(0))));
		}
		// otherwise, it's a binary-or-more local tree, so decompose it into a sequence
		// of binary and unary trees.
		String intermediateLabel = "@" + label + "->";
		Tree<String> intermediateTree = binarizeTreeHelper(tree, 0, intermediateLabel);
		return new Tree<String>(label, intermediateTree.getChildren());
	}

	private Tree<String> binarizeTreeHelper(Tree<String> tree, int numChildrenGenerated, String intermediateLabel) {
		Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
		List<Tree<String>> children = new ArrayList<Tree<String>>();
		children.add(binarizeTree(leftTree));
		if(numChildrenGenerated < tree.getChildren().size() - 1) {
			String newIntermediateLabel = horizontalMarkovizeTree(intermediateLabel, leftTree.getLabel());
			Tree<String> rightTree = binarizeTreeHelper(tree, numChildrenGenerated + 1, newIntermediateLabel);
			children.add(rightTree);
		}
		return new Tree<String>(intermediateLabel, children);
	}
	
	private Tree<String> verticalMarkovizeTree(Tree<String> tree) {
		String parent = tree.getLabel();
		List<Tree<String> > children = tree.getChildren();
		for(Tree<String> child : children) {
			if(!child.isLeaf()) {
				String childLabel = child.getLabel();
				String[] parentTrimmed = parent.replace("\\@", "").split("\\^");
				for(int i=0; i<verticalLevel-1; i++) {	
					if(i < parentTrimmed.length)
						childLabel = childLabel + "^" + parentTrimmed[i];
				}
				child.setLabel(childLabel);
			}
		}

		return tree;
	}
	
	private String horizontalMarkovizeTree(String label, String leftTreeLabel) {
		String[] labelArray = label.split("->");
		if(horizontalLevel == 0)
			return labelArray[0];
		if(labelArray.length == 1)
			return label + "_" + leftTreeLabel;
		
		String[] suffix = labelArray[1].split("_");
		if(suffix.length - 1 == horizontalLevel) {
			String horChild = "";
			for(int i=2; i<suffix.length; i++)
				horChild += "_" + suffix[i];
			return labelArray[0] + "->" + horChild + "_" + leftTreeLabel.split("\\^")[0];
		}
		return labelArray[0] + "->" + labelArray[1] + "_" + leftTreeLabel.split("\\^")[0];
	}

	public Tree<String> unAnnotateTree(Tree<String> annotatedTree) {
		// Remove intermediate nodes (labels beginning with "@"
		// Remove all material on node labels which follow their base symbol (cuts
		// anything after <,>,^,=,_ or ->)
		// Examples: a node with label @NP->DT_JJ will be spliced out, and a node with
		// label NP^S will be reduced to NP
		Tree<String> debinarizedTree = Trees.spliceNodes(annotatedTree, new Filter<String>() {
			public boolean accept(String s) {
				return s.startsWith("@");
			}
		});
		Tree<String> unAnnotatedTree = (new Trees.LabelNormalizer()).transformTree(debinarizedTree);
		return unAnnotatedTree;
	}
}
