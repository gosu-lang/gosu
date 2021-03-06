/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.xml.xsd.typeprovider;

import gw.lang.reflect.IAnnotationInfo;
import gw.lang.reflect.IFeatureInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeInfo;
import gw.lang.reflect.java.JavaTypes;

/**
 *
 */
class XmlSchemaAutoinsertAnnotationData implements IAnnotationInfo
{

  private final IFeatureInfo _container;

  public XmlSchemaAutoinsertAnnotationData( IFeatureInfo container ) {
    _container = container;
  }

  @Override
  public Object getInstance() {
    ITypeInfo autoinsertTypeInfo = JavaTypes.AUTOINSERT().getTypeInfo();
    return autoinsertTypeInfo.getConstructor().getConstructor().newInstance();
  }

  @Override
  public Object getFieldValue(String field) {
    throw new RuntimeException("Not supported yet");
  }

  @Override
  public IType getType() {
    return JavaTypes.AUTOINSERT();
  }

  @Override
  public String getName() {
    return JavaTypes.AUTOINSERT().getName();
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public IType getOwnersType() {
    return _container.getOwnersType();
  }
}