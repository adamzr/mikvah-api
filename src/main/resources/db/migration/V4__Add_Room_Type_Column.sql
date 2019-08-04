ALTER TABLE appointment_slot
ADD room_type varchar(255);

UPDATE appointment_slot SET room_type = 'SHOWER' WHERE room_type IS NULL;

ALTER TABLE appointment_slot
ALTER COLUMN room_type SET NOT NULL;
