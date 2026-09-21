package com.fixedasset.lifecycle;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.lifecycle.dto.LoanApplyRequest;
import com.fixedasset.lifecycle.dto.RejectRequest;
import com.fixedasset.lifecycle.entity.LoanRecord;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
/**
 * 领用归还接口：申请、审批、驳回、发起归还和确认归还。
 */
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('loan:read')")
    public ApiResponse<PageResult<LoanRecord>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assetId
    ) {
        return ApiResponse.ok(loanService.page(page, size, applicantId, status, assetId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('loan:apply')")
    public ApiResponse<LoanRecord> apply(@Valid @RequestBody LoanApplyRequest request) {
        return ApiResponse.ok(loanService.apply(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('loan:manage')")
    public ApiResponse<LoanRecord> approve(@PathVariable Long id) {
        return ApiResponse.ok(loanService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('loan:manage')")
    public ApiResponse<LoanRecord> reject(@PathVariable Long id, @RequestBody(required = false) RejectRequest request) {
        return ApiResponse.ok(loanService.reject(id, request == null ? null : request.reason()));
    }

    @PostMapping("/{id}/request-return")
    @PreAuthorize("hasAuthority('loan:return')")
    public ApiResponse<LoanRecord> requestReturn(@PathVariable Long id) {
        return ApiResponse.ok(loanService.requestReturn(id));
    }

    @PostMapping("/{id}/confirm-return")
    @PreAuthorize("hasAuthority('loan:manage')")
    public ApiResponse<LoanRecord> confirmReturn(@PathVariable Long id) {
        return ApiResponse.ok(loanService.confirmReturn(id));
    }
}
