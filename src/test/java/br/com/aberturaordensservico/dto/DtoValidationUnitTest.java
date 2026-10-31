package br.com.aberturaordensservico.dto;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.aberturaordensservico.model.enums.OriginChannel;
import br.com.aberturaordensservico.model.enums.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationUnitTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("OrderCancellationRequest deve falhar com menos de 10 caracteres ou mais de 300 caracteres")
    void testOrderCancellationRequestSizeValidation() {
        // Menos de 10 caracteres (9 chars)
        OrderCancellationRequest tooShort = new OrderCancellationRequest("123456789");
        Set<ConstraintViolation<OrderCancellationRequest>> violationsShort = validator.validate(tooShort);
        assertFalse(violationsShort.isEmpty());
        assertTrue(violationsShort.stream().anyMatch(v -> v.getPropertyPath().toString().equals("reason")));

        // Exatamente 10 caracteres (mínimo válido)
        OrderCancellationRequest minValid = new OrderCancellationRequest("1234567890");
        Set<ConstraintViolation<OrderCancellationRequest>> violationsMin = validator.validate(minValid);
        assertTrue(violationsMin.isEmpty());

        // Exatamente 300 caracteres (máximo válido)
        OrderCancellationRequest maxValid = new OrderCancellationRequest("a".repeat(300));
        Set<ConstraintViolation<OrderCancellationRequest>> violationsMax = validator.validate(maxValid);
        assertTrue(violationsMax.isEmpty());

        // Mais de 300 caracteres (301 chars)
        OrderCancellationRequest tooLong = new OrderCancellationRequest("a".repeat(301));
        Set<ConstraintViolation<OrderCancellationRequest>> violationsLong = validator.validate(tooLong);
        assertFalse(violationsLong.isEmpty());
        assertTrue(violationsLong.stream().anyMatch(v -> v.getPropertyPath().toString().equals("reason")));
    }

    @Test
    @DisplayName("ServiceOrderRequest deve validar limites de título (5 a 60) e descrição (10 a 500)")
    void testServiceOrderRequestSizeValidation() {
        // Título menor que 5 caracteres
        ServiceOrderRequest shortTitle = new ServiceOrderRequest(
                "1234",
                "Descrição válida com mais de 10 caracteres.",
                Priority.HIGH,
                OriginChannel.API_CLIENT,
                1L,
                null
        );
        Set<ConstraintViolation<ServiceOrderRequest>> violationsTitleShort = validator.validate(shortTitle);
        assertFalse(violationsTitleShort.isEmpty());
        assertTrue(violationsTitleShort.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));

        // Título com mais de 60 caracteres (61 chars)
        ServiceOrderRequest longTitle = new ServiceOrderRequest(
                "a".repeat(61),
                "Descrição válida com mais de 10 caracteres.",
                Priority.HIGH,
                OriginChannel.API_CLIENT,
                1L,
                null
        );
        Set<ConstraintViolation<ServiceOrderRequest>> violationsTitleLong = validator.validate(longTitle);
        assertFalse(violationsTitleLong.isEmpty());
        assertTrue(violationsTitleLong.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));

        // Descrição com menos de 10 caracteres (9 chars)
        ServiceOrderRequest shortDesc = new ServiceOrderRequest(
                "Título Válido",
                "123456789",
                Priority.HIGH,
                OriginChannel.API_CLIENT,
                1L,
                null
        );
        Set<ConstraintViolation<ServiceOrderRequest>> violationsDescShort = validator.validate(shortDesc);
        assertFalse(violationsDescShort.isEmpty());
        assertTrue(violationsDescShort.stream().anyMatch(v -> v.getPropertyPath().toString().equals("problemDescription")));

        // Descrição com mais de 500 caracteres (501 chars)
        ServiceOrderRequest longDesc = new ServiceOrderRequest(
                "Título Válido",
                "a".repeat(501),
                Priority.HIGH,
                OriginChannel.API_CLIENT,
                1L,
                null
        );
        Set<ConstraintViolation<ServiceOrderRequest>> violationsDescLong = validator.validate(longDesc);
        assertFalse(violationsDescLong.isEmpty());
        assertTrue(violationsDescLong.stream().anyMatch(v -> v.getPropertyPath().toString().equals("problemDescription")));

        // Requisição totalmente válida com limites de borda (5 chars de título, 10 chars de descrição)
        ServiceOrderRequest validMin = new ServiceOrderRequest(
                "12345",
                "1234567890",
                Priority.LOW,
                OriginChannel.API_CLIENT,
                1L,
                null
        );
        assertTrue(validator.validate(validMin).isEmpty());

        // Requisição totalmente válida com limites superiores de borda (60 chars de título, 500 chars de descrição)
        ServiceOrderRequest validMax = new ServiceOrderRequest(
                "a".repeat(60),
                "b".repeat(500),
                Priority.CRITICAL,
                OriginChannel.API_CLIENT,
                1L,
                null
        );
        assertTrue(validator.validate(validMax).isEmpty());
    }
}
