-- Remove unique constraint from order_num column in activity_type table
-- This allows temporary duplicate orderNum values during batch updates for reordering
-- Application-level validation will ensure uniqueness is maintained after updates

ALTER TABLE activity_type DROP INDEX IF EXISTS UK_activity_type_order_num;
ALTER TABLE activity_type DROP CONSTRAINT IF EXISTS UK_activity_type_order_num; 