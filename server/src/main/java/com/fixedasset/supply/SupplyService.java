package com.fixedasset.supply;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixedasset.asset.AssetService;
import com.fixedasset.asset.entity.Asset;
import com.fixedasset.asset.entity.AssetCategory;
import com.fixedasset.asset.mapper.AssetCategoryMapper;
import com.fixedasset.common.aop.OperationLog;
import com.fixedasset.common.exception.BusinessException;
import com.fixedasset.common.model.PageResult;
import com.fixedasset.organization.entity.Department;
import com.fixedasset.organization.mapper.DepartmentMapper;
import com.fixedasset.security.SecurityUtils;
import com.fixedasset.supply.dto.SupplyRequests.InboundItemRequest;
import com.fixedasset.supply.dto.SupplyRequests.InboundOrderRequest;
import com.fixedasset.supply.dto.SupplyRequests.PurchaseItemRequest;
import com.fixedasset.supply.dto.SupplyRequests.PurchaseOrderRequest;
import com.fixedasset.supply.entity.InboundOrder;
import com.fixedasset.supply.entity.InboundOrderItem;
import com.fixedasset.supply.entity.PurchaseOrder;
import com.fixedasset.supply.entity.PurchaseOrderItem;
import com.fixedasset.supply.entity.Supplier;
import com.fixedasset.supply.mapper.InboundOrderItemMapper;
import com.fixedasset.supply.mapper.InboundOrderMapper;
import com.fixedasset.supply.mapper.PurchaseOrderItemMapper;
import com.fixedasset.supply.mapper.PurchaseOrderMapper;
import com.fixedasset.supply.mapper.SupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplyService {

    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseItemMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundItemMapper;
    private final DepartmentMapper departmentMapper;
    private final AssetCategoryMapper categoryMapper;
    private final AssetService assetService;

    public SupplyService(
            SupplierMapper supplierMapper,
            PurchaseOrderMapper purchaseOrderMapper,
            PurchaseOrderItemMapper purchaseItemMapper,
            InboundOrderMapper inboundOrderMapper,
            InboundOrderItemMapper inboundItemMapper,
            DepartmentMapper departmentMapper,
            AssetCategoryMapper categoryMapper,
            AssetService assetService
    ) {
        this.supplierMapper = supplierMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseItemMapper = purchaseItemMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundItemMapper = inboundItemMapper;
        this.departmentMapper = departmentMapper;
        this.categoryMapper = categoryMapper;
        this.assetService = assetService;
    }

    public List<Supplier> suppliers(String keyword) {
        return supplierMapper.selectList(Wrappers.<Supplier>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(Supplier::getName, keyword)
                        .or()
                        .like(Supplier::getCode, keyword))
                .orderByAsc(Supplier::getId));
    }

    @OperationLog(module = "供应商", action = "新增供应商")
    public Supplier createSupplier(Supplier supplier) {
        requireUniqueSupplierCode(supplier.getCode(), null);
        supplier.setId(null);
        supplier.setStatus(supplier.getStatus() == null ? "ACTIVE" : supplier.getStatus());
        supplier.setCreatedAt(LocalDateTime.now());
        supplier.setUpdatedAt(LocalDateTime.now());
        supplierMapper.insert(supplier);
        return supplier;
    }

    @OperationLog(module = "供应商", action = "编辑供应商")
    public Supplier updateSupplier(Long id, Supplier payload) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException(404, "供应商不存在");
        }
        requireUniqueSupplierCode(payload.getCode(), id);
        supplier.setName(payload.getName());
        supplier.setCode(payload.getCode());
        supplier.setContactName(payload.getContactName());
        supplier.setPhone(payload.getPhone());
        supplier.setEmail(payload.getEmail());
        supplier.setAddress(payload.getAddress());
        supplier.setStatus(payload.getStatus());
        supplier.setUpdatedAt(LocalDateTime.now());
        supplierMapper.updateById(supplier);
        return supplier;
    }

    public PageResult<PurchaseOrder> purchasePage(long page, long size, String status, String keyword) {
        Page<PurchaseOrder> result = purchaseOrderMapper.selectPage(new Page<>(page, size),
                Wrappers.<PurchaseOrder>lambdaQuery()
                        .eq(status != null && !status.isBlank(), PurchaseOrder::getStatus, status)
                        .like(keyword != null && !keyword.isBlank(), PurchaseOrder::getOrderNo, keyword)
                        .orderByDesc(PurchaseOrder::getOrderDate));
        return PageResult.from(result);
    }

    public Map<String, Object> purchaseDetail(Long id) {
        PurchaseOrder order = requirePurchase(id);
        List<PurchaseOrderItem> items = purchaseItemMapper.selectList(
                Wrappers.<PurchaseOrderItem>lambdaQuery()
                        .eq(PurchaseOrderItem::getPurchaseOrderId, id));
        return Map.of("order", order, "items", items);
    }

    @Transactional
    @OperationLog(module = "采购管理", action = "创建采购单")
    public PurchaseOrder createPurchase(PurchaseOrderRequest request) {
        requireSupplier(request.supplierId());
        requireUniqueOrderNo(request.orderNo(), null);
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(request.orderNo());
        order.setSupplierId(request.supplierId());
        order.setApplicantId(request.applicantId());
        order.setOrderDate(request.orderDate());
        order.setExpectedDate(request.expectedDate());
        order.setStatus("DRAFT");
        order.setRemark(request.remark());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setTotalAmount(calculateTotal(request.items()));
        purchaseOrderMapper.insert(order);
        replacePurchaseItems(order.getId(), request.items());
        return order;
    }

    @Transactional
    @OperationLog(module = "采购管理", action = "编辑采购单")
    public PurchaseOrder updatePurchase(Long id, PurchaseOrderRequest request) {
        PurchaseOrder order = requirePurchase(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只有草稿采购单可以编辑");
        }
        requireSupplier(request.supplierId());
        requireUniqueOrderNo(request.orderNo(), id);
        order.setOrderNo(request.orderNo());
        order.setSupplierId(request.supplierId());
        order.setApplicantId(request.applicantId());
        order.setOrderDate(request.orderDate());
        order.setExpectedDate(request.expectedDate());
        order.setRemark(request.remark());
        order.setTotalAmount(calculateTotal(request.items()));
        order.setUpdatedAt(LocalDateTime.now());
        purchaseOrderMapper.updateById(order);
        purchaseItemMapper.delete(Wrappers.<PurchaseOrderItem>lambdaQuery()
                .eq(PurchaseOrderItem::getPurchaseOrderId, id));
        replacePurchaseItems(id, request.items());
        return order;
    }

    @OperationLog(module = "采购管理", action = "提交采购单")
    public PurchaseOrder submitPurchase(Long id) {
        PurchaseOrder order = requirePurchase(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只有草稿采购单可以提交");
        }
        order.setStatus("PENDING");
        order.setUpdatedAt(LocalDateTime.now());
        purchaseOrderMapper.updateById(order);
        return order;
    }

    @OperationLog(module = "采购管理", action = "审核采购单")
    public PurchaseOrder approvePurchase(Long id) {
        PurchaseOrder order = requirePurchase(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("采购单当前不可审核");
        }
        order.setStatus("APPROVED");
        order.setApprovedBy(SecurityUtils.currentUserId());
        order.setApprovedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        purchaseOrderMapper.updateById(order);
        return order;
    }

    @OperationLog(module = "采购管理", action = "驳回采购单")
    public PurchaseOrder rejectPurchase(Long id, String reason) {
        PurchaseOrder order = requirePurchase(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("采购单当前不可驳回");
        }
        order.setStatus("REJECTED");
        order.setRemark(reason);
        order.setUpdatedAt(LocalDateTime.now());
        purchaseOrderMapper.updateById(order);
        return order;
    }

    public PageResult<InboundOrder> inboundPage(long page, long size, String status) {
        Page<InboundOrder> result = inboundOrderMapper.selectPage(new Page<>(page, size),
                Wrappers.<InboundOrder>lambdaQuery()
                        .eq(status != null && !status.isBlank(), InboundOrder::getStatus, status)
                        .orderByDesc(InboundOrder::getInboundDate));
        return PageResult.from(result);
    }

    public Map<String, Object> inboundDetail(Long id) {
        InboundOrder order = requireInbound(id);
        List<InboundOrderItem> items = inboundItemMapper.selectList(
                Wrappers.<InboundOrderItem>lambdaQuery()
                        .eq(InboundOrderItem::getInboundOrderId, id));
        return Map.of("order", order, "items", items);
    }

    @Transactional
    @OperationLog(module = "入库管理", action = "创建入库单")
    public InboundOrder createInbound(InboundOrderRequest request) {
        requireSupplier(request.supplierId());
        if (request.purchaseOrderId() != null) {
            PurchaseOrder purchase = requirePurchase(request.purchaseOrderId());
            if (!"APPROVED".equals(purchase.getStatus())) {
                throw new BusinessException("关联采购单尚未审核通过");
            }
        }
        requireUniqueInboundNo(request.inboundNo(), null);
        InboundOrder order = new InboundOrder();
        order.setInboundNo(request.inboundNo());
        order.setPurchaseOrderId(request.purchaseOrderId());
        order.setSupplierId(request.supplierId());
        order.setWarehouseName(request.warehouseName());
        order.setInboundDate(request.inboundDate());
        order.setOperatorId(request.operatorId());
        order.setRemark(request.remark());
        order.setStatus("DRAFT");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        inboundOrderMapper.insert(order);
        replaceInboundItems(order.getId(), request.items());
        return order;
    }

    @Transactional
    @OperationLog(module = "入库管理", action = "确认入库")
    public Map<String, Object> confirmInbound(Long id) {
        InboundOrder order = requireInbound(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只有草稿入库单可以确认");
        }
        List<InboundOrderItem> items = inboundItemMapper.selectList(
                Wrappers.<InboundOrderItem>lambdaQuery()
                        .eq(InboundOrderItem::getInboundOrderId, id));
        if (items.isEmpty()) {
            throw new BusinessException("入库单没有明细");
        }
        int created = 0;
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            InboundOrderItem item = items.get(itemIndex);
            for (int index = 1; index <= item.getQuantity(); index++) {
                Asset asset = new Asset();
                asset.setAssetNo(order.getInboundNo() + "-" + (itemIndex + 1) + "-" + index);
                asset.setName(item.getAssetName());
                asset.setCategoryId(item.getCategoryId());
                asset.setBrandModel(item.getBrandModel());
                asset.setPurchaseDate(order.getInboundDate());
                asset.setOriginalValue(item.getUnitPrice());
                asset.setUsefulLife(defaultUsefulLife(item.getCategoryId()));
                asset.setDepartmentId(item.getDepartmentId());
                asset.setStatus("IDLE");
                asset.setRemark("由入库单 " + order.getInboundNo() + " 生成");
                assetService.create(asset);
                created++;
            }
        }
        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        inboundOrderMapper.updateById(order);
        if (order.getPurchaseOrderId() != null) {
            PurchaseOrder purchase = requirePurchase(order.getPurchaseOrderId());
            purchase.setStatus("COMPLETED");
            purchase.setUpdatedAt(LocalDateTime.now());
            purchaseOrderMapper.updateById(purchase);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("createdAssets", created);
        return result;
    }

    @OperationLog(module = "入库管理", action = "取消入库单")
    public InboundOrder cancelInbound(Long id) {
        InboundOrder order = requireInbound(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException("只有草稿入库单可以取消");
        }
        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        inboundOrderMapper.updateById(order);
        return order;
    }

    private void replacePurchaseItems(Long orderId, List<PurchaseItemRequest> items) {
        for (PurchaseItemRequest request : items) {
            if (request.quantity() <= 0 || request.unitPrice().signum() < 0) {
                throw new BusinessException("采购数量和单价必须合法");
            }
            requireCategory(request.categoryId());
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrderId(orderId);
            item.setAssetName(request.assetName());
            item.setCategoryId(request.categoryId());
            item.setQuantity(request.quantity());
            item.setUnitPrice(request.unitPrice());
            item.setAmount(request.unitPrice().multiply(BigDecimal.valueOf(request.quantity())));
            item.setRemark(request.remark());
            purchaseItemMapper.insert(item);
        }
    }

    private void replaceInboundItems(Long orderId, List<InboundItemRequest> items) {
        for (InboundItemRequest request : items) {
            if (request.quantity() <= 0 || request.unitPrice().signum() < 0) {
                throw new BusinessException("入库数量和单价必须合法");
            }
            requireCategory(request.categoryId());
            Department department = departmentMapper.selectById(request.departmentId());
            if (department == null) {
                throw new BusinessException("入库资产必须指定部门");
            }
            InboundOrderItem item = new InboundOrderItem();
            item.setInboundOrderId(orderId);
            item.setAssetName(request.assetName());
            item.setCategoryId(request.categoryId());
            item.setBrandModel(request.brandModel());
            item.setQuantity(request.quantity());
            item.setUnitPrice(request.unitPrice());
            item.setDepartmentId(request.departmentId());
            item.setRemark(request.remark());
            inboundItemMapper.insert(item);
        }
    }

    private BigDecimal calculateTotal(List<PurchaseItemRequest> items) {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int defaultUsefulLife(Long categoryId) {
        AssetCategory category = requireCategory(categoryId);
        return category.getParentId() == null ? 5 : 5;
    }

    private Supplier requireSupplier(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException("供应商不存在");
        }
        return supplier;
    }

    private AssetCategory requireCategory(Long id) {
        AssetCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("资产分类不存在");
        }
        return category;
    }

    private PurchaseOrder requirePurchase(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "采购单不存在");
        }
        return order;
    }

    private InboundOrder requireInbound(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "入库单不存在");
        }
        return order;
    }

    private void requireUniqueSupplierCode(String code, Long excludeId) {
        long count = supplierMapper.selectCount(Wrappers.<Supplier>lambdaQuery()
                .eq(Supplier::getCode, code)
                .ne(excludeId != null, Supplier::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("供应商编码已存在");
        }
    }

    private void requireUniqueOrderNo(String orderNo, Long excludeId) {
        long count = purchaseOrderMapper.selectCount(Wrappers.<PurchaseOrder>lambdaQuery()
                .eq(PurchaseOrder::getOrderNo, orderNo)
                .ne(excludeId != null, PurchaseOrder::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("采购单号已存在");
        }
    }

    private void requireUniqueInboundNo(String inboundNo, Long excludeId) {
        long count = inboundOrderMapper.selectCount(Wrappers.<InboundOrder>lambdaQuery()
                .eq(InboundOrder::getInboundNo, inboundNo)
                .ne(excludeId != null, InboundOrder::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("入库单号已存在");
        }
    }
}
