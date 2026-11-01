-- 03_seed_dev_data.sql
-- DEV ONLY. Test users (passwords are BCrypt hashes). Never run this on a real environment.
-- Ask the team lead for the plain-text test passwords.

USE TicketingSystem;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = 'mona.hassan')
    INSERT INTO dbo.users (username, password_hash, role)
    VALUES ('mona.hassan', '$2a$12$vhEOf4kcSnj/FQWrjTIroOV8yTX3m9Twc7TC1ok.u7CFUjPd/L.8O', 'AGENT');

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = 'sara.nabil')
    INSERT INTO dbo.users (username, password_hash, role)
    VALUES ('sara.nabil', '$2a$12$NnGtCTganzHRgEiwds59puq2U5OMCd.rqFvQIFJInYiyVSRGx8aEC', 'REVIEWER');

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = 'ahmed.ali')
    INSERT INTO dbo.users (username, password_hash, role)
    VALUES ('ahmed.ali', '$2a$12$JaYPgn/CzRw5zPSUy7s6xerudSKAWafcbl1dA8qhIymSWFGPcDPEy', 'ADMIN');
GO

SELECT id, username, role, enabled FROM dbo.users;
GO