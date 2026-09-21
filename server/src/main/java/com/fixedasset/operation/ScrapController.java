package com.fixedasset.operation;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.operation.dto.OperationRequests.ScrapCompleteRequest;
import com.fixedasset.operation.dto.OperationRequests.ScrapCreateRequest;
import com.fixedasset.operation.entity.ScrapRecord;
import com.fixedasset.operation.service.ScrapService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/operations/scraps")
/**
 * 资产报废接口：申请、审核、驳回和完成处置。
 */
public class ScrapController {

    private final ScrapService scrapService;

    public ScrapController(ScrapService scrapService) {
        this.scrapService = scrapService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('scrap:read')")
    public ApiResponse<PageResult<ScrapRecord>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(scrapService.page(page, size, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('scrap:write')")
    public ApiResponse<ScrapRecord> create(@Valid @RequestBody ScrapCreateRequest request) {
        return ApiResponse.ok(scrapService.create(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('scrap:approve')")
    public ApiResponse<ScrapRecord> approve(@PathVariable Long id) {
        return ApiResponse.ok(scrapService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('scrap:approve')")
    public ApiResponse<ScrapRecord> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        return ApiResponse.ok(scrapService.reject(id, payload == null ? null : payload.get("reason")));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('scrap:write')")
    public ApiResponse<ScrapRecord> complete(
            @PathVariable Long id,
            @Valid @RequestBody ScrapCompleteRequest request
    ) {
        return ApiResponse.ok(scrapService.complete(id, request));
    }
}
