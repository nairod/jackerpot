/**
 * File Name: MethodCallReplacer.java
 * 
 * Copyright (c) 2018 BISON Schweiz AG, All Rights Reserved.
 */

package tech.bison.jackerpot;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.CompilationUnit.Storage;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import tech.bison.jackerpot.source.Replacement;

/**
 *
 * @author dorian.nyffeler
 */
public final class MethodCallReplacer {
  private static final Logger LOGGER = Logger.getLogger(MethodCallReplacer.class.getName());
  private final TypeSolver solver;

  public MethodCallReplacer(TypeSolver solver) {
    this.solver = solver;
    LOGGER.setLevel(Level.INFO);
  }

  public void applyReplacement(CompilationUnit cu, Replacement replacement, MethodCallExpr methodCall) {
    String methodCallToReplace = replacement.getOldMethod();
    try {
      if (needsReplacement(replacement, methodCall, methodCallToReplace)) {
        methodCall.setName(replacement.getNewMethod());
        LOGGER.finest(() -> String.format("replace %s -> %s", methodCallToReplace, replacement.getNewMethod()));
        replacement.getNewMethodTypeParameters().forEach(methodCall::addArgument);
        addClassImport(replacement, methodCall);
        cu.getStorage().ifPresent(Storage::save);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, String.format("could not parse %s", cu.getPrimaryTypeName().orElse("unknown")));
      LOGGER.log(Level.FINE, String.format("could not parse %s", cu.getPrimaryTypeName().orElse("unknown")), e);
    }
  }

  private boolean needsReplacement(Replacement replacement, MethodCallExpr methodCall, String methodCallToReplace) {
    boolean isNameMatch = methodCallToReplace.equals(methodCall.getNameAsString());
    Optional<Expression> scope = methodCall.getScope();
    if (!isNameMatch || !scope.isPresent()) {
      return false;
    }
    try {
      ResolvedType resolvedType = JavaParserFacade.get(solver).getType(scope.get());
      return resolvedType.isReferenceType()
          && resolvedType.asReferenceType().getQualifiedName().equals(replacement.getFullQualifiedViewType());
    } catch (RuntimeException e) {
      LOGGER.log(Level.WARNING, String.format("could not resolve type for %s. Skipped replacement", scope.get().toString()));
      LOGGER.log(Level.FINER, String.format("could not resolve type for %s. Skipped replacement", scope.get().toString()), e);
      return false;
    }
  }

  private void addClassImport(Replacement replacement, MethodCallExpr methodCallExpression) {
    replacement.getClassesToImport().forEach(fullQualifiedClassName -> {
      try {
        methodCallExpression.tryAddImportToParentCompilationUnit(Class.forName(fullQualifiedClassName));
      } catch (ClassNotFoundException e) {
        LOGGER.log(Level.SEVERE, String.format("could not add import for type %s. Check classpath.", fullQualifiedClassName));
        LOGGER.log(Level.FINE, String.format("could not add import for type %s. Check classpath.", fullQualifiedClassName), e);
      }
    });
  }
}
