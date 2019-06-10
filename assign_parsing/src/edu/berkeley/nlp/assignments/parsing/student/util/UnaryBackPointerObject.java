package edu.berkeley.nlp.assignments.parsing.student.util;

import edu.berkeley.nlp.assignments.parsing.UnaryRule;

public class UnaryBackPointerObject {
	
	private UnaryRule unaryRule;
	
	public UnaryBackPointerObject(UnaryRule unaryRule) {
		this.unaryRule = unaryRule;
	}

	public UnaryRule getUnaryRule() {
		return unaryRule;
	}

	public void setUnaryRule(UnaryRule unaryRule) {
		this.unaryRule = unaryRule;
	}
	
}
