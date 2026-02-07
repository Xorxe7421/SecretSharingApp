package org.pavl.secretsharingapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CombinedNotNullValidator implements ConstraintValidator<CombinedNotNull, Object> {

    private String[] fields;

    @Override
    public void initialize(CombinedNotNull constraintAnnotation) {
        this.fields = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        List<Field> actualFields = Arrays.stream(fields)
                        .map(field -> getField(o, field))
                        .filter(Objects::nonNull)
                        .toList();

        if (actualFields.size() != fields.length) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("{There are invalid field/s declared}")
                    .addConstraintViolation();

            return false;
        }

        return actualFields.stream().anyMatch(field -> {
            try {
                return field.get(o) != null;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Field getField(Object o, String field) {
        try {
            Class<?> clazz = o.getClass();
            Field fieldObj = clazz.getDeclaredField(field);
            fieldObj.setAccessible(true);
            return fieldObj;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
