/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.gosu.parser.java.classinfo;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import gw.internal.gosu.parser.GenericTypeVariable;
import gw.internal.gosu.parser.TypeLord;
import gw.lang.parser.TypeVarToTypeMap;
import gw.lang.reflect.FunctionType;
import gw.lang.reflect.IAnnotationInfo;
import gw.lang.reflect.IFeatureInfo;
import gw.lang.reflect.IParameterInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.SimpleParameterInfo;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGenericTypeVariable;
import gw.lang.reflect.java.ClassInfoUtil;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.java.IJavaClassMethod;
import gw.lang.reflect.java.IJavaClassType;
import gw.lang.reflect.java.IJavaClassTypeVariable;
import gw.lang.reflect.java.IJavaMethodInfo;
import gw.lang.reflect.java.ITypeInfoResolver;
import gw.lang.reflect.module.IModule;

import javax.lang.model.element.Name;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static gw.internal.gosu.parser.java.classinfo.JavaSourceType.getTypeName;

public class JavaSourceMethod implements IJavaClassMethod, ITypeInfoResolver {
  protected MethodTree _method;
  protected JavaSourceType _containingClass;
  protected JavaSourceParameter[] _parameters;
  protected IJavaClassType _genericReturnType;
  protected JavaSourceModifierList _modifierList;
  protected IJavaClassTypeVariable[] _typeParameters;
  protected IJavaClassType[] _genericParameterTypes;
  protected IJavaClassInfo[] _parameterTypes;
  protected IJavaClassInfo _returnType;

  public JavaSourceMethod(MethodTree method, JavaSourceType containingClass) {
    _containingClass = containingClass;
    _method = method;
  }

  @Override
  public IJavaClassInfo getReturnClassInfo() {
    if (_returnType == null) {
      final Tree returnType = _method.getReturnType();
      _returnType = (IJavaClassInfo) JavaSourceType.createType(this, getTypeName(returnType), JavaSourceType.IGNORE_NONE).getConcreteType();
      if (_returnType == null) {
        throw new RuntimeException("Cannot compute return type.");
      }
    }
    return _returnType;
  }

  public IJavaClassType getGenericReturnType() {
    if (_genericReturnType == null) {
      final Tree returnType = _method.getReturnType();
      _genericReturnType = JavaSourceType.createType(this, returnType);
      if (_genericReturnType == null) {
        throw new RuntimeException("Cannot compute return type.");
      }
    }
    return _genericReturnType;
  }

  @Override
  public String getReturnTypeName() {
    IJavaClassType type = getGenericReturnType();
    if (type instanceof JavaArrayClassInfo ) {
      return "[" + ((JavaArrayClassInfo) type).getComponentType().getNameSignature();
    } else {
      return type.getName();
    }
  }

  @Override
  public IType getReturnType() {
    return getReturnClassInfo().getJavaType();
  }

  @Override
  public IJavaClassType[] getGenericParameterTypes() {
    if (_genericParameterTypes == null) {
      _genericParameterTypes = initGenericParameterTypes();
    }
    return _genericParameterTypes;
  }

  @Override
  public IJavaClassInfo[] getParameterTypes() {
    if (_parameterTypes == null) {
      _parameterTypes = initParameterTypes();
    }
    return _parameterTypes;
  }

  public String getName() {
    return _method.getName().toString();
  }

  public JavaSourceParameter[] getParameters() {
    if (_parameters == null) {
      List<? extends VariableTree> parameters = _method.getParameters();
      if (parameters.isEmpty()) {
        _parameters = new JavaSourceParameter[0];
      } else {
        _parameters = new JavaSourceParameter[parameters.size()];
        for (int i = 0; i < _parameters.length; i++) {
          _parameters[i] = new JavaSourceParameter(this, parameters.get(i));
        }
      }
    }
    return _parameters;
  }

  public boolean isConstructor() {
    return false;
  }

  public JavaSourceType getEnclosingClass() {
    return _containingClass;
  }

  @Override
  public int getModifiers() {
    return getModifierList().getModifiers();
  }

  public IModifierList getModifierList() {
    if (_modifierList == null) {
      _modifierList = new JavaSourceModifierList(this, _method.getModifiers());
    }
    return _modifierList;
  }

  public IJavaClassInfo[] getExceptionTypes() {
    return new IJavaClassInfo[0];
  }

  @Override
  public IJavaClassType resolveType(String relativeName, int ignoreFlags) {
    IJavaClassTypeVariable[] typeParameters = getTypeParameters();
    for (IJavaClassTypeVariable typeParameter : typeParameters) {
      if (relativeName.equals(typeParameter.getName())) {
        return typeParameter;
      }
    }

    return _containingClass.resolveType(relativeName, ignoreFlags);
  }

  @Override
  public IJavaClassType resolveType(String relativeName, IJavaClassInfo whosAskin, int ignoreFlags) {
    return null;
  }

  @Override
  public IJavaClassType resolveImport(String relativeName) {
    return null;
  }

  @Override
  public IModule getModule() {
    return _containingClass.getModule();
  }

  public String toString() {
    return getName() + "(...)";
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return getModifierList().isAnnotationPresent(annotationClass);
  }

  @Override
  public IAnnotationInfo getAnnotation(Class annotationClass) {
    return getModifierList().getAnnotation(annotationClass);
  }

  @Override
  public IAnnotationInfo[] getDeclaredAnnotations() {
    return getModifierList().getAnnotations();
  }

  protected IJavaClassInfo[] initParameterTypes() {
    JavaSourceParameter[] psiParameters = getParameters();
    IJavaClassInfo[] paramTypes = new IJavaClassInfo[psiParameters.length];
    for (int i = 0; i < psiParameters.length; i++) {
      paramTypes[i] = psiParameters[i].getType();
    }
    return paramTypes;
  }

  protected IJavaClassType[] initGenericParameterTypes() {
    JavaSourceParameter[] parameters = getParameters();
    IJavaClassType[] types = new IJavaClassType[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      JavaSourceParameter parameter = parameters[i];
      types[i] = parameter.getGenericType();
    }
    return types;
  }

  @Override
  public Object getDefaultValue() {
    throw new RuntimeException("Should never be called");
  }

  @Override
  public Object invoke(Object ctx, Object[] args) throws InvocationTargetException, IllegalAccessException {
    throw new RuntimeException("Not supported");
  }

  public IJavaClassTypeVariable[] getTypeParameters() {
    if (_typeParameters == null) {
      final List<? extends TypeParameterTree> typeParameters = _method.getTypeParameters();
      if (!typeParameters.isEmpty()) {
        _typeParameters = new IJavaClassTypeVariable[typeParameters.size()];
        for (int i = 0; i < _typeParameters.length; i++) {
          _typeParameters[i] = JavaSourceTypeVariable.create(this, typeParameters.get(i));
        }
      } else {
        _typeParameters = JavaSourceTypeVariable.EMPTY;
      }
    }
    return _typeParameters;
  }

  @Override
  public IGenericTypeVariable[] getTypeVariables(IJavaMethodInfo javaMethodInfo) {
    IJavaClassTypeVariable[] rawTypeParams = getTypeParameters();
    IType declClass = TypeSystem.get( getEnclosingClass() );
    TypeVarToTypeMap actualParamByVarName = TypeLord.mapTypeByVarName( declClass, declClass );
    FunctionType functionType = new FunctionType(javaMethodInfo, true);
    return GenericTypeVariable.convertTypeVars( functionType, rawTypeParams, actualParamByVarName );
  }

  @Override
  public boolean isBridge() {
    return false;
  }

  @Override
  public boolean isSynthetic() {
    return false;
  }

  public static JavaSourceMethod create(MethodTree method, JavaSourceType containingClass) {
    final Name name = method.getName();
    if(name == null) {
      return null;
    }
    String methodName = name.toString();
    if (methodName.equals("<init>")) {
      return new JavaSourceConstructor(method, containingClass);
    } else if (containingClass.isAnnotation()) {
      return new JavaSourceAnnotationMethod(method, containingClass);
    } else {
      return new JavaSourceMethod(method, containingClass);
    }
  }

  protected IParameterInfo[] getActualParameterInfos(IFeatureInfo container, TypeVarToTypeMap actualParamByVarName, boolean bKeepTypeVars) {
    IType[] paramTypes = ClassInfoUtil.getActualTypes(getGenericParameterTypes(), actualParamByVarName, bKeepTypeVars);
    IParameterInfo[] paramInfos = new IParameterInfo[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
      paramInfos[i] = new SimpleParameterInfo(container, paramTypes[i], i);
    }
    return paramInfos;
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode( _parameterTypes );
    result = 31 * result + _returnType.hashCode();
    result = 31 * result + getName().hashCode();
    return result;
  }

  public boolean equals( Object o ) {
    if( !(o instanceof IJavaClassMethod) ) {
      return false;
    }

    IJavaClassMethod jcm = (IJavaClassMethod)o;
    return getName().equals( jcm.getName() ) &&
           getReturnType() == jcm.getReturnType() &&
           Arrays.equals( getParameterTypes(), jcm.getParameterTypes() );
  }

  @Override
  public int compareTo( IJavaClassMethod o ) {
    return getName().compareTo( o.getName() );
  }
}
