/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.sdk;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import gw.plugin.ij.compiler.parser.ICompilerParser;
import org.jetbrains.annotations.NotNull;

public class SDKCreatorExtensionBean extends AbstractExtensionPointBean {
  public static final ExtensionPointName<SDKCreatorExtensionBean> EP_NAME = new ExtensionPointName<>("com.guidewire.gosu.sdkCreator");

  @Attribute("class")
  public String className;

  private final LazyInstance<ISDKCreator> myHandler = new LazyInstance<ISDKCreator>() {
    @NotNull
    protected Class<ISDKCreator> getInstanceClass() throws ClassNotFoundException {
      return findClass(className);
    }
  };

  @NotNull
  public ISDKCreator getCreator() {
    return myHandler.getValue();
  }
}
