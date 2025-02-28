package org.spockframework.compiler.interaction;

import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.spockframework.compiler.IRewriteResources;

public interface InteractionRewriteResources extends IRewriteResources {

  MethodNode getCurrentMethod();

  MethodCallExpression getMockInvocationMatcher();

}
