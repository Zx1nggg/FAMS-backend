package com.Zx1nggg.FAMS.modules.regulator.controller;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.api.Result;
import com.Zx1nggg.FAMS.modules.base.dto.QuarantineApprovalDTO;
import com.Zx1nggg.FAMS.modules.base.service.IPurchaseBatchService;
import com.Zx1nggg.FAMS.modules.base.vo.PurchaseBatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "苗种检疫审核")
@RestController
@RequestMapping("/regulator/purchase-batches")
public class QuarantineReviewController {

    private final IPurchaseBatchService purchaseBatchService;

    public QuarantineReviewController(IPurchaseBatchService purchaseBatchService) {
        this.purchaseBatchService = purchaseBatchService;
    }

    @Log(title = "苗种检疫审核", businessType = 2)
    @Operation(summary = "监管方签发采购批次检疫合格证明")
    @PutMapping("/{id}/quarantine-approval")
    public Result<PurchaseBatchVO> approve(@PathVariable Long id,
                                            @Valid @RequestBody QuarantineApprovalDTO dto) {
        PurchaseBatchVO result = purchaseBatchService.approveQuarantine(id, dto.getQuarantineCertNo());
        return result == null ? Result.error(404, "采购批次不存在") : Result.success(result);
    }
}
