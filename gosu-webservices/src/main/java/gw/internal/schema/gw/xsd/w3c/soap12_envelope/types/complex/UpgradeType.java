package gw.internal.schema.gw.xsd.w3c.soap12_envelope.types.complex;

/***************************************************************************/
/* THIS IS AUTOGENERATED CODE - DO NOT MODIFY OR YOUR CHANGES WILL BE LOST */
/* THIS CODE CAN BE REGENERATED USING 'xsd-codegen'                        */
/***************************************************************************/
public class UpgradeType extends gw.internal.schema.gw.xsd.w3c.xmlschema.types.complex.AnyType implements gw.internal.xml.IXmlGeneratedClass {

  public static final javax.xml.namespace.QName $ELEMENT_QNAME_SupportedEnvelope = new javax.xml.namespace.QName( "http://www.w3.org/2003/05/soap-envelope", "SupportedEnvelope", "soap12" );
  public static final javax.xml.namespace.QName $QNAME = new javax.xml.namespace.QName( "http://www.w3.org/2003/05/soap-envelope", "UpgradeType", "soap12" );
  public static final gw.util.concurrent.LockingLazyVar<gw.lang.reflect.IType> TYPE = new gw.util.concurrent.LockingLazyVar<gw.lang.reflect.IType>( gw.lang.reflect.TypeSystem.getGlobalLock() ) {
          @Override
          protected gw.lang.reflect.IType init() {
            return gw.lang.reflect.TypeSystem.getByFullName( "gw.xsd.w3c.soap12_envelope.types.complex.UpgradeType" );
          }
        };
  private static final gw.util.concurrent.LockingLazyVar<java.lang.Object> SCHEMAINFO = new gw.util.concurrent.LockingLazyVar<java.lang.Object>( gw.lang.reflect.TypeSystem.getGlobalLock() ) {
          @Override
          protected java.lang.Object init() {
            gw.lang.reflect.IType type = TYPE.get();
            return getSchemaInfoByType( type );
          }
        };

  public UpgradeType() {
    super( TYPE.get(), SCHEMAINFO.get() );
  }

  protected UpgradeType( gw.lang.reflect.IType type, java.lang.Object schemaInfo ) {
    super( type, schemaInfo );
  }


  public java.util.List<gw.internal.schema.gw.xsd.w3c.soap12_envelope.anonymous.elements.UpgradeType_SupportedEnvelope> SupportedEnvelope() {
    //noinspection unchecked
    return (java.util.List<gw.internal.schema.gw.xsd.w3c.soap12_envelope.anonymous.elements.UpgradeType_SupportedEnvelope>) TYPE.get().getTypeInfo().getProperty( "SupportedEnvelope" ).getAccessor().getValue( this );
  }

  public void setSupportedEnvelope$( java.util.List<gw.internal.schema.gw.xsd.w3c.soap12_envelope.anonymous.elements.UpgradeType_SupportedEnvelope> param ) {
    TYPE.get().getTypeInfo().getProperty( "SupportedEnvelope" ).getAccessor().setValue( this, param );
  }

  @SuppressWarnings( {"UnusedDeclaration"} )
  private static final long FINGERPRINT = 6048722374270687003L;

}