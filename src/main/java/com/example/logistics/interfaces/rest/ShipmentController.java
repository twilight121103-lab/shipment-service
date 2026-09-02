package com.example.logistics.interfaces.rest;

import com.example.logistics.application.command.CancelShipmentCommand;
import com.example.logistics.application.command.ChangeStatusCommand;
import com.example.logistics.application.command.CreateShipmentCommand;
import com.example.logistics.application.query.ShipmentQuery;
import com.example.logistics.application.service.ShipmentApplicationService;
import com.example.logistics.domain.model.Shipment;
import com.example.logistics.domain.repository.PageResult;
import com.example.logistics.infrastructure.security.Roles;
import com.example.logistics.interfaces.rest.dto.CancelShipmentRequest;
import com.example.logistics.interfaces.rest.dto.CreateShipmentRequest;
import com.example.logistics.interfaces.rest.dto.PageResponse;
import com.example.logistics.interfaces.rest.dto.ShipmentResponse;
import com.example.logistics.interfaces.rest.dto.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for the shipment resource. Concerned only with HTTP binding: request
 * validation, principal extraction and DTO mapping. Business rules live in the
 * application/domain layers.
 */
@RestController
@RequestMapping("/api/v1/shipments")
@Tag(name = "Shipments", description = "Manage shipments and their lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class ShipmentController {

    private final ShipmentApplicationService applicationService;

    public ShipmentController(ShipmentApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    @PreAuthorize("hasAnyRole('LOGISTICS_USER','LOGISTICS_OPERATOR','LOGISTICS_ADMIN')")
    @Operation(summary = "Create a shipment",
            description = "Creates a shipment, assigns a tracking number, and publishes ShipmentCreated. " +
                    "Supports idempotent creation via the Idempotency-Key header.")
    public ResponseEntity<ShipmentResponse> create(
            @Valid @RequestBody CreateShipmentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            Authentication authentication) {

        final CreateShipmentCommand command = toCommand(request, subject(authentication));

        final ShipmentApplicationService.ShipmentAction action =
                applicationService.createShipment(command, idempotencyKey);

        if (action instanceof ShipmentApplicationService.ShipmentAction.Replay replay) {
            // Idempotent replay: return the original resource with fresh data.
            final Shipment shipment = applicationService.getById(UUID.fromString(replay.resourceId()));
            final URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/shipments/{id}").buildAndExpand(shipment.getId()).toUri();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(location)
                    .body(ShipmentResponse.from(shipment));
        }

        final Shipment created = ((ShipmentApplicationService.ShipmentAction.Created) action).shipment();
        final ShipmentResponse body = ShipmentResponse.from(created);
        final URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/shipments/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR','LOGISTICS_ADMIN')")
    @Operation(summary = "Get a shipment by id")
    public ShipmentResponse getById(@PathVariable UUID id) {
        return ShipmentResponse.from(applicationService.getById(id));
    }

    @GetMapping(value = "/tracking/{trackingNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR','LOGISTICS_ADMIN')")
    @Operation(summary = "Get a shipment by tracking number")
    public ShipmentResponse getByTrackingNumber(@PathVariable String trackingNumber) {
        return ShipmentResponse.from(applicationService.getByTrackingNumber(trackingNumber));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR','LOGISTICS_ADMIN')")
    @Operation(summary = "List shipments with pagination, sorting and filtering")
    public PageResponse<ShipmentResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc", required = false) String sortDirection) {

        final ShipmentQuery.SortDirection dir = ShipmentQuery.SortDirection.parse(sortDirection,
                ShipmentQuery.SortDirection.DESC);
        final ShipmentQuery query = ShipmentQuery.of(
                parseStatus(status), trackingNumber, page, size, sortBy, dir);

        final PageResult<Shipment> result = applicationService.search(query);
        final PageResponse<ShipmentResponse> response = PageResponse.of(
                ShipmentResponse.list(result.content()),
                result.page(), result.size(), result.totalElements(),
                result.totalPages(), result.hasNext());
        return response;
    }

    @PatchMapping(value = "/{id}/status", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR','LOGISTICS_ADMIN')")
    @Operation(summary = "Advance a shipment's status")
    public ShipmentResponse changeStatus(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateStatusRequest request,
                                         Authentication authentication) {
        final ChangeStatusCommand command = new ChangeStatusCommand(
                id, request.status(), subject(authentication));
        return ShipmentResponse.from(applicationService.changeStatus(command));
    }

    @PostMapping(value = "/{id}/cancel", produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    @PreAuthorize("hasAnyRole('LOGISTICS_USER','LOGISTICS_OPERATOR','LOGISTICS_ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Cancel a shipment")
    public ShipmentResponse cancel(@PathVariable UUID id,
                                   @RequestBody(required = false) @Valid CancelShipmentRequest request,
                                   Authentication authentication) {
        final String reason = request == null ? null : request.reason();
        final CancelShipmentCommand command = new CancelShipmentCommand(id, reason, subject(authentication));
        return ShipmentResponse.from(applicationService.cancel(command));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private CreateShipmentCommand toCommand(CreateShipmentRequest r, String subject) {
        return new CreateShipmentCommand(
                new com.example.logistics.domain.model.Party(
                        r.sender().name(), r.sender().phone(), r.sender().email()),
                new com.example.logistics.domain.model.Party(
                        r.recipient().name(), r.recipient().phone(), r.recipient().email()),
                new com.example.logistics.domain.model.Address(
                        r.pickupAddress().street(), r.pickupAddress().city(),
                        r.pickupAddress().postalCode(), r.pickupAddress().country()),
                new com.example.logistics.domain.model.Address(
                        r.deliveryAddress().street(), r.deliveryAddress().city(),
                        r.deliveryAddress().postalCode(), r.deliveryAddress().country()),
                new com.example.logistics.domain.model.Dimensions(
                        r.dimensions().lengthCm(), r.dimensions().widthCm(),
                        r.dimensions().heightCm(), r.dimensions().weightKg()),
                r.deliveryType(),
                r.estimatedDeliveryDate(),
                subject);
    }

    private static String subject(Authentication authentication) {
        return Optional.ofNullable(authentication).map(Authentication::getName)
                .orElse("anonymous");
    }

    private static com.example.logistics.domain.model.ShipmentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return com.example.logistics.domain.model.ShipmentStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new com.example.logistics.interfaces.exception.InvalidParameterException(
                    "Unknown shipment status: " + status);
        }
    }
}
