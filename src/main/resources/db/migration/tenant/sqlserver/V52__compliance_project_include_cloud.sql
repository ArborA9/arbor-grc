-- Framework Import V2 Phase 4: cloud scope on compliance projects.
ALTER TABLE compliance_project ADD include_cloud BIT NOT NULL CONSTRAINT df_cp_include_cloud DEFAULT 0;
GO
