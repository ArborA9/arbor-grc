-- BIA client comments: structured staff / assets / MOR resources. Old columns kept.

IF COL_LENGTH('bia_mor', 'min_staff_people') IS NULL
    ALTER TABLE bia_mor ADD min_staff_people NVARCHAR(MAX) NULL;
IF COL_LENGTH('bia_mor', 'recovery_strategies') IS NULL
    ALTER TABLE bia_mor ADD recovery_strategies NVARCHAR(MAX) NULL;
IF COL_LENGTH('bia_mor', 'internet_yn') IS NULL
    ALTER TABLE bia_mor ADD internet_yn NVARCHAR(8) NULL;
IF COL_LENGTH('bia_mor', 'intranet_yn') IS NULL
    ALTER TABLE bia_mor ADD intranet_yn NVARCHAR(8) NULL;
IF COL_LENGTH('bia_mor', 'vpn_yn') IS NULL
    ALTER TABLE bia_mor ADD vpn_yn NVARCHAR(8) NULL;
IF COL_LENGTH('bia_mor', 'site_type') IS NULL
    ALTER TABLE bia_mor ADD site_type NVARCHAR(32) NULL;
IF COL_LENGTH('bia_mor', 'software_apps') IS NULL
    ALTER TABLE bia_mor ADD software_apps NVARCHAR(MAX) NULL;
IF COL_LENGTH('bia_mor', 'laptops') IS NULL
    ALTER TABLE bia_mor ADD laptops INT NULL;
IF COL_LENGTH('bia_mor', 'm365_yn') IS NULL
    ALTER TABLE bia_mor ADD m365_yn NVARCHAR(8) NULL;
IF COL_LENGTH('bia_mor', 'third_party_contacts') IS NULL
    ALTER TABLE bia_mor ADD third_party_contacts NVARCHAR(MAX) NULL;
