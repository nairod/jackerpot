
package tech.bison.jackerpot;

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
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import tech.bison.jackerpot.hintparser.HintParser;
import tech.bison.jackerpot.hintparser.HintParserFactory;
import tech.bison.jackerpot.source.Replacement;

public class SourceWalker {
  private static final Logger LOGGER = Logger.getLogger(SourceWalker.class.getName());
  private static final String APPLICATION_ROOT = "C:\\workspace\\landi_frame_head\\Application";
  private static final String HINT_FILE_FOLDER = "C:\\workspace\\landi_frame_head\\tech.bison.jackerpot\\hintfiles";
  private static final List<String> ROOT_FOLDER_PREFIX = Arrays.asList("CH.fenaco", "CH.obj.Application", "xModultests");
  private static List<Replacement> replacements;
  private static CombinedTypeSolver combinedTypeSolver;
  private static MethodCallReplacer methodReplacer;

  public static void main(String[] args) {
    LOGGER.setLevel(Level.INFO);

    List<Path> sourceFolders = getSourceFolders();
    sourceFolders.forEach(f -> LOGGER.info("added folder " + f.toString()));

    createParserConfiguration(combinedTypeSolver);
    combinedTypeSolver = createTypeSolvers();
    methodReplacer = new MethodCallReplacer(combinedTypeSolver);
    replacements = createReplacements();

    LOGGER.info(sourceFolders::toString);
    sourceFolders.parallelStream().forEach(path -> {
      LOGGER.info(() -> "entering path " + path.toString());
      List<Path> javaFiles = getJavaFiles(path);
      javaFiles.parallelStream() //
          .map(SourceWalker::parseFile) //
          .filter(Optional<CompilationUnit>::isPresent) //
          .map(Optional<CompilationUnit>::get) //
          .forEach(SourceWalker::applyReplacements);
    });
    LOGGER.info("---The End. Now let's try to compile. ---");
  }

  private static Optional<CompilationUnit> parseFile(Path javaFile) {
    try {
      return Optional.of(JavaParser.parse(javaFile));
    } catch (NoClassDefFoundError e) {
      LOGGER.severe(() -> "Class not found: " + e.getMessage());
      LOGGER.fine(() -> Arrays.toString(e.getStackTrace()));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e, () -> "could not load file " + javaFile.toString());
    }
    return Optional.empty();
  }

  private static void applyReplacements(CompilationUnit cu) {
    LOGGER.fine(() -> cu.getPrimaryTypeName().orElse("unknown Class"));
    cu.findAll(MethodCallExpr.class).stream() //
        .filter(m -> m.getScope().isPresent()) //
        .forEach(methodCall -> replacements.forEach(r -> methodReplacer.applyReplacement(cu, r, methodCall)));
  }

  private static List<Replacement> createReplacements() {
    List<Replacement> allReplacements = new ArrayList<>();
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

  private static CombinedTypeSolver createTypeSolvers() {
    combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(new ReflectionTypeSolver(false));
    return combinedTypeSolver;
  }

  private static List<Path> getSourceFolders() {
    List<Path> rootFolders;
    List<Path> sourceFolders = new ArrayList<>();
    try {
      rootFolders = Files.walk(Paths.get(APPLICATION_ROOT)) //
          .filter(Files::isDirectory) //
          .filter(SourceWalker::isRootFolder) //
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

  private static List<Path> getJavaFiles(Path sourceFolder) {
    List<Path> sourceFiles = new ArrayList<>();
    try {
      sourceFiles.addAll(Files.walk(sourceFolder) //
          .filter(Files::isRegularFile) //
          .filter(f -> f.getFileName().toString().endsWith(".java")).collect(Collectors.toList()));
    } catch (IOException e) {
      LOGGER.severe("Can't read files in " + sourceFolder.toString());
      LOGGER.log(Level.FINE, "Can't read files in " + sourceFolder.toString(), e);
    }
    return sourceFiles;
  }
}
