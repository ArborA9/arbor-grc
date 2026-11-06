-- Framework Import V2 Phase 4: cloud scope on tenant activation.
ALTER TABLE tenant_framework ADD include_cloud BIT NOT NULL CONSTRAINT df_tf_include_cloud DEFAULT 0;
GO
