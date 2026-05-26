package ru.copperside.core.api;

import ru.copperside.core.application.MerchantService;
import ru.copperside.core.domain.Merchant;
import ru.copperside.core.domain.MerchantConfigSnapshotSortField;
import ru.copperside.core.domain.MerchantConfigSortField;
import ru.copperside.core.domain.MerchantSortField;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortOrder;
import ru.copperside.core.domain.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants")
@Tag(name = "Merchants", description = "Merchants and merchant configuration queries")
@Validated
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping
    @Operation(summary = "Get merchants list")
    public ApiResponse<List<MerchantResponse>> getAll(
            @Parameter(description = "Page size (>0)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @Parameter(description = "Page offset (>=0)")
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @Parameter(description = "Search in name/initiator/circuit/id")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field: mercId|name|hierarchyId|initiator|circuit")
            @RequestParam(defaultValue = "mercId") String sortBy,
            @Parameter(description = "Sort direction: asc|desc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        List<MerchantResponse> data = merchantService.getAll(
                        PageWindow.of(limit, offset),
                        SearchTerm.of(search),
                        SortOrder.of(MerchantSortField.from(sortBy), SortDirection.from(sortDir))
                ).stream()
                .map(MerchantResponse::from)
                .toList();

        return ApiResponse.success(data, new ApiMeta(limit, offset, data.size(), search, sortBy, sortDir, null));
    }

    @GetMapping("/configurations/active-line")
    @Operation(summary = "Get merchants with active configuration as JSON object")
    public ApiResponse<List<MerchantWithConfigLineResponse>> getAllWithActiveConfigurationLine(
            @Parameter(description = "Page size (>0)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @Parameter(description = "Page offset (>=0)")
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @Parameter(description = "Search in merchant fields and active config key/value")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field: mercId|name|hierarchyId|initiator|circuit")
            @RequestParam(defaultValue = "mercId") String sortBy,
            @Parameter(description = "Sort direction: asc|desc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        List<MerchantWithConfigLineResponse> data = merchantService.getAllWithActiveConfigLine(
                        PageWindow.of(limit, offset),
                        SearchTerm.of(search),
                        SortOrder.of(MerchantSortField.from(sortBy), SortDirection.from(sortDir))
                ).stream()
                .map(MerchantWithConfigLineResponse::from)
                .toList();

        return ApiResponse.success(data, new ApiMeta(limit, offset, data.size(), search, sortBy, sortDir, null));
    }

    @GetMapping("/{merchantId}")
    @Operation(summary = "Get merchant by id")
    public ApiResponse<MerchantResponse> getById(@Parameter(description = "Merchant identifier") @PathVariable Long merchantId) {
        Merchant merchant = merchantService.getById(merchantId);
        return ApiResponse.success(MerchantResponse.from(merchant));
    }

    @GetMapping("/{merchantId}/configurations")
    @Operation(summary = "Get merchant configuration active at specific time")
    public ApiResponse<List<MerchantConfigEntryResponse>> getConfiguration(
            @Parameter(description = "Merchant identifier")
            @PathVariable Long merchantId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Moment in ISO-8601 format, defaults to current UTC time")
            OffsetDateTime at,
            @Parameter(description = "Page size (>0)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @Parameter(description = "Page offset (>=0)")
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @Parameter(description = "Search in configuration key/value")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field: parameterName|parameterValue|dateBegin|dateEnd")
            @RequestParam(defaultValue = "parameterName") String sortBy,
            @Parameter(description = "Sort direction: asc|desc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        OffsetDateTime atMoment = at == null ? OffsetDateTime.now(ZoneOffset.UTC) : at.withOffsetSameInstant(ZoneOffset.UTC);
        List<MerchantConfigEntryResponse> data = merchantService.getConfigByMerchantIdAt(
                        merchantId,
                        atMoment,
                        PageWindow.of(limit, offset),
                        SearchTerm.of(search),
                        SortOrder.of(MerchantConfigSortField.from(sortBy), SortDirection.from(sortDir))
                ).stream()
                .map(MerchantConfigEntryResponse::from)
                .toList();

        return ApiResponse.success(data, new ApiMeta(limit, offset, data.size(), search, sortBy, sortDir, atMoment.toString()));
    }

    @GetMapping("/{merchantId}/configuration-history")
    @Operation(summary = "Get merchant full configuration history as snapshots")
    public ApiResponse<List<MerchantConfigSnapshotResponse>> getConfigurationHistory(
            @Parameter(description = "Merchant identifier")
            @PathVariable Long merchantId,
            @Parameter(description = "Page size (>0)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @Parameter(description = "Page offset (>=0)")
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @Parameter(description = "Search in snapshot dates and configuration key/value")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field: dateBegin|dateEnd")
            @RequestParam(defaultValue = "dateBegin") String sortBy,
            @Parameter(description = "Sort direction: asc|desc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        List<MerchantConfigSnapshotResponse> data = merchantService.getConfigSnapshotsByMerchantId(
                        merchantId,
                        PageWindow.of(limit, offset),
                        SearchTerm.of(search),
                        SortOrder.of(MerchantConfigSnapshotSortField.from(sortBy), SortDirection.from(sortDir))
                ).stream()
                .map(MerchantConfigSnapshotResponse::from)
                .toList();

        return ApiResponse.success(data, new ApiMeta(limit, offset, data.size(), search, sortBy, sortDir, null));
    }
}
