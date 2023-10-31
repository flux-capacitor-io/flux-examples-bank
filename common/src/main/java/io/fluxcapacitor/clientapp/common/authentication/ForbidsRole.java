package io.fluxcapacitor.clientapp.common.authentication;

import io.fluxcapacitor.javaclient.tracking.handling.authentication.ForbidsAnyRole;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE, METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ForbidsAnyRole
public @interface ForbidsRole {
    Role[] value();
}
