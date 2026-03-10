create index if not exists ix_order_info_available_grouping
    on order_info (postal_code, pickup_from, pickup_to, created_at)
    where status = 'NEW' and courier_id is null;

create index if not exists ix_order_info_assigned_grouping
    on order_info (courier_id, pickup_from, pickup_to, created_at)
    where status = 'ASSIGNED';
