-- Persist raw invite token only while outbound email is off, so Admin can re-open
-- the vendor portal link after Issue (OTP remains LOCAL_PREVIEW_OTP / 1111).
ALTER TABLE tprm_vendor_token ADD COLUMN token_preview VARCHAR(512);
