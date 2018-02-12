UPDATE membership SET auto_renew_enabled = true WHERE auto_renew_enabled IS NULL

ALTER TABLE membership
ALTER COLUMN auto_renew_enabled SET DEFAULT TRUE;

ALTER TABLE membership
ALTER COLUMN auto_renew_enabled SET NOT NULL;