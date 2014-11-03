package com.pinggusoft.operator;

public class LessThanNumOperator implements Operator<Number> {
    public boolean execute(Number lhs, Number rhs) {
       return  lhs.doubleValue() < rhs.doubleValue();
    }
}
