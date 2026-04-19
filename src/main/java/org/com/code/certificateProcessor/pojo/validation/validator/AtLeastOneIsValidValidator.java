package org.com.code.certificateProcessor.pojo.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.com.code.certificateProcessor.pojo.validation.AtLeastOneIsValid;

import java.lang.reflect.Field;

public class AtLeastOneIsValidValidator implements ConstraintValidator<AtLeastOneIsValid, Object> {

    private String[] fieldNames;

    @Override
    public void initialize(AtLeastOneIsValid constraintAnnotation) {
        this.fieldNames = constraintAnnotation.fieldNames();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            for (String fieldName : fieldNames) {
                Field field = object.getClass().getDeclaredField(fieldName);
                field.setAccessible(true); // 允许访问私有字段

                Object subObject = field.get(object);
                if(subObject != null){
                    if(!(subObject instanceof String) || !((String)subObject).isEmpty()){
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false; // 校验失败
    }
}
