package ru.copperside.core.api;

import ru.copperside.core.application.TerminalService;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortDirection;
import ru.copperside.core.domain.SortOrder;
import ru.copperside.core.domain.TerminalPage;
import ru.copperside.core.domain.TerminalSortField;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Terminals", description = "Acquiring terminal settings (read-only)")
@Validated
public class TerminalController {

    private final TerminalService terminalService;

    public TerminalController(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    @GetMapping("/terminals")
    @Operation(summary = "List terminal settings (password masked)")
    public ApiResponse<List<TerminalResponse>> getAll(
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @Parameter(description = "Search in mercId/mps/gate/terminalId/merchantId/mcc/name/merchant name")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field: mercId|mps|gate|terminalId|mcc")
            @RequestParam(defaultValue = "mercId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        TerminalPage page = terminalService.getAll(
                PageWindow.of(limit, offset),
                SearchTerm.of(search),
                SortOrder.of(TerminalSortField.from(sortBy), SortDirection.from(sortDir)));
        List<TerminalResponse> data = page.lines().stream().map(TerminalResponse::from).toList();
        return ApiResponse.success(data,
                new ApiMeta(limit, offset, data.size(), search, sortBy, sortDir, null, page.total(), null));
    }

    @GetMapping("/merchants/{merchantId}/terminals")
    @Operation(summary = "List a merchant's terminal settings (password masked)")
    public ApiResponse<List<TerminalResponse>> getByMerchant(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "100") @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "mercId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        TerminalPage page = terminalService.getByMerchant(
                merchantId,
                PageWindow.of(limit, offset),
                SearchTerm.of(search),
                SortOrder.of(TerminalSortField.from(sortBy), SortDirection.from(sortDir)));
        List<TerminalResponse> data = page.lines().stream().map(TerminalResponse::from).toList();
        return ApiResponse.success(data,
                new ApiMeta(limit, offset, data.size(), search, sortBy, sortDir, null, page.total(), null));
    }
}
