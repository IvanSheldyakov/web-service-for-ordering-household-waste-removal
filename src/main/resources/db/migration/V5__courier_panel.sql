alter table courier
    add column postal_code varchar(16);

alter table courier
    add column timezone text;

alter table courier
    alter column postal_code set not null;

alter table courier
    alter column timezone set not null;

create index if not exists ix_courier_postal_code on courier (postal_code);
