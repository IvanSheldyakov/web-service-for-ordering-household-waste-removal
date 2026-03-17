alter table if exists person_info
    add column if not exists password_hash text;
