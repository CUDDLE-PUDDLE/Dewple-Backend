ALTER TABLE activity_participant
    ADD COLUMN participant_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
