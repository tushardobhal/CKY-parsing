package edu.berkeley.nlp.assignments.parsing.student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.berkeley.nlp.assignments.parsing.BinaryRule;
import edu.berkeley.nlp.assignments.parsing.Grammar;
import edu.berkeley.nlp.assignments.parsing.Parser;
import edu.berkeley.nlp.assignments.parsing.SimpleLexicon;
import edu.berkeley.nlp.assignments.parsing.UnaryClosure;
import edu.berkeley.nlp.assignments.parsing.UnaryRule;
import edu.berkeley.nlp.assignments.parsing.student.util.BinaryBackPointerObject;
import edu.berkeley.nlp.assignments.parsing.student.util.UnaryBackPointerObject;
import edu.berkeley.nlp.ling.Tree;
import edu.berkeley.nlp.util.Indexer;

public class CKYParser implements Parser {

	private static final String ROOT_INDEX = "ROOT";
	private int maxVerticalLevel = 2;
	private int maxHorizontalLevel = 2;
	
	private Grammar grammar;
	private UnaryClosure unaryClosure;
	private SimpleLexicon lexicon;
	private Indexer<String> labelIndexer;
	private MarkovizedTreeAnnotations treeAnnotations;

	@Override
	public Tree<String> getBestParse(List<String> sentence) {
		System.out.println("Starting Parsing of sentence of size - " + sentence.size() + "...");

	    int sentenceSize = sentence.size();
		int symbolSize = labelIndexer.size();
	    double[][][] unaryChart = new double[sentenceSize+1][sentenceSize+1][symbolSize];
	    fillDefaultValues(unaryChart);
	    double[][][] binaryChart = new double[sentenceSize+1][sentenceSize+1][symbolSize];
	    fillDefaultValues(binaryChart);
	    UnaryBackPointerObject[][][] unaryBackPointerChart = 
	    		new UnaryBackPointerObject[sentenceSize+1][sentenceSize+1][symbolSize];
	    BinaryBackPointerObject[][][] binaryBackPointerChart = 
	    		new BinaryBackPointerObject[sentenceSize+1][sentenceSize+1][symbolSize];

	    // Base Binary Case
	    for(int i=0; i<sentenceSize; i++) {	
	    	for(String tag : lexicon.getAllTags()) {
	    		int tagIndex = labelIndexer.indexOf(tag);
				double oldVal = binaryChart[i][i+1][tagIndex];
	    		double newVal = lexicon.scoreTagging(sentence.get(i), tag);
	    		if(!Double.isNaN(newVal) && newVal > oldVal) {
	    			binaryChart[i][i+1][tagIndex] = newVal;
	    		}
	    	}
	    }
	    // Unary Chart Update
	    for(int i=0; i<sentenceSize; i++) {	
	    	for(String tag : lexicon.getAllTags()) {
	    		int symbol = labelIndexer.indexOf(tag);
	    		if(binaryChart[i][i+1][symbol] > Double.NEGATIVE_INFINITY) {
	    			for(UnaryRule unaryRule : unaryClosure.getClosedUnaryRulesByChild(symbol)) {
	    				double oldVal = unaryChart[i][i+1][unaryRule.parent];
	    	    		double newVal = unaryRule.getScore() + binaryChart[i][i+1][symbol];
	    	    		if(newVal > oldVal) {
	    	    			unaryChart[i][i+1][unaryRule.parent] = newVal;
	    	    			unaryBackPointerChart[i][i+1][unaryRule.parent] = 
	    	    					new UnaryBackPointerObject(unaryRule);
	    	    		}
	    			}
	    		}
	    	}
	    }
	    
	    // Main Algorithm
	    for(int j=2; j<=sentenceSize; j++) {
	    	for(int i=j-2; i>=0; i--) {
	    		// Binary Chart Update
	    		for(int k=i+1; k<=j-1; k++) {
	    			for(BinaryRule binaryRule : grammar.getBinaryRules()) {
	    				if(unaryChart[i][k][binaryRule.leftChild] > Double.NEGATIVE_INFINITY && 
	    						unaryChart[k][j][binaryRule.rightChild] > Double.NEGATIVE_INFINITY) {
	    					double oldVal = binaryChart[i][j][binaryRule.parent];
	    					double newVal = binaryRule.getScore() + unaryChart[i][k][binaryRule.leftChild] + 
	    							unaryChart[k][j][binaryRule.rightChild];
	    					if(newVal > oldVal) {
	    						binaryChart[i][j][binaryRule.parent] = newVal;
	    						binaryBackPointerChart[i][j][binaryRule.parent] = 
	    								new BinaryBackPointerObject(k, binaryRule);
	    					}
	    				}
	    			}
	    		}
	    		// Unary Chart Update
	    		for(int symbol=0; symbol<labelIndexer.size(); symbol++) {
		    		if(binaryChart[i][j][symbol] > Double.NEGATIVE_INFINITY) {
		    			for(UnaryRule unaryRule : unaryClosure.getClosedUnaryRulesByChild(symbol)) {
		    				double oldVal = unaryChart[i][j][unaryRule.parent];
		    	    		double newVal = unaryRule.getScore() + binaryChart[i][j][unaryRule.child];
		    	    		if(newVal > oldVal) {
		    	    			unaryChart[i][j][unaryRule.parent] = newVal;
		    	    			unaryBackPointerChart[i][j][unaryRule.parent] = 
		    	    					new UnaryBackPointerObject(unaryRule);
		    	    		}
		    			}
		    		}
		    	}
	    	}
	    }
	    
	    if(unaryChart[0][sentenceSize][labelIndexer.indexOf(ROOT_INDEX)] == Double.NEGATIVE_INFINITY)
	    	return new Tree<String>(ROOT_INDEX, Collections.singletonList(new Tree<String>("JUNK"))); 
				
	    return treeAnnotations.unAnnotateTree(reconstructBestParseTree(sentence, 0, sentenceSize, 
	    		labelIndexer.indexOf(ROOT_INDEX), true, unaryBackPointerChart, binaryBackPointerChart));
	}

	private void fillDefaultValues(double[][][] array) {
		for(int i=0; i<array.length; i++)
			for(int j=0; j<array[i].length; j++)
				Arrays.fill(array[i][j], Double.NEGATIVE_INFINITY);
	}
	
	private Tree<String> reconstructBestParseTree(List<String> sentence, int i, int j, int parent, boolean isUnary, 
			UnaryBackPointerObject[][][] unaryBackPointerChart, BinaryBackPointerObject[][][] binaryBackPointerChart) {
		if(j==i+1 && !isUnary) {
			Tree<String> terminalTree = new Tree<String>(sentence.get(i));
			return new Tree<String>(labelIndexer.get(parent), Arrays.asList(terminalTree));
		}
		else if(isUnary) {
			UnaryRule closedUnaryRule = unaryBackPointerChart[i][j][parent].getUnaryRule();
			if(closedUnaryRule.parent == closedUnaryRule.child) {
				return reconstructBestParseTree(sentence, i, j, parent, false, 
						unaryBackPointerChart, binaryBackPointerChart);
			} else {
				List<Integer> unaryTreePath = unaryClosure.getPath(closedUnaryRule);
				if(unaryTreePath != null && unaryTreePath.size() > 2) {
					Tree<String> childUnaryTree = reconstructBestParseTree(sentence, i, j, 
							unaryTreePath.get(unaryTreePath.size()-1), false, unaryBackPointerChart, binaryBackPointerChart);
					return constructFullUnaryTree(unaryTreePath, childUnaryTree);
				} else {
					Tree<String> childUnaryTree = reconstructBestParseTree(sentence, i, j, 
							closedUnaryRule.child, false, unaryBackPointerChart, binaryBackPointerChart);
					return new Tree<String>(labelIndexer.get(parent), Arrays.asList(childUnaryTree));
				}
			}
		}
		else {
			BinaryBackPointerObject binaryBackPointer = binaryBackPointerChart[i][j][parent];
			Tree<String> leftChildTree = reconstructBestParseTree(sentence, i, binaryBackPointer.getK(), 
					binaryBackPointer.getBinaryRule().leftChild, true, unaryBackPointerChart, binaryBackPointerChart);
			Tree<String> rightChildTree = reconstructBestParseTree(sentence, binaryBackPointer.getK(), j, 
					binaryBackPointer.getBinaryRule().rightChild, true, unaryBackPointerChart, binaryBackPointerChart);
			return new Tree<String>(labelIndexer.get(parent), Arrays.asList(leftChildTree, rightChildTree));
		}
		
	}

	private Tree<String> constructFullUnaryTree(List<Integer> unaryTreePath, Tree<String> childUnaryTree) {
		Tree<String> parentUnaryTree = null;
		for(int k=unaryTreePath.size()-2; k>=0; k--) {
			parentUnaryTree = new Tree<String>(labelIndexer.get(unaryTreePath.get(k)), 
					Arrays.asList(childUnaryTree));
			childUnaryTree = parentUnaryTree;
		}
		return parentUnaryTree;
	}

	public CKYParser(List<Tree<String>> trainTrees) {
		System.out.println("Initializing CKY Parser...");

		System.out.print("Annotating / binarizing training trees with Vertical Markovization " + maxVerticalLevel
				+ " and Horizontal Markovization " + maxHorizontalLevel + "... ");
		treeAnnotations = new MarkovizedTreeAnnotations(maxVerticalLevel, maxHorizontalLevel);
		List<Tree<String>> annotatedTrainTrees = annotateTrees(trainTrees);
		System.out.println("Done.");

		System.out.print("Building grammar ... ");
		grammar = Grammar.generativeGrammarFromTrees(annotatedTrainTrees);
		labelIndexer = grammar.getLabelIndexer();
		System.out.println("Done. (" + grammar.getLabelIndexer().size() + " states)");

		System.out.print("Building unary closure rules ... ");
		unaryClosure = new UnaryClosure(grammar.getLabelIndexer(), grammar.getUnaryRules());
		System.out.println("Done. (" + unaryClosure.getPathMap().size() + " unary closure rules)");

		System.out.print("Building lexicon ... ");
		lexicon = new SimpleLexicon(annotatedTrainTrees);
		System.out.println("Done. (" + lexicon.getAllTags().size() + " tags)");

		System.out.println("Done initializing CKY Parser.");
	}

	private List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
		List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			Tree<String> annotatedTree = treeAnnotations.annotateTreeLosslessBinarization(tree);
			annotatedTrees.add(annotatedTree);
		}
		return annotatedTrees;
	}

}
