package de.incentergy.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * This annotation can validate an email address.
 * 
 * @author Manuel Blechchmidt <manuel.blechschmidt@incentergy.de>
 *
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Email.EmailValidator.class)
public @interface Email {

	String message() default "{de.incentergy.decide.validation.email}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	public class EmailValidator implements ConstraintValidator<Email, String> {

		@Override
		public void initialize(Email arg0) {
		}

		@Override
		public boolean isValid(String arg0, ConstraintValidatorContext arg1) {
			return arg0.matches("^[a-zA-Z0-9-.]+@[a-zA-Z0-9-.]+\\.[a-zA-Z0-9]+$");
		}

	}
}