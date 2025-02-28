package org.spockframework.compiler.interaction;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.spockframework.compiler.AstNodeCache;
import org.spockframework.compiler.ErrorReporter;
import org.spockframework.compiler.SourceLookup;
import org.spockframework.compiler.condition.DefaultConditionErrorRecorders;
import org.spockframework.compiler.condition.IConditionErrorRecorders;
import spock.lang.Specification;

import static org.spockframework.compiler.AstUtil.createDirectMethodCall;

public class DefaultInteractionRewriteResources implements InteractionRewriteResources {

  private final MethodNode currentMethod;
  private final AstNodeCache nodeCache;
  private final SourceLookup sourceLookup;
  private final ErrorReporter errorReporter;
  private final IConditionErrorRecorders errorRecorders;

  public DefaultInteractionRewriteResources(
    MethodNode currentMethod,
    AstNodeCache nodeCache,
    SourceLookup sourceLookup,
    ErrorReporter errorReporter
  ) {
    this.currentMethod = currentMethod;
    this.nodeCache = nodeCache;
    this.sourceLookup = sourceLookup;
    this.errorReporter = errorReporter;
    this.errorRecorders = new DefaultConditionErrorRecorders(nodeCache);
  }

  @Override
  public MethodNode getCurrentMethod() {
    return currentMethod;
  }

  public MethodCallExpression getSpecificationContext() {
    return createDirectMethodCall(
      new VariableExpression("spec", ClassHelper.make(Specification.class)),
      nodeCache.SpecInternals_GetSpecificationContext,
      ArgumentListExpression.EMPTY_ARGUMENTS
    );
  }

  @Override
  public MethodCallExpression getMockInvocationMatcher() {
    return createDirectMethodCall(
      getSpecificationContext(),
      nodeCache.SpecificationContext_GetMockController,
      ArgumentListExpression.EMPTY_ARGUMENTS);
  }

  @Override
  public AstNodeCache getAstNodeCache() {
    return nodeCache;
  }

  @Override
  public String getSourceText(ASTNode node) {
    return sourceLookup.lookup(node);
  }

  @Override
  public ErrorReporter getErrorReporter() {
    return errorReporter;
  }

  @Override
  public IConditionErrorRecorders getErrorRecorders() {
    return errorRecorders;
  }
}
