package edu.berkeley.nlp.assignments.parsing.student.util;

import edu.berkeley.nlp.assignments.parsing.BinaryRule;

public class BinaryBackPointerObject {
	
	private int k;
	private BinaryRule binaryRule;
	
	public BinaryBackPointerObject(int k, BinaryRule binaryRule) {
		this.k = k;
		this.binaryRule = binaryRule;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public BinaryRule getBinaryRule() {
		return binaryRule;
	}

	public void setLeftChild(BinaryRule binaryRule) {
		this.binaryRule = binaryRule;
	}

}
