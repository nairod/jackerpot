
package tech.bison.jackerpot.experimental;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.CompilationUnit.Storage;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import tech.bison.jackerpot.MethodCallReplacer;
import tech.bison.jackerpot.hintparser.HintParser;
import tech.bison.jackerpot.hintparser.HintParserFactory;
import tech.bison.jackerpot.source.Replacement;

public class Test1 {
  private static final String APPLICATION_ROOT = "C:\\workspace\\landi_frame_head\\Application";
  private static final String HINT_FILE_FOLDER = "C:\\workspace\\landi_frame_head\\tech.bison.jackerpot\\hintfiles";
  private static final List<String> ROOT_FOLDER_PREFIX = Arrays.asList("CH.fenaco", "CH.obj.Application", "xModultests");
  private static final Logger LOGGER = Logger.getLogger(Test1.class.getName());
  private static List<Replacement> replacements;
  private static CombinedTypeSolver combinedTypeSolver;
  private static MethodCallReplacer methodReplacer;

  public static void main(String[] args) throws IOException {
    LOGGER.setLevel(Level.INFO);

    replacements = createReplacements();
    combinedTypeSolver = createTypeSolvers();
    methodReplacer = new MethodCallReplacer(combinedTypeSolver);
    List<Path> sourceFolders = getSourceFolders();
    LOGGER.info(sourceFolders::toString);
    applyForAll();
    // applyReplacements5();
  }

  private static void singleFile(Path sourceFolder, String javaFile) {
    try {
      CompilationUnit cu = JavaParser.parse(Paths.get(javaFile));
      applyReplacements4(cu);
    } catch (NoClassDefFoundError e) {
      LOGGER.severe(() -> "Class not found: " + e.getMessage());
      LOGGER.fine(() -> Arrays.toString(e.getStackTrace()));
    } catch (IOException e) {
      LOGGER.severe(() -> "can't get path of " + javaFile);
      LOGGER.fine(() -> Arrays.toString(e.getStackTrace()));
    }
  }

  private static void applyReplacements(CompilationUnit cu) {
    if (cu.getPrimaryTypeName().isPresent()) {
      LOGGER.fine(() -> cu.getPrimaryTypeName().get());
    }

    for (Replacement replacement : replacements) {
      for (MethodCallExpr methodCall : cu.findAll(MethodCallExpr.class)) {
        Optional<Expression> methodScope = methodCall.getScope();
        if (methodScope.isPresent()) {
          try {
            String methodCallToReplace = replacement.getOldMethod();
            boolean isNameMatch = isNameMatch(methodCall, methodCallToReplace);
            if (isNameMatch) {
              ResolvedType typeOfView = typeOfView(methodScope);
              boolean isTypeMatch = isTypeMatch(replacement, typeOfView);
              if (isTypeMatch) {
                methodCall.setName(replacement.getNewMethod());
                LOGGER.finest(() -> String.format("replace %s -> %s", methodCallToReplace, replacement.getNewMethod()));
                replacement.getNewMethodTypeParameters().forEach(methodCall::addArgument);
                addClassImport(replacement, methodCall);
                save(cu);
                LOGGER.fine(() -> "Saved: " + cu.getPrimaryTypeName());
              }
            }
          } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "in " + cu.getPrimaryTypeName().orElse("Unknown"));
          }
        }
      }
    }
  }

  private static void applyReplacements1(CompilationUnit cu) {
    if (cu.getPrimaryTypeName().isPresent()) {
      LOGGER.fine(() -> cu.getPrimaryTypeName().get());
    }

    for (Replacement replacement : replacements) {
      for (MethodCallExpr methodCall : cu.findAll(MethodCallExpr.class)) {
        Optional<Expression> methodScope = methodCall.getScope();
        if (methodScope.isPresent()) {
          try {
            String methodCallToReplace = replacement.getOldMethod();
            if (isNameMatch(methodCall, methodCallToReplace) && isTypeMatch(replacement, typeOfView(methodScope))) {
              methodCall.setName(replacement.getNewMethod());
              LOGGER.finest(() -> String.format("replace %s -> %s", methodCallToReplace, replacement.getNewMethod()));
              replacement.getNewMethodTypeParameters().forEach(methodCall::addArgument);
              addClassImport(replacement, methodCall);
              save(cu);
              LOGGER.fine(() -> "Saved: " + cu.getPrimaryTypeName());
            }
          } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "in " + cu.getPrimaryTypeName().orElse("Unknown"));
          }
        }
      }
    }
  }

  private static void applyReplacements2(CompilationUnit cu) {
    LOGGER.fine(() -> cu.getPrimaryTypeName().orElse("unknown Class"));
    cu.findAll(MethodCallExpr.class).stream().filter(m -> m.getScope().isPresent()).forEach(methodCall -> {
      for (Replacement replacement : replacements) {
        try {
          String methodCallToReplace = replacement.getOldMethod();
          if (isNameMatch(methodCall, methodCallToReplace) && isTypeMatch(replacement, typeOfView(methodCall.getScope()))) {
            methodCall.setName(replacement.getNewMethod());
            LOGGER.finest(() -> String.format("replace %s -> %s", methodCallToReplace, replacement.getNewMethod()));
            replacement.getNewMethodTypeParameters().forEach(methodCall::addArgument);
            addClassImport(replacement, methodCall);
            save(cu);
            LOGGER.fine(() -> "Saved: " + cu.getPrimaryTypeName());
          }
        } catch (Exception e) {
          LOGGER.warning(e.getMessage() + "in " + cu.getPrimaryTypeName().orElse("Unknown"));
        }
      }
    });
  }

  private static void applyReplacements3(CompilationUnit cu) {
    LOGGER.fine(() -> cu.getPrimaryTypeName().orElse("unknown Class"));

    cu.findAll(MethodCallExpr.class).stream().filter(m -> m.getScope().isPresent()).forEach(methodCall -> {
      for (Replacement replacement : replacements) {
        methodReplacer.applyReplacement(cu, replacement, methodCall);
      }
    });
  }

  private static void applyReplacements4(CompilationUnit cu) {
    LOGGER.fine(() -> cu.getPrimaryTypeName().orElse("unknown Class"));
    cu.findAll(MethodCallExpr.class).stream() //
        .filter(m -> m.getScope().isPresent()) //
        .forEach(methodCall -> {
          replacements.forEach(r -> methodReplacer.applyReplacement(cu, r, methodCall));
        });
  }

  // private static void applyReplacements5() {
  // createParserConfiguration(combinedTypeSolver);
  // getSourceFolders().parallelStream() //
  // .flatMap(folders -> getJavaFiles(folders).parallelStream()) //
  // .map(javaFiles -> {
  // try {
  // return JavaParser.parse(javaFiles);
  // } catch (Exception e) {
  // LOGGER.log(Level.SEVERE, "error parsing java", e);
  // }
  // return null;
  // }) //
  // .map(compilationUnit -> {
  // LOGGER.fine(() -> compilationUnit.getPrimaryTypeName().orElse("unknown Class"));
  // return compilationUnit;
  // }) //
  // .flatMap(c -> c.findAll(MethodCallExpr.class).stream()) //
  // .filter(m -> m.getScope().isPresent()) //
  // .forEach(methodCall -> replacements.forEach(r -> methodReplacer.apply(r, methodCall)));
  // LOGGER.info("-----------------End--------------------");
  // }

  private static ResolvedType typeOfView(Optional<Expression> methodScope) {
    return JavaParserFacade.get(combinedTypeSolver).getType(methodScope.get());
  }

  private static boolean isTypeMatch(Replacement replacement, ResolvedType typeOfView) {
    return typeOfView.isReferenceType() && typeOfView.asReferenceType().getQualifiedName().equals(replacement.getFullQualifiedViewType());
  }

  private static boolean isNameMatch(MethodCallExpr methodCallExpression, String methodCallToReplace) {
    return methodCallToReplace.equals(methodCallExpression.getNameAsString());
  }

  private static void save(CompilationUnit cu) {
    Optional<Storage> storage = cu.getStorage();
    if (storage.isPresent()) {
      storage.get().save();
    }
  }

  private static void addClassImport(Replacement replacement, MethodCallExpr methodCallExpression) {
    replacement.getClassesToImport().forEach(fullQualifiedClassName -> {
      try {
        methodCallExpression.tryAddImportToParentCompilationUnit(Class.forName(fullQualifiedClassName));
      } catch (ClassNotFoundException e) {
        RuntimeException runtimeException = new RuntimeException(e);
        throw runtimeException;
      }
    });
  }

  private static SourceRoot prepareSourceRoot(Path sourceFolder, TypeSolver typeSolver) {
    ParserConfiguration symbolResolver = createParserConfiguration(typeSolver);
    SourceRoot sourceRoot = new SourceRoot(sourceFolder);
    sourceRoot.setParserConfiguration(symbolResolver);
    return sourceRoot;
  }

  private static List<Replacement> createReplacements() {
    List<Replacement> allReplacements = new ArrayList<>();
    // getHintFiles().stream().map(Test2::parseHintFile).forEach(replacements::addAll);
    for (URI hintfile : getHintFiles()) {
      LOGGER.info(() -> "add hintfile" + hintfile.toString());
      allReplacements.addAll(parseHintFile(hintfile));
    }
    return allReplacements;
  }

  private static List<URI> getHintFiles() {
    List<URI> hintfiles = new ArrayList<>();
    try {
      Files.walk(Paths.get(HINT_FILE_FOLDER)).filter(Files::isRegularFile).filter(f -> f.getFileName().toString().endsWith(".hint"))
          .map(Path::toUri).forEach(hintfiles::add);
    } catch (IOException e) {
      RuntimeException runtimeException = new RuntimeException("unable to read hintfile folder: " + HINT_FILE_FOLDER);
      runtimeException.addSuppressed(e);
      throw runtimeException;
    }
    return hintfiles;
  }

  private static List<Replacement> parseHintFile(URI hintFile) {
    HintParser hintParser;
    List<Replacement> parsed;
    try {
      // URI uri = URI.create(HINT_FILE);
      hintParser = HintParserFactory.getHintParser(hintFile);
      parsed = hintParser.parse();

    } catch (Exception e) {
      RuntimeException runtimeException = new RuntimeException("unable to read hintfile: " + hintFile);
      runtimeException.addSuppressed(e);
      throw runtimeException;
    }
    return parsed;
  }

  private static ParserConfiguration createParserConfiguration(TypeSolver typeSolver) {
    JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
    return JavaParser.getStaticConfiguration().setSymbolResolver(symbolSolver);
  }

  private static CombinedTypeSolver createTypeSolvers() throws IOException {
    combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(new ReflectionTypeSolver(false));
    // combinedTypeSolver.add(new JavaParserTypeSolver(
    // new File("C:\\workspace\\landi_frame_head\\tech.bison.jackerpot\\target\\classes\\tech\\bison\\jackerpot")));
    // combinedTypeSolver
    // .add(new JavaParserTypeSolver(new File("C:\\workspace\\landi_frame_head\\Frame\\CH.obj.Core.api\\bin\\CH\\obj\\core\\data\\view")));
    // combinedTypeSolver.add(new JavaParserTypeSolver(
    // new File("C:\\workspace\\landi_frame_head\\Frame\\CH.obj.Core.api\\bin\\CH\\obj\\Libraries\\BusinessLogic")));
    //
    // getJarSolvers().forEach(combinedTypeSolver::add);
    return combinedTypeSolver;
  }

  private static List<JarTypeSolver> getJarSolvers() throws IOException {
    Path path = Paths.get("C:\\workspace\\landi_frame_head\\.metadata\\.ivy-cache\\repository\\Bison Schweiz AG\\");
    return Files.walk(path).filter(Files::isRegularFile).filter(f -> f.getFileName().toString().endsWith(".jar")).map(p -> p.toString())
        .map(arg0 -> {
          try {
            LOGGER.info("add jar: " + arg0);
            return new JarTypeSolver(arg0);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());

  }

  private static List<Path> getSourceFolders() {
    List<Path> rootFolders;
    List<Path> sourceFolders = new ArrayList<>();
    try {
      rootFolders = Files.walk(Paths.get(APPLICATION_ROOT)) //
          .filter(Files::isDirectory) //
          .filter(Test1::isRootFolder) //
          .collect(Collectors.toList());

      for (Path path : rootFolders) {
        sourceFolders.addAll(Files.walk(path) //
            .filter(Files::isDirectory) //
            .filter(isSourceFolder()) //
            .collect(Collectors.toList()));
      }
    } catch (IOException e) {
      LOGGER.severe("Can't read source Folders in " + APPLICATION_ROOT);
      LOGGER.severe(e.getMessage());
    }
    return sourceFolders;
  }

  private static boolean isRootFolder(Path f) {
    for (String wildcard : ROOT_FOLDER_PREFIX) {
      if (f.getFileName().toString().startsWith(wildcard)) {
        return true;
      }
    }
    return false;
  }

  private static Predicate<? super Path> isSourceFolder() {
    return f -> f.getFileName().toString().contains("src");
  }

  private static void applyForAll() {
    // ProgressBar pb = new ProgressBar("Test", getSourceFolders().size()); // name, initial max

    // pb.start(); // the progress bar starts timing
    createParserConfiguration(combinedTypeSolver);
    for (Path path : getSourceFolders()) {
      // pb.setExtraMessage("Converting path" + path.toString());
      LOGGER.info(() -> "entering path " + path.toString());
      getJavaFiles(path).parallelStream().forEach(f -> singleFile(path, f.toString()));
      // pb.step();
    }
    // pb.stop();
    LOGGER.info("-----------------End--------------------");
  }

  private static List<Path> getJavaFiles(Path sourceFolder) {
    List<Path> sourceFiles = new ArrayList<>();
    try {
      sourceFiles.addAll(Files.walk(sourceFolder) //
          .filter(Files::isRegularFile) //
          .filter(f -> f.getFileName().toString().endsWith(".java")).collect(Collectors.toList()));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sourceFiles;
  }

}
