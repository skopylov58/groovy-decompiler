/**
(c) Sergey Kopylov
skopylov@gmail.com
*/

package com.github.skopylov58.groovy.decompiler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.PrettyPrinter;

public class GroovyDecompiler {
  
  public static void main(String[] args) throws Exception {
    
    if (args.length != 2) {
      usage();
      return;
    }
    
    String [] symbols = loadCallSite(args[0]);
    GroovyVisitor gv = new GroovyVisitor(symbols);

    JavaParser p = new JavaParser();
    ParseResult<CompilationUnit> parseResult = p.parse(Paths.get(args[1]));
    
    if (!parseResult.isSuccessful()) {
      parseResult.getProblems().forEach(System.out::println);
      return;
    }
    
    parseResult.ifSuccessful( u -> {
      u.accept(gv, new Object());
      System.out.println(u);
    });
    
//    DotPrinter printer = new DotPrinter(true);
//     printer.output(block);
    
//    BlockStmt block = StaticJavaParser.parseBlock(code);
//    block.accept(gv, Integer.valueOf(0));
//    System.out.println(block);

  }

  private static void usage() {
    String usage = "Usage: java -jar groovy-decompiler.jar fileName1 filename2\n" +
                   "where:\n" +
                   " -  filename1 - path to the file decompiled by 'javap -c -p' command\n" +
                   " -  filename2 - path to the file decompiled by one of CFR, Procyon or Fernflower decompiler\n";
    System.out.println(usage);
  }

  public static String[] loadCallSite(Stream<String> lines) {

    AtomicBoolean ab = new AtomicBoolean(false);
    Consumer<String> latch = s -> {
      if (s.contains("private static void $createCallSiteArray_1")) {
        ab.set(true);
      } else if (ab.get() && s.trim().isEmpty()) {
        ab.set(false);
      }
    };
    
    var list = lines
        .peek(latch)
        .filter(s -> ab.get())
        .filter(s -> s.matches(".*ldc.*"))
        .filter(s -> s.contains("String"))
        .map(s -> s.split("\\s+")[6])
        .toList();
    
    return list.toArray(new String[0]);
  }
  
  public static String[] loadCallSite(String fileName) throws Exception {
    List<String> allLines = Files.readAllLines(Paths.get(fileName));
    return loadCallSite(allLines.stream());
  }
}
