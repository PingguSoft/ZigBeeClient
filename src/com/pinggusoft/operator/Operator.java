package com.pinggusoft.operator;

public interface Operator<T> {
    public boolean execute(T lhs, T rhs);
}
