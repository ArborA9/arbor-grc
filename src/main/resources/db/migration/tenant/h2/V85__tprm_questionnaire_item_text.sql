-- TPRM questionnaires: admin-authored questions (snapshot text, optional library link).

ALTER TABLE tprm_control ALTER COLUMN control_code SET DATA TYPE VARCHAR(40);

ALTER TABLE tprm_questionnaire_item ALTER COLUMN control_id DROP NOT NULL;

ALTER TABLE tprm_questionnaire_item DROP CONSTRAINT IF EXISTS uq_tprm_qitem_questionnaire_control;

ALTER TABLE tprm_questionnaire_item ADD COLUMN IF NOT EXISTS control_code VARCHAR(40);
ALTER TABLE tprm_questionnaire_item ADD COLUMN IF NOT EXISTS domain VARCHAR(80);
ALTER TABLE tprm_questionnaire_item ADD COLUMN IF NOT EXISTS requirement VARCHAR(2000);
ALTER TABLE tprm_questionnaire_item ADD COLUMN IF NOT EXISTS required_evidence VARCHAR(2000);

UPDATE tprm_questionnaire_item i
SET control_code = (SELECT c.control_code FROM tprm_control c WHERE c.id = i.control_id),
    domain = (SELECT c.domain FROM tprm_control c WHERE c.id = i.control_id),
    requirement = (SELECT c.requirement FROM tprm_control c WHERE c.id = i.control_id),
    required_evidence = (SELECT c.required_evidence FROM tprm_control c WHERE c.id = i.control_id)
WHERE i.control_code IS NULL AND i.control_id IS NOT NULL;

ALTER TABLE tprm_assessment_item ADD COLUMN IF NOT EXISTS control_code VARCHAR(40);
ALTER TABLE tprm_assessment_item ADD COLUMN IF NOT EXISTS domain VARCHAR(80);
ALTER TABLE tprm_assessment_item ADD COLUMN IF NOT EXISTS requirement VARCHAR(2000);
ALTER TABLE tprm_assessment_item ADD COLUMN IF NOT EXISTS required_evidence VARCHAR(2000);
