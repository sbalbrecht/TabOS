package tabos.config

import java.lang.annotation.*

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Config {
    String value() default ""
}
