ALTER TABLE member
    ADD CONSTRAINT fk_member_university FOREIGN KEY (university_id) REFERENCES university(id);
