/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.plugin.ij.core;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

public class TypeSystemStartupContributorExtensionBean extends AbstractExtensionPointBean {
  public static final ExtensionPointName<TypeSystemStartupContributorExtensionBean> EP_NAME = new ExtensionPointName<>("com.guidewire.gosu.typesystemStartupContributor");

  @Attribute("class")
  public String className;

  private final LazyInstance<ITypeSystemStartupContributor> myHandler = new LazyInstance<ITypeSystemStartupContributor>() {
    @NotNull
    protected Class<ITypeSystemStartupContributor> getInstanceClass() throws ClassNotFoundException {
      return findClass(className);
    }
  };

  @NotNull
  public ITypeSystemStartupContributor getHandler() {
    return myHandler.getValue();
  }
}
