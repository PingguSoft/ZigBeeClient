package com.pinggusoft.operator;

import java.util.HashMap;
import java.util.Map;

public class OperatorFactory {
    private static final Map<String, Operator<?>> OPERATORS = new HashMap<String, Operator<?>>();

    static {
      OPERATORS.put("<Number", new LessThanNumOperator());
//      OPERATORS.put("==Number", new EqualToNumOperator());
//      OPERATORS.put("<String", new LessThanStringOperator());
    }

    public static Operator<?> getOperator(String someUserSpecifiedOp, Class<?> paramType) {
      String key = someUserSpecifiedOp;
      if (Number.class.isAssignableFrom(paramType)) {
        key += "Number";
      } else if (String.class.isAssignableFrom(paramType)) {
        key += "String";
      }
      return OPERATORS.get(key);
    }
}

// Integer lhs = 5;
// Integer rhs = 6;
// OperatorFactory.getOperator(userDesignatedOperation, lhs.getClass()).execute(lhs, rhs);