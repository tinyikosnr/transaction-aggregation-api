/**
 * Shared presentation layer for all REST APIs: controllers, request/response DTOs and
 * global exception handling.
 *
 * <p>This package contains no business logic. It may depend only on the {@code application}
 * package of each business module — their use-case interfaces — and must never depend on a
 * module's {@code domain} or {@code persistence} packages directly. A JPA or domain entity
 * must never be returned to a client; always map to a purpose-specific response DTO.
 */
package za.co.tinyiko.transactionaggregation.api;
