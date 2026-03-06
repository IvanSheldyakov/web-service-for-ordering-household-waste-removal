create index if not exists ix_uah_user_created_at
    on user_action_history (user_id, created_at desc, id desc);
