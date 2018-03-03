
package tech.bison.jackerpot.experimental;

import java.io.File;
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
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.CompilationUnit.Storage;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;

import tech.bison.jackerpot.hintparser.HintParser;
import tech.bison.jackerpot.hintparser.HintParserFactory;
import tech.bison.jackerpot.source.Replacement;

public class Test2 {
  private static final List<String> ROOT_FOLDER_PREFIX = Arrays.asList("CH.fenaco", "CH.obj.Application", "Modultests");
  private static final String HINT_FILE = "file:/C:/workspace/landi_frame_head/tech.bison.jackerpot/hintfiles/QuattroItemExtension.adjusted.hint";
  private static final String HINT_FILE_FOLDER = "C:\\workspace\\landi_frame_head\\tech.bison.jackerpot\\hintfiles";
  private static final String APPLICATION_ROOT = "C:\\workspace\\landi_frame_head\\Application";
  private static final Logger LOGGER = Logger.getLogger(Test2.class.getName());
  private static TypeSolver typeSolvers;
  private static List<Replacement> replacements;

  public static void main(String[] args) throws IOException {
    LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.INFO);

    typeSolvers = createTypeSolvers();
    replacements = createReplacements();

    for (Path sourceFolder : getSourceFolders()) {
      LOGGER.info(() -> "entering " + sourceFolder);

      SourceRoot sourceRoot = prepareSourceRoot(sourceFolder);
      List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParseParallelized();
      parseResults.stream() //
          .filter(result -> result.isSuccessful() && result.getResult().isPresent()) //
          .map(result -> result.getResult().get()) //
          .filter(cu -> !cu.getPrimaryTypeName().get().contains("Condition")) // workaround
          .forEach(Test2::addVisitor);

      LOGGER.info(() -> "exiting " + sourceFolder);
    }
  }

  private static void addVisitor(CompilationUnit cu) {
    LOGGER.fine(() -> "add visitor for " + cu.getPrimaryTypeName());
    MethodReplacerVisitor methodReplacerVisitor = new MethodReplacerVisitor(replacements, typeSolvers);
    cu.accept(methodReplacerVisitor, null);
    Optional<Storage> storage = cu.getStorage();
    if (storage.isPresent() && methodReplacerVisitor.needSave()) {
      storage.get().save();
    }
  }

  private static SourceRoot prepareSourceRoot(Path sourceFolder) {
    ParserConfiguration symbolResolver = createParserConfiguration(typeSolvers);
    SourceRoot sourceRoot = new SourceRoot(sourceFolder);
    sourceRoot.setParserConfiguration(symbolResolver);
    return sourceRoot;
  }

  private static List<Replacement> createReplacements() {
    List<Replacement> allReplacements = new ArrayList<>();
    // getHintFiles().stream().map(Test2::parseHintFile).forEach(replacements::addAll);
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

  // private static CombinedTypeSolver createTypeSolvers() {
  // CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
  // combinedTypeSolver.add(new ReflectionTypeSolver(false));
  // return combinedTypeSolver;
  // }

  private static CombinedTypeSolver createTypeSolvers() throws IOException {
    CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(new ReflectionTypeSolver(false));
    combinedTypeSolver.add(new JavaParserTypeSolver(
        new File("C:\\workspace\\landi_frame_head\\tech.bison.jackerpot\\target\\classes\\tech\\bison\\jackerpot")));
    combinedTypeSolver
        .add(new JavaParserTypeSolver(new File("C:\\workspace\\landi_frame_head\\Frame\\CH.obj.Core.api\\bin\\CH\\obj\\core\\data\\view")));
    combinedTypeSolver.add(new JavaParserTypeSolver(
        new File("C:\\workspace\\landi_frame_head\\Frame\\CH.obj.Core.api\\bin\\CH\\obj\\Libraries\\BusinessLogic")));

    getJarSolvers().forEach(combinedTypeSolver::add);
    return combinedTypeSolver;
  }

  private static List<JarTypeSolver> getJarSolvers() throws IOException {
    Path path = Paths.get("C:\\workspace\\landi_frame_head\\.metadata\\.ivy-cache\\repository\\Bison Schweiz AG\\");
    return Files.walk(path).filter(Files::isRegularFile).filter(f -> f.getFileName().toString().endsWith(".jar")).map(Path::toString)
        .map(arg0 -> {
          try {
            LOGGER.info("add jar: " + arg0);
            return new JarTypeSolver(arg0);
          } catch (IOException e) {
            RuntimeException runtimeException = new RuntimeException("Error while reading jars");
            runtimeException.addSuppressed(e);
            throw runtimeException;
          }
        }).collect(Collectors.toList());

  }

  private static List<Path> getSourceFolders() {
    List<Path> rootFolders;
    List<Path> sourceFolders = new ArrayList<>();
    try {
      rootFolders = Files.walk(Paths.get(APPLICATION_ROOT)) //
          .filter(Files::isDirectory) //
          .filter(Test2::isRootFolder) //
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

}
