/*
 * Copyright 2013 Guidewire Software, Inc.
 */

package gw.internal.xml.xsd.typeprovider;

import gw.config.CommonServices;
import gw.internal.xml.XmlElementInternals;
import gw.internal.xml.XmlTypeInstanceInternals;
import gw.internal.xml.xsd.typeprovider.schema.XmlSchemaElement;
import gw.internal.xml.xsd.typeprovider.schema.XmlSchemaObject;
import gw.internal.xml.xsd.typeprovider.schema.XmlSchemaType;
import gw.lang.reflect.ConstructorInfoBuilder;
import gw.lang.reflect.IConstructorHandler;
import gw.lang.reflect.IConstructorInfo;
import gw.lang.reflect.ILocationAwareFeature;
import gw.lang.reflect.IMethodCallHandler;
import gw.lang.reflect.IMethodInfo;
import gw.lang.reflect.IPropertyAccessor;
import gw.lang.reflect.IPropertyInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.LocationInfo;
import gw.lang.reflect.MethodInfoBuilder;
import gw.lang.reflect.ParameterInfoBuilder;
import gw.lang.reflect.PropertyInfoBuilder;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IAsmJavaClassInfo;
import gw.lang.reflect.java.IJavaClassConstructor;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.java.JavaTypes;
import gw.util.GosuExceptionUtil;
import gw.util.concurrent.LockingLazyVar;
import gw.xml.XmlElement;
import gw.xml.XmlParseOptions;
import gw.xml.XmlTypeInstance;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IType for statically typed XmlElement types.
 */
public class XmlSchemaElementTypeData<T> extends XmlSchemaTypeData<T> implements IXmlSchemaElementTypeData<T>, ILocationAwareFeature {

  private static final String TYPEINSTANCE_PROPERTY_NAME = "$TypeInstance";
  private static final String QNAME_PROPERTY_NAME = "$QNAME";

  private final T _context;
  private final XmlSchemaResourceTypeLoaderBase<T> _typeLoader;
  private final String _typeName;
  private XmlSchemaType _xsdType;
  private final XmlSchemaElement _xsdElement;
  private final boolean _anonymousElement;
  private LockingLazyVar<IType> _superType = new LockingLazyVar<IType>() {
    @Override
    protected IType init() {
      if ( _superTypeData != null ) {
        return _superTypeData.getType();
      }
      if ( _xsdElement.getSubstitutionGroup() != null ) {
        return XmlSchemaIndex.getGosuTypeBySchemaObject(_xsdElement.getSchemaIndex().getXmlSchemaElementByQName(_xsdElement.getSubstitutionGroup()));
      }
      return TypeSystem.get( XmlElement.class );
    }
  };
  private final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
  private final XmlSchemaElementTypeData _superTypeData;

  public XmlSchemaElementTypeData( XmlSchemaResourceTypeLoaderBase<T> typeLoader, String typeName, XmlSchemaType xsdType, XmlSchemaElement xsdElement, boolean anonymousElement, T context, XmlSchemaIndex<T> schemaIndex, XmlSchemaElementTypeData superTypeData ) {
    super( schemaIndex );
    _typeLoader = typeLoader;
    _typeName = typeName;
    _xsdType = xsdType;
    _xsdElement = xsdElement;
    _anonymousElement = anonymousElement;
    _context = context;
    _superTypeData = superTypeData;
  }

  public XmlSchemaResourceTypeLoaderBase getTypeProvider() {
    return _typeLoader;
  }

  public XmlSchemaElement getXsdElement() {
    return _xsdElement;
  }

  public XmlSchemaType getXsdType() {
    if ( _xsdType == null ) {
      _xsdType = XmlSchemaIndex.getSchemaTypeForElement( _xsdElement );
    }
    return _xsdType;
  }

  @Override
  public boolean prefixSuperProperties() {
    return getSuperType().equals( TypeSystem.get( XmlElement.class ) );
  }

  @Override
  public long getFingerprint() {
    return getSchemaIndex().getFingerprint();
  }


  public List<IPropertyInfo> getDeclaredProperties() {
    //    if ( _prefixSuperProperties ) { // add $ to super properties
    List<IPropertyInfo> props = new ArrayList<IPropertyInfo>();
    final IType delegate = XmlSchemaIndex.getGosuTypeBySchemaObject( getXsdType() );
    for ( IPropertyInfo prop : delegate.getTypeInfo().getProperties() ) {
      if ( XmlSchemaIndex.getSchemaIndexByType( prop.getOwnersType() ) == null ) {
        continue; // only copy properties generated by us - not the ones from Object, etc
      }
      if ( prop.getName().equals( QNAME_PROPERTY_NAME ) ) {
        continue; // we add our own
      }
      props.add( new PropertyInfoBuilder().like( prop ).build( this ) );
    }
    // add static typing to "TypeInstance" property
    IPropertyInfo typeInstanceProperty = TypeSystem.get( XmlElement.class ).getTypeInfo().getProperty( "TypeInstance" );
    props.add( new PropertyInfoBuilder()
            .like( typeInstanceProperty )
            .withLocation( _xsdType.getLocationInfo() )
            .withName( TYPEINSTANCE_PROPERTY_NAME )
            .withType( getXmlTypeInstanceTypeData().getType() )
            .build( this ) 
    );

    props.add( new PropertyInfoBuilder()
            .withLocation( getLocationInfo() )
            .withName( QNAME_PROPERTY_NAME ) 
            .withType( JavaTypes.QNAME() )
            .withDescription( "The QName of this element" )
            .withWritable( false )
            .withStatic()
            .withAccessor( new IPropertyAccessor() {
                @Override
                public Object getValue( Object ctx ) {
                  QName qname = getXsdElement().getQName();
                  if ( qname == null ) {
                    qname = getXsdElement().getRefName();
                  }
                  return qname;
                }

                @Override
                public void setValue( Object ctx, Object value ) {
                  throw new UnsupportedOperationException();
                }
              }
            )
            .build( this ) );
    props.addAll( _typeLoader.getAdditionalProperties( this ) );
    return props;
  }

  public List<IMethodInfo> getDeclaredMethods() {
    List<IMethodInfo> methods = new ArrayList<IMethodInfo>();

    // parse(String)
    IMethodCallHandler callHandler = new IMethodCallHandler() {
      public Object handleCall( Object ctx, Object... args ) {
        final String string = (String) args[0];
        if ( string.length() > 0 && string.indexOf( '<' ) < 0 ) {
          throw new RuntimeException( "Please use " + _typeName + ".parse( java.io.File ) to parse a file" );
        }
        return XmlElementInternals.instance().parse( getType(), new StringReader( string ), "string", true, null, args.length > 1 ? (XmlParseOptions) args[1] : null );
      }
    };
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters( new ParameterInfoBuilder()
                    .withName( "xmlString" )
                    .withDescription( "The string to parse" )
                    .withType( TypeSystem.get( String.class ) )
            ).build( this ) );
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters(
                    new ParameterInfoBuilder()
                    .withName( "xmlString" )
                    .withDescription( "The string to parse" )
                    .withType( TypeSystem.get( String.class ) ),
                    new ParameterInfoBuilder()
                    .withName( "options" )
                    .withDescription( "XML parse options" )
                    .withType( TypeSystem.get( XmlParseOptions.class ) )
            ).build( this ) );

    // parse(File)
    callHandler = new IMethodCallHandler() {
      public Object handleCall( Object ctx, Object... args ) {
        try {
          File file = (File) args[0];
          FileInputStream stream = new FileInputStream( file );
          try {
            return XmlElementInternals.instance().parse( getType(), stream, file.getCanonicalPath(), true, null, args.length > 1 ? (XmlParseOptions) args[1] : null, file.toURI().toURL().toExternalForm() );
          }
          finally {
            try {
              stream.close();
            }
            catch ( Exception ex ) {
              // ignore
            }
          }
        }
        catch ( Exception ex ) {
          throw GosuExceptionUtil.forceThrow( ex );
        }
      }
    };
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters( new ParameterInfoBuilder()
                    .withName( "file" )
                    .withDescription( "The file to parse" )
                    .withType( TypeSystem.get( File.class ) )
            ).build( this ) );
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters(
                    new ParameterInfoBuilder()
                    .withName( "file" )
                    .withDescription( "The file to parse" )
                    .withType( TypeSystem.get( File.class ) ),
                    new ParameterInfoBuilder()
                    .withName( "options" )
                    .withDescription( "XML parse options" )
                    .withType( TypeSystem.get( XmlParseOptions.class ) )
            ).build( this ) );

    // parse(URL)
    callHandler = new IMethodCallHandler() {
      public Object handleCall( Object ctx, Object... args ) {
        try {
          URL url = (URL) args[0];
          //      try {
//        return CommonServices.getFileSystem().getIFile(new File(url.toURI()));
//      } catch (URISyntaxException e) {
//        throw new RuntimeException(e);
//      }
          InputStream stream = CommonServices.getFileSystem().getIFile(url).openInputStream();
          try {
            return XmlElementInternals.instance().parse( getType(), stream, url.toExternalForm(), true, null, args.length > 1 ? (XmlParseOptions) args[1] : null, url.toExternalForm() );
          }
          finally {
            try {
              stream.close();
            }
            catch ( Exception ex ) {
              // ignore
            }
          }
        }
        catch ( Exception ex ) {
          throw GosuExceptionUtil.forceThrow( ex );
        }
      }
    };
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters( new ParameterInfoBuilder()
                    .withName( "url" )
                    .withDescription( "The URL to parse" )
                    .withType( TypeSystem.get( URL.class ) )
            ).build( this ) );
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters(
                    new ParameterInfoBuilder()
                    .withName( "url" )
                    .withDescription( "The URL to parse" )
                    .withType( TypeSystem.get( URL.class ) ),
                    new ParameterInfoBuilder()
                    .withName( "options" )
                    .withDescription( "XML parse options" )
                    .withType( TypeSystem.get( XmlParseOptions.class ) )
            ).build( this ) );

    // parse(InputStream)
    callHandler = new IMethodCallHandler() {
      public Object handleCall( Object ctx, Object... args ) {
        InputStream stream = (InputStream) args[0];
        try {
          return XmlElementInternals.instance().parse( getType(), stream, "input stream", true, null, args.length > 1 ? (XmlParseOptions) args[1] : null, null );
        }
        finally {
          try {
            stream.close();
          }
          catch ( Exception ex ) {
            // ignore
          }
        }
      }
    };
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters( new ParameterInfoBuilder()
                    .withName( "inputStream" )
                    .withDescription( "The input stream to parse" )
                    .withType( TypeSystem.get( InputStream.class ) )
            ).build( this ) );
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters(
                    new ParameterInfoBuilder()
                    .withName( "inputStream" )
                    .withDescription( "The input stream to parse" )
                    .withType( TypeSystem.get( InputStream.class ) ),
                    new ParameterInfoBuilder()
                    .withName( "options" )
                    .withDescription( "XML parse options" )
                    .withType( TypeSystem.get( XmlParseOptions.class ) )
            ).build( this ) );

    // parse(Reader)
    callHandler = new IMethodCallHandler() {
      public Object handleCall( Object ctx, Object... args ) {
        Reader reader = (Reader) args[0];
        try {
          return XmlElementInternals.instance().parse( getType(), reader, "input reader", true, null, args.length > 1 ? (XmlParseOptions) args[1] : null );
        }
        finally {
          try {
            reader.close();
          }
          catch ( Exception ex ) {
            // ignore
          }
        }
      }
    };
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters( new ParameterInfoBuilder()
                    .withName( "reader" )
                    .withDescription( "The reader to parse" )
                    .withType( TypeSystem.get( Reader.class ) )
            ).build( this ) );
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters(
                    new ParameterInfoBuilder()
                    .withName( "reader" )
                    .withDescription( "The reader to parse" )
                    .withType( TypeSystem.get( Reader.class ) ),
                    new ParameterInfoBuilder()
                    .withName( "options" )
                    .withDescription( "XML parse options" )
                    .withType( TypeSystem.get( XmlParseOptions.class ) )
            ).build( this ) );

    // parse(byte[])
    callHandler = new IMethodCallHandler() {
      public Object handleCall( Object ctx, Object... args ) {
        return XmlElementInternals.instance().parse( getType(), new ByteArrayInputStream( (byte[]) args[0] ), "byte array", true, null, args.length > 1 ? (XmlParseOptions) args[1] : null, null );
      }
    };
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters( new ParameterInfoBuilder()
                    .withName( "byteArray" )
                    .withDescription( "The byte array to parse" )
                    .withType( TypeSystem.get( byte[].class ) )
            ).build( this ) );
    methods.add( makeParseMethod()
            .withCallHandler( callHandler )
            .withParameters(
                    new ParameterInfoBuilder()
                    .withName( "byteArray" )
                    .withDescription( "The byte array to parse" )
                    .withType( TypeSystem.get( byte[].class ) ),
                    new ParameterInfoBuilder()
                    .withName( "options" )
                    .withDescription( "XML parse options" )
                    .withType( TypeSystem.get( XmlParseOptions.class ) )
            ).build( this ) );

    methods.addAll( _typeLoader.getAdditionalMethods( this ) );
    return methods;
  }

  private MethodInfoBuilder makeParseMethod() {
    return new MethodInfoBuilder().withStatic().withName("parse").withReturnType( getType() );
  }

  public List<IConstructorInfo> getDeclaredConstructors() {
    final XmlSchemaTypeInstanceTypeData xmlTypeInstanceTypeData = getXmlTypeInstanceTypeData();
    final IType xmlTypeInstanceType = xmlTypeInstanceTypeData.getType();
    final IConstructorHandler constructorHandler;
    final Class clazz;
    final Class typeInstanceClass;
    try {
      IJavaClassInfo generatedClass = getSchemaIndex().getGeneratedClass( getType().getName() );
      clazz = generatedClass == null ? null : Class.forName( generatedClass.getName());
      IJavaClassInfo generatedClassTypeInst = XmlSchemaIndex.getSchemaIndexByType( xmlTypeInstanceType ).getGeneratedClass( xmlTypeInstanceType.getName() );
      typeInstanceClass = generatedClassTypeInst == null ? null : Class.forName( generatedClassTypeInst.getName() );
    } catch (ClassNotFoundException e) {
      throw new RuntimeException( e );
    }
    if ( clazz != null && typeInstanceClass == null ) {
      throw new RuntimeException( "Partial codegen schema graph detected\n" +
                                  "Missing Generated Class: " + xmlTypeInstanceType.getName() );
    }
    if ( clazz != null ) {
      constructorHandler = new IConstructorHandler() {
        @Override
        public Object newInstance( Object... args ) {
          try {
            if ( args.length > 0 ) {
              return clazz.getConstructor( typeInstanceClass ).newInstance( args[0] );
            }
            else {
              return clazz.newInstance();
            }
          }
          catch ( Exception ex ) {
            throw new RuntimeException( ex );
          }
        }
      };
    }
    else {
      constructorHandler = new IConstructorHandler() {
        @Override
        public Object newInstance( Object... args ) {
          XmlTypeInstance xmlTypeInstance = null;
          if ( args.length > 0 ) {
            xmlTypeInstance = (XmlTypeInstance) args[0];
          }
          if ( xmlTypeInstance == null ) {
            IJavaClassInfo clazz = xmlTypeInstanceTypeData.getSchemaObject().getSchemaIndex().getGeneratedClass( xmlTypeInstanceType.getName() );
            if ( clazz != null ) {
              try {
                xmlTypeInstance = (XmlTypeInstance) clazz.newInstance();
              }
              catch ( Exception ex ) {
                throw GosuExceptionUtil.forceThrow( ex );
              }
            }
            else {
              xmlTypeInstance = XmlTypeInstanceInternals.instance().create( xmlTypeInstanceType, xmlTypeInstanceTypeData.getSchemaInfo(), EMPTY_OBJECT_ARRAY );
            }
          }
          return XmlElementInternals.instance().create( getXsdElement().getQName(), getType(), xmlTypeInstanceType, xmlTypeInstance );
        }
      };
    }
    List<IConstructorInfo> constructors = new ArrayList<IConstructorInfo>();
    constructors.add( new ConstructorInfoBuilder().withConstructorHandler( constructorHandler ).build( this ) );
    constructors.add( new ConstructorInfoBuilder().withConstructorHandler( constructorHandler ).withParameters( new ParameterInfoBuilder().withName( "typeInstance" ).withDescription( "The backing type instance for this element" ).withType( xmlTypeInstanceType ) ).build( this ) );
    constructors.addAll( _typeLoader.getAdditionalConstructors( this ) );
    return constructors;
  }

  private IJavaClassConstructor getDeclaredConstructor(IJavaClassInfo type, IJavaClassInfo paramType) {
    for (IJavaClassConstructor c : type.getDeclaredConstructors()) {
      if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0].equals(paramType)) {
        return c;
      }
    }
    return null;
  }  

  @Override
  public boolean isFinal() {
    return true;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public IType getSuperType() {
    return _superType.get();
  }

  @Override
  public List<Class<?>> getAdditionalInterfaces() {
    //noinspection unchecked
    return Arrays.asList( IXmlSchemaElementTypeData.class, ILocationAwareFeature.class );
  }

  public XmlSchemaTypeSchemaInfo getSchemaInfo() {
    XmlSchemaTypeInstanceTypeData typeInstanceTypeData = getXmlTypeInstanceTypeData();
    return typeInstanceTypeData.getSchemaInfo();
  }

  public XmlSchemaTypeInstanceTypeData getXmlTypeInstanceTypeData() {
    return (XmlSchemaTypeInstanceTypeData) XmlSchemaIndex.getGosuTypeDataBySchemaObject( getXsdType() );
  }

  @Override
  public String getName() {
    return _typeName;
  }

  @Override
  public XmlSchemaObject getSchemaObject() {
    return _xsdElement;
  }

  @Override
  public boolean isAnonymous() {
    return _anonymousElement;
  }

  public T getContext() {
    return _context;
  }

  @Override
  public void maybeInit() {
    // nothing to do
  }

    @Override
    public Class getBackingClass() {
      IJavaClassInfo clazz = getSchemaIndex().getGeneratedClass( getType().getName() );
      return (clazz != null && !(clazz instanceof IAsmJavaClassInfo)) ? clazz.getBackingClass() : XmlElement.class;
    }

  @Override
  public IJavaClassInfo getBackingClassInfo() {
    IJavaClassInfo clazz = getSchemaIndex().getGeneratedClass( getType().getName() );
    return clazz != null ? clazz : JavaTypes.getSystemType(XmlElement.class).getBackingClassInfo();
  }

  @Override
  public LocationInfo getLocationInfo() {
    return _xsdElement.getLocationInfo();
  }
}
