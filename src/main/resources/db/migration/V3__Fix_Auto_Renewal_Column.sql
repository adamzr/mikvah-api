ALTER TABLE membership
ALTER COLUMN auto_renew_enabled SET DEFAULT TRUE;

ALTER TABLE membership
ALTER COLUMN auto_renew_enabled SET NOT NULL;