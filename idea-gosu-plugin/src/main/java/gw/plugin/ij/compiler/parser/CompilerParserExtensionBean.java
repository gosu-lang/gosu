/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.compiler.parser;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

public class CompilerParserExtensionBean extends AbstractExtensionPointBean {
  static final ExtensionPointName<CompilerParserExtensionBean> EP_NAME = new ExtensionPointName<>("com.guidewire.gosu.compilerParser");

  @Attribute("class")
  public String className;

  private final LazyInstance<ICompilerParser> myHandler = new LazyInstance<ICompilerParser>() {
    @NotNull
    protected Class<ICompilerParser> getInstanceClass() throws ClassNotFoundException {
      return findClass(className);
    }
  };

  @NotNull
  public ICompilerParser getHandler() {
    return myHandler.getValue();
  }
}
