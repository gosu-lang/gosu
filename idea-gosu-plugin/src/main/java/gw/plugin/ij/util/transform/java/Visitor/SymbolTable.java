/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.util.transform.java.Visitor;

import java.util.*;

public class SymbolTable {
  private LinkedList<Scope> globals;
  private LinkedList<Scope> locals;
  private HashSet<String> reservedWords;
  int counter;
  private String[] reserved = {"true", "false", "NaN", "Infinity", "and", "or", "not", "null", "length", "exists",
                              "in", "startswith", "contains", "where", "find", "var", "delegate", "represents",
                              "as", "typeof", "statictypeof", "typeis", "typeas", "package", "uses", "if", "else",
                              "except", "unless", "foreach", "for", "index", "iterator", "while", "do",
                              "continue", "break", "return", "construct", "function", "property", "get", "set",
                              "try", "catch", "finally", "this", "throw", "new", "switch", "case", "default",
                              "eval", "private", "internal", "protected", "public", "abstract", "override",
                              "hide", "final", "static", "extends", "transient", "implements", "readonly",
                              "class", "interface", "structure", "enum", "super", "outer", "execution", "request", "session",
                              "application", "void", "block", "enhancement", "classpath", "typeloader", "using", "now"};

  private class Scope {
    String clazz;
    Set<String> idents;
    Map<String, String> conversions;


    Scope() {
      idents = new HashSet<String>();
      conversions = new HashMap<String, String>();
    }

    Scope(String clazz) {
      this.clazz = clazz;
      idents = new HashSet<String>();
      conversions = new HashMap<String, String>();
    }
  }

  public SymbolTable() {
    counter = 0;
    globals = new LinkedList<Scope>();
    locals = new LinkedList<Scope>();
    reservedWords = new HashSet<String>();
    for(String word : reserved) {
      reservedWords.add(word);
    }
  }

  void pushGlobalScope(String clazz) {
    globals.add(new Scope(clazz));
  }

  void pushLocalScope() {
    locals.add(new Scope());
  }

  public String convertLocalSymbol(String ident) {
    Scope s;
    int end = locals.size() - 1;
    String converted = null;

    while (end >= 0) {
      s = locals.get(end);
      converted = s.conversions.get(ident);
      if (converted != null) {
        break;
      }
      end--;
    }
    return converted == null ? ident : converted;
  }

  String addLocally(String ident) {
    String original = ident;
    int i = 0;
    while (isDefinedLocally(ident) || isDefinedGlobally(ident) || isReserved(ident)) {
      ident = original + "_" + i;
      i++;
    }
    Scope last;
    try {
      last = locals.getLast();
    } catch (NoSuchElementException ex) {
      throw new RuntimeException("Error: adding a new identifier in an empty scope." +
              "Have you forgotten to pushNewScope() first?");
    }
    last.idents.add(ident);
    if (!original.equals(ident)) {
      last.conversions.put(original, ident);
    }
    return ident;
  }

  private boolean isReserved(String ident) {
    return reservedWords.contains(ident);
  }

  void popLocalScope() {
    try {
      locals.removeLast();
    } catch (NoSuchElementException ex) {
      throw new RuntimeException("Error: the scope's stack is empty." +
              "Have you forgotten to pushNewScope() first?");
    }
  }

  void popGlobalScope() {
    try {
      globals.removeLast();
    } catch (NoSuchElementException ex) {
      throw new RuntimeException("Error: the scope's stack is empty." +
              "Have you forgotten to pushNewScope() first?");
    }
  }

  private boolean isDefinedLocally(String ident) {
    int end = locals.size() - 1;
    while (end >= 0) {
      Scope s = locals.get(end);
      if (s.idents.contains(ident)) {
        return true;
      }
      end--;
    }
    return false;
  }

  private boolean isDefinedGlobally(String ident) {
    int end = globals.size() - 1;
    while (end >= 0) {
      Scope s = globals.get(end);
      if (s.idents.contains(ident)) {
        return true;
      }
      end--;
    }
    return false;
  }

  public int getClassLevelFromCurrent(String clazz) {
    int end = globals.size() - 1;
    int level = end;
    while (end >= 0) {
      Scope s = globals.get(end);
      if (s.clazz.equals(clazz)) {
        level -= end;
        break;
      }
      end--;
    }
    return level;
  }

  public String addGlobally(String ident) {
    Scope last;
    try {
      last = globals.getLast();
    } catch (NoSuchElementException ex) {
      throw new RuntimeException("Error: adding a new identifier in an empty scope." +
              "Have you forgotten to pushNewScope() first?");
    }
    last.idents.add(ident);
    return ident;
  }
}

