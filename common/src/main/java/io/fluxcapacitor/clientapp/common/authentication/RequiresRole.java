package io.fluxcapacitor.clientapp.common.authentication;

import io.fluxcapacitor.javaclient.tracking.handling.authentication.RequiresAnyRole;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target(TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@RequiresAnyRole
public @interface RequiresRole {
    Role[] value() default {};
}
