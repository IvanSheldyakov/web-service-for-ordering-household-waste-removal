create index if not exists ix_order_info_cluster_status_pickup
    on order_info (postal_code, status, pickup_from);

create index if not exists ix_address_postal_code
    on address (postal_code);
