
package tech.bison.jackerpot;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import tech.bison.jackerpot.hintparser.HintParser;
import tech.bison.jackerpot.hintparser.HintParserFactory;
import tech.bison.jackerpot.source.Replacement;

public class ExtensionMethodReplacer {
  private static final String APPLICATION_ROOT = "C:\\workspace\\landi_frame_head\\Application";
  private static final String HINT_FILE_FOLDER = "C:\\workspace\\landi_frame_head\\tech.bison.jackerpot\\hintfiles";
  private static final List<String> ROOT_FOLDER_PREFIX = Arrays.asList("CH.fenaco", "CH.obj.Application", "Modultests");
  private static final Logger LOGGER = Logger.getLogger(ExtensionMethodReplacer.class.getName());
  private static List<Replacement> replacements;
  private static TypeSolver combinedTypeSolver;

  public static void main(String[] args) throws IOException {
    replacements = createReplacements();
    combinedTypeSolver = createTypeSolvers();

    List<Path> sourceFolders = getSourceFolders();
    LOGGER.info(sourceFolders.toString());

    // for (Path sourceFolder : sourceFolders) {
    // SourceRoot sourceRoot = prepareSourceRoot(sourceFolder, combinedTypeSolver);
    //
    // try {
    // sourceRoot.parseParallelized("", (Path localPath, Path absolutePath, ParseResult<CompilationUnit> result) -> {
    // if (result.getResult().isPresent() && result.isSuccessful()) {
    // applyReplacements(combinedTypeSolver, result.getResult().get(), replacements);
    // }
    // return null;
    // });
    // } catch (NoClassDefFoundError e) {
    // e.printStackTrace();
    // }
    //
    // }

    applyForAll();

    // Path sourceFolder = Paths.get("C:\\workspace\\landi_frame_head\\Application\\CH.fenaco\\src\\CH\\fenaco\\RLdi\\quattro\\behaviour");
    // singleFile(replacements, combinedTypeSolver, sourceFolder);

  }

  private static void singleFile(Path sourceFolder, String javaFile) {

    try {
      // CompilationUnit cu = sourceRoot.parse("", javaFile);

      CompilationUnit cu = JavaParser.parse(Paths.get(javaFile));

      applyReplacements(cu);
    } catch (NoClassDefFoundError e) {
      e.printStackTrace();
    }
    // sourceRoot.saveAll();
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void applyReplacements(CompilationUnit cu) {
    if (cu.getPrimaryTypeName().isPresent()) {
      LOGGER.info(cu.getPrimaryTypeName().get());
    }

    for (Replacement replacement : replacements) {
      for (MethodCallExpr methodCallExpression : cu.findAll(MethodCallExpr.class)) {
        if (methodCallExpression.getScope().isPresent()) {
          try {
            String methodCallToReplace = replacement.getOldMethod();
            boolean isNameMatch = methodCallToReplace.equals(methodCallExpression.getNameAsString());
            if (isNameMatch) {
              ResolvedType typeOfView = JavaParserFacade.get(combinedTypeSolver).getType(methodCallExpression.getScope().get());
              boolean isTypeMatch = typeOfView.isReferenceType()
                  && typeOfView.asReferenceType().getQualifiedName().equals(replacement.getFullQualifiedViewType());
              if (isTypeMatch) {
                methodCallExpression.setName(replacement.getNewMethod());
                // LOGGER.info(String.format("replace %s -> %s", replacement.getOldMethod(), replacement.getNewMethod()));
                replacement.getNewMethodTypeParameters().forEach(methodCallExpression::addArgument);
                replacement.getClassesToImport().forEach(fullQualifiedClassName -> {
                  try {
                    methodCallExpression.tryAddImportToParentCompilationUnit(Class.forName(fullQualifiedClassName));
                  } catch (ClassNotFoundException e) {
                    RuntimeException runtimeException = new RuntimeException(e);
                    throw runtimeException;
                  }
                });
                cu.getStorage().get().save();
                LOGGER.info("Save: " + cu.getPrimaryTypeName());
              }
            }
          } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "in " + cu.getPrimaryTypeName().orElse("Unknown"));
          }
        }
      }
    }
  }

  private static SourceRoot prepareSourceRoot(Path sourceFolder, TypeSolver typeSolver) {
    ParserConfiguration symbolResolver = createParserConfiguration(typeSolver);
    SourceRoot sourceRoot = new SourceRoot(sourceFolder);
    sourceRoot.setParserConfiguration(symbolResolver);
    return sourceRoot;
  }

  private static List<Replacement> createReplacements() {
    List<Replacement> allReplacements = new ArrayList<>();
    // getHintFiles().stream().map(Jackerpot::parseHintFile).forEach(replacements::addAll);
    for (URI hintfile : getHintFiles()) {
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
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
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
          .filter(ExtensionMethodReplacer::isRootFolder) //
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
    createParserConfiguration(combinedTypeSolver);
    for (Path path : getSourceFolders()) {
      getJavaFiles(path).parallelStream().forEach(f -> singleFile(path, f.toString()));
    }
    LOGGER.info("-----------------End--------------------");
  }

  private static List<Path> getJavaFiles(Path sourceFolder) {
    List<Path> sourceFiles = new ArrayList<>();
    try {
      sourceFiles.addAll(Files.walk(sourceFolder).filter(Files::isRegularFile).filter(f -> f.getFileName().toString().endsWith(".java"))
          .collect(Collectors.toList()));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sourceFiles;
  }

}
