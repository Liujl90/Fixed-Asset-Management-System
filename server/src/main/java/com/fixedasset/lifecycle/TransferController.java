package com.fixedasset.lifecycle;

import com.fixedasset.common.model.ApiResponse;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.lifecycle.dto.TransferCreateRequest;
import com.fixedasset.lifecycle.entity.TransferRecord;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('transfer:read')")
    public ApiResponse<PageResult<TransferRecord>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long assetId
    ) {
        return ApiResponse.ok(transferService.page(page, size, assetId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('transfer:write')")
    public ApiResponse<TransferRecord> create(@Valid @RequestBody TransferCreateRequest request) {
        return ApiResponse.ok(transferService.create(request));
    }
}
