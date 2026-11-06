-- Letter E — email delivery status detail (SMTP/ACS send errors).

ALTER TABLE notification_email_outbox ADD COLUMN error_message VARCHAR(500);
