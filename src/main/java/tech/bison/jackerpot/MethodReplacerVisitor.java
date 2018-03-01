
package tech.bison.jackerpot;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import tech.bison.jackerpot.source.Replacement;

public class MethodReplacerVisitor extends VoidVisitorAdapter<Void> {
  private List<Replacement> replacements;
  private TypeSolver solver;
  private static final Logger LOGGER = Logger.getLogger(MethodReplacerVisitor.class.getName());
  private boolean needSave;

  public MethodReplacerVisitor(List<Replacement> replacements, TypeSolver solver) {
    this.solver = solver;
    this.replacements = replacements;
  }

  @Override
  public void visit(MethodCallExpr methodCallExpression, Void arg) {
    super.visit(methodCallExpression, arg);
    Optional<Expression> methodScope = methodCallExpression.getScope();
    if (!methodScope.isPresent()) {
      return;
    }

    List<Replacement> matchingReplacements = replacements.stream()
        .filter(r -> r.getOldMethod().equals(methodCallExpression.getNameAsString())).collect(Collectors.toList());

    for (Replacement replacement : matchingReplacements) {
      LOGGER.finest(() -> String.format("start applying: replace %s -> %s", replacement.getOldMethod(), replacement.getNewMethod()));
      try {
        ResolvedType typeOfView = JavaParserFacade.get(solver).getType(methodScope.get());
        boolean isTypeMatch = typeOfView.isReferenceType()
            && typeOfView.asReferenceType().getQualifiedName().equals(replacement.getFullQualifiedViewType());
        if (isTypeMatch) {
          needSave = true;
          methodCallExpression.setName(replacement.getNewMethod());
          LOGGER.finest("applied successful");
          replacement.getNewMethodTypeParameters().forEach(methodCallExpression::addArgument);
          replacement.getClassesToImport()
              .forEach(fullQualifiedClassName -> addImportDeclaration(methodCallExpression, fullQualifiedClassName));
        }
      } catch (Exception e) {
        LOGGER.severe(e.getMessage() + " in " + methodCallExpression.getNameAsString());
      }
    }
  }

  public boolean needSave() {
    return needSave;
  }

  private void addImportDeclaration(MethodCallExpr methodCallExpression, String fullQualifiedClassName) {
    try {
      methodCallExpression.tryAddImportToParentCompilationUnit(Class.forName(fullQualifiedClassName));
    } catch (ClassNotFoundException e) {
      LOGGER.severe("Error trying to add import for " + fullQualifiedClassName);
      LOGGER.severe(e.getMessage());
    }
  }
}