package org.wimi.playground.server.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD })
/**
 *
 * @author Michael WIRTH
 *
 */
public @interface Random {
    Type value();

    enum Type {
        STRING, INT, DOUBLE, STRING_INT, STRING_DOUBLE
    }
}
