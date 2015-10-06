package de.incentergy.decide.validation;

import static org.junit.Assert.*;

import org.junit.Test;

import de.incentergy.validation.Email;

public class EmailValidatorTest {

	@Test
	public void testValidator() {
		Email.EmailValidator emailValidator = new Email.EmailValidator();
		
		assertTrue(emailValidator.isValid("test@test.de", null));
		assertTrue(emailValidator.isValid("asdasdsad@example.com", null));
		assertTrue(emailValidator.isValid("manuel.blechschmidt@test-test.com", null));
		assertTrue(emailValidator.isValid("manuel-blechschmidt@test-test.com", null));
		
		assertFalse(emailValidator.isValid("üäösdsad@example.com", null));
		assertFalse(emailValidator.isValid("test@.com", null));
		assertFalse(emailValidator.isValid("@asdasd.com", null));
	}

}
