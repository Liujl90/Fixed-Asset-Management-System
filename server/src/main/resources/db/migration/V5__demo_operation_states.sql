UPDATE inventory_check
SET status = 'IN_PROGRESS'
WHERE id = 1 AND status = 'DRAFT';

INSERT INTO scrap_record (
    scrap_no, asset_id, reason, applicant_id, status, remark
)
SELECT 'SC-2026-002', 6, '闲置办公桌损坏，申请报废', 4, 'PENDING', '待审核演示数据'
WHERE NOT EXISTS (SELECT 1 FROM scrap_record WHERE scrap_no = 'SC-2026-002');
