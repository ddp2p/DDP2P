package net.ddp2p.ASN1;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@Documented
 @Retention(RetentionPolicy.RUNTIME)
 @Target({ElementType.TYPE})
public @interface ASN1Type {
	public Encoder.CLASS _CLASS() default Encoder.CLASS.UNIVERSAL;
	public Encoder.PC _PC() default Encoder.PC.PRIMITIVE;
	public int _class() default Encoder.CLASS_UNIVERSAL;
	public int _pc() default Encoder.PC_PRIMITIVE;
	public int _tag() default -1;
	public String _stag() default "";
}
