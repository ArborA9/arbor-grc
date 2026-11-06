-- Letter E — email delivery status detail (SMTP/ACS send errors).

ALTER TABLE notification_email_outbox ADD error_message NVARCHAR(500) NULL;
