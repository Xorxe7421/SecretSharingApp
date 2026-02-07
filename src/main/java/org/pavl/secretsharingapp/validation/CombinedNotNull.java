package org.pavl.secretsharingapp.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = CombinedNotNullValidator.class)
@Documented
public @interface CombinedNotNull {

    String message() default "{org.pavl.secretsharingapp.validation.CombinedNotNull.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    String[] value();
}
