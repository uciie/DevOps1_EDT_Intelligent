package com.example.backend.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour GoogleApiException.
 * 
 * Teste la création d'exceptions avec différents paramètres et
 * la récupération des propriétés (errorCode, retryable).
 */
@DisplayName("GoogleApiException - Tests unitaires")
class GoogleApiExceptionTest {

    @Nested
    @DisplayName("Constructeur avec message, errorCode et retryable")
    class ConstructorWithMessageTests {

        @Test
        @DisplayName("✅ Devrait créer une exception avec tous les paramètres valides")
        void shouldCreateException_WithAllValidParameters() {
            // Given
            String message = "Service temporairement indisponible";
            String errorCode = "SERVICE_UNAVAILABLE";
            boolean retryable = true;

            // When
            GoogleApiException exception = new GoogleApiException(message, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("✅ Devrait créer une exception non-retryable")
        void shouldCreateNonRetryableException() {
            // Given
            String message = "Token expiré";
            String errorCode = "UNAUTHORIZED";
            boolean retryable = false;

            // When
            GoogleApiException exception = new GoogleApiException(message, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("✅ Devrait gérer un message null")
        void shouldHandleNullMessage() {
            // Given
            String message = null;
            String errorCode = "UNKNOWN_ERROR";
            boolean retryable = false;

            // When
            GoogleApiException exception = new GoogleApiException(message, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isNull();
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }

        @Test
        @DisplayName("✅ Devrait gérer un errorCode null")
        void shouldHandleNullErrorCode() {
            // Given
            String message = "Erreur inconnue";
            String errorCode = null;
            boolean retryable = true;

            // When
            GoogleApiException exception = new GoogleApiException(message, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("✅ Devrait créer une exception avec un message vide")
        void shouldCreateExceptionWithEmptyMessage() {
            // Given
            String message = "";
            String errorCode = "EMPTY_MESSAGE";
            boolean retryable = true;

            // When
            GoogleApiException exception = new GoogleApiException(message, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEmpty();
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }
    }

    @Nested
    @DisplayName("Constructeur avec message, cause, errorCode et retryable")
    class ConstructorWithCauseTests {

        @Test
        @DisplayName("✅ Devrait créer une exception avec une cause")
        void shouldCreateException_WithCause() {
            // Given
            String message = "Erreur de communication";
            Throwable cause = new RuntimeException("Connexion perdue");
            String errorCode = "NETWORK_ERROR";
            boolean retryable = true;

            // When
            GoogleApiException exception = new GoogleApiException(message, cause, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("✅ Devrait créer une exception avec IOException comme cause")
        void shouldCreateException_WithIOExceptionCause() {
            // Given
            String message = "Impossible de lire la réponse";
            Throwable cause = new java.io.IOException("Connection reset");
            String errorCode = "IO_ERROR";
            boolean retryable = true;

            // When
            GoogleApiException exception = new GoogleApiException(message, cause, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isInstanceOf(java.io.IOException.class);
            assertThat(exception.getCause().getMessage()).isEqualTo("Connection reset");
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }

        @Test
        @DisplayName("✅ Devrait gérer une cause null")
        void shouldHandleNullCause() {
            // Given
            String message = "Erreur sans cause";
            Throwable cause = null;
            String errorCode = "NO_CAUSE";
            boolean retryable = false;

            // When
            GoogleApiException exception = new GoogleApiException(message, cause, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }

        @Test
        @DisplayName("✅ Devrait créer une exception chaînée avec plusieurs niveaux")
        void shouldCreateChainedException() {
            // Given
            Throwable rootCause = new IllegalStateException("État invalide");
            Throwable cause = new RuntimeException("Erreur intermédiaire", rootCause);
            String message = "Erreur finale";
            String errorCode = "CHAINED_ERROR";
            boolean retryable = false;

            // When
            GoogleApiException exception = new GoogleApiException(message, cause, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }
    }

    @Nested
    @DisplayName("Codes d'erreur spécifiques")
    class ErrorCodeTests {

        @Test
        @DisplayName("✅ Devrait créer une exception SERVICE_UNAVAILABLE retryable")
        void shouldCreateServiceUnavailableException() {
            // When
            GoogleApiException exception = new GoogleApiException(
                    "Service Google Calendar temporairement indisponible",
                    "SERVICE_UNAVAILABLE",
                    true
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("SERVICE_UNAVAILABLE");
            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("✅ Devrait créer une exception NETWORK_ERROR retryable")
        void shouldCreateNetworkErrorException() {
            // When
            GoogleApiException exception = new GoogleApiException(
                    "Problème de connexion réseau",
                    "NETWORK_ERROR",
                    true
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("NETWORK_ERROR");
            assertThat(exception.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("✅ Devrait créer une exception UNAUTHORIZED non-retryable")
        void shouldCreateUnauthorizedException() {
            // When
            GoogleApiException exception = new GoogleApiException(
                    "Token d'accès expiré",
                    "UNAUTHORIZED",
                    false
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("UNAUTHORIZED");
            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("✅ Devrait créer une exception INSUFFICIENT_PERMISSIONS non-retryable")
        void shouldCreateInsufficientPermissionsException() {
            // When
            GoogleApiException exception = new GoogleApiException(
                    "Permissions insuffisantes pour accéder au calendrier",
                    "INSUFFICIENT_PERMISSIONS",
                    false
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("INSUFFICIENT_PERMISSIONS");
            assertThat(exception.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("✅ Devrait créer une exception avec un code personnalisé")
        void shouldCreateCustomErrorCodeException() {
            // When
            GoogleApiException exception = new GoogleApiException(
                    "Erreur spécifique au métier",
                    "CUSTOM_BUSINESS_ERROR",
                    false
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo("CUSTOM_BUSINESS_ERROR");
            assertThat(exception.isRetryable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests d'héritage RuntimeException")
    class InheritanceTests {

        @Test
        @DisplayName("✅ Devrait hériter de RuntimeException")
        void shouldExtendRuntimeException() {
            // When
            GoogleApiException exception = new GoogleApiException(
                    "Test",
                    "TEST_ERROR",
                    true
            );

            // Then
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("✅ Devrait pouvoir être lancée sans try-catch (unchecked)")
        void shouldBeUnchecked() {
            // When/Then - Ne nécessite pas de try-catch
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                throw new GoogleApiException("Test", "TEST", false);
            });
        }

        @Test
        @DisplayName("✅ Devrait pouvoir être attrapée comme RuntimeException")
        void shouldBeCatchableAsRuntimeException() {
            // Given
            GoogleApiException exception = new GoogleApiException(
                    "Test error",
                    "TEST_CODE",
                    true
            );

            // When/Then
            assertThrows(RuntimeException.class, () -> {
                throw exception;
            });
        }
    }

    @Nested
    @DisplayName("Tests de cas limites")
    class EdgeCaseTests {

        @Test
        @DisplayName("✅ Devrait gérer un message très long")
        void shouldHandleVeryLongMessage() {
            // Given
            String longMessage = "A".repeat(10000);
            String errorCode = "LONG_MESSAGE";
            boolean retryable = true;

            // When
            GoogleApiException exception = new GoogleApiException(longMessage, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).hasSize(10000);
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }

        @Test
        @DisplayName("✅ Devrait gérer un errorCode très long")
        void shouldHandleVeryLongErrorCode() {
            // Given
            String message = "Error message";
            String longErrorCode = "ERROR_".repeat(1000);
            boolean retryable = false;

            // When
            GoogleApiException exception = new GoogleApiException(message, longErrorCode, retryable);

            // Then
            assertThat(exception.getErrorCode()).startsWith("ERROR_");
            assertThat(exception.getErrorCode()).hasSize("ERROR_".length() * 1000);
        }

        @Test
        @DisplayName("✅ Devrait gérer des caractères spéciaux dans le message")
        void shouldHandleSpecialCharactersInMessage() {
            // Given
            String message = "Erreur: é à ç ñ 中文 🚀 «»";
            String errorCode = "SPECIAL_CHARS";
            boolean retryable = true;

            // When
            GoogleApiException exception = new GoogleApiException(message, errorCode, retryable);

            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getMessage()).contains("é", "à", "ç", "ñ", "中文", "🚀");
        }

        @Test
        @DisplayName("✅ Devrait gérer des caractères spéciaux dans le errorCode")
        void shouldHandleSpecialCharactersInErrorCode() {
            // Given
            String message = "Test error";
            String errorCode = "ERROR-CODE_WITH.SPECIAL:CHARS";
            boolean retryable = false;

            // When
            GoogleApiException exception = new GoogleApiException(message, errorCode, retryable);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        }
    }

    @Nested
    @DisplayName("Tests de getters")
    class GetterTests {

        @Test
        @DisplayName("✅ getErrorCode() devrait retourner le code d'erreur")
        void getErrorCodeShouldReturnErrorCode() {
            // Given
            GoogleApiException exception = new GoogleApiException(
                    "Message",
                    "TEST_ERROR_CODE",
                    true
            );

            // When
            String errorCode = exception.getErrorCode();

            // Then
            assertThat(errorCode).isEqualTo("TEST_ERROR_CODE");
        }

        @Test
        @DisplayName("✅ isRetryable() devrait retourner true pour exception retryable")
        void isRetryableShouldReturnTrueForRetryableException() {
            // Given
            GoogleApiException exception = new GoogleApiException(
                    "Message",
                    "RETRYABLE_ERROR",
                    true
            );

            // When
            boolean retryable = exception.isRetryable();

            // Then
            assertThat(retryable).isTrue();
        }

        @Test
        @DisplayName("✅ isRetryable() devrait retourner false pour exception non-retryable")
        void isRetryableShouldReturnFalseForNonRetryableException() {
            // Given
            GoogleApiException exception = new GoogleApiException(
                    "Message",
                    "NON_RETRYABLE_ERROR",
                    false
            );

            // When
            boolean retryable = exception.isRetryable();

            // Then
            assertThat(retryable).isFalse();
        }
    }
}