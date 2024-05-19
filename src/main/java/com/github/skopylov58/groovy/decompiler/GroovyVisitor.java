package com.github.skopylov58.groovy.decompiler;

import java.util.ArrayList;
import java.util.Optional;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class GroovyVisitor extends VoidVisitorAdapter<Object>{
  
  private final String[] symbols;
  
  public GroovyVisitor(String[] symbols) {
    this.symbols = symbols;
  }
  
    public void visit(MethodCallExpr mc, Object arg) {
      super.visit(mc, arg);
      //System.out.println(mc);

      String identifier = mc.getName().getIdentifier();
      NodeList<Expression> arguments = mc.getArguments();
      NodeList<Expression> rest = new NodeList<>(arguments.stream().skip(1).toList());
      
      if (identifier.equals("callGroovyObjectGetProperty")) {
        NameExpr symbolExpr = new NameExpr(new SimpleName(getSymbol(mc)));
        mc.replace(symbolExpr);
      } else if (identifier.equals("call")) {
        NameExpr symbolExpr = new NameExpr(new SimpleName(getSymbol(mc)));
        Expression target = arguments.get(0);
        MethodCallExpr call = new MethodCallExpr(target, symbolExpr.getNameAsString(), rest);
        mc.replace(call);
      } else if (identifier.equals("callCurrent")) {
        NameExpr symbolExpr = new NameExpr(new SimpleName(getSymbol(mc)));
        MethodCallExpr call = new MethodCallExpr(new ThisExpr(), symbolExpr.getNameAsString(), rest);
        mc.replace(call);
      } else if (identifier.equals("callConstructor")) {
        Expression target = arguments.get(0);
        ObjectCreationExpr oc = new ObjectCreationExpr();
        oc.setType(new ClassOrInterfaceType(target.toString().replace(".class", "")));
        oc.setArguments(rest);
        mc.replace(oc);
      } else if (identifier.equals("callGetProperty")) {
        NameExpr targetExpr = new NameExpr(new SimpleName(arguments.get(0).toString().replace(".class", "")));
        FieldAccessExpr fa = new FieldAccessExpr();
        fa.setScope(targetExpr);
        fa.setName(getSymbol(mc));
        mc.replace(fa);
      } else if (identifier.equals("callStatic")) {
        NameExpr symbolExpr = new NameExpr(new SimpleName(getSymbol(mc)));
        NameExpr targetExpr = new NameExpr(new SimpleName(arguments.get(0).toString().replace(".class", "")));
        MethodCallExpr call = new MethodCallExpr(targetExpr, symbolExpr.getNameAsString(), rest);
        mc.replace(call);
      }
    }

    public String getSymbol(MethodCallExpr mc) {
      Optional<Expression> scope = mc.getScope();
      
      return scope.map(e -> (ArrayAccessExpr) e)
      .map(e -> (IntegerLiteralExpr) e.getIndex())
      .map(e -> e.getValue())
      .map(Integer::parseInt)
      .map(i -> symbols[i])
      .orElse("N/A")
      ;

//      if (scope.isPresent()) {
//      
//      ArrayAccessExpr aee = (ArrayAccessExpr) scope.get();
//      IntegerLiteralExpr index = (IntegerLiteralExpr) aee.getIndex();
//      int i = Integer.parseInt(index.getValue());
//      return symbols.get(i);
//      } else {
//        return "N/A";
//      }
    }

}
