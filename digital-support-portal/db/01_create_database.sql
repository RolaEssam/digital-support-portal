-- 01_create_database.sql
-- Creates the database and the application login/user.
-- Run as an admin (sa or Windows login), connected to any database. Safe to run twice.

USE master;
GO

IF DB_ID('TicketingSystem') IS NULL
    CREATE DATABASE TicketingSystem;
GO

-- Login used by the application (DB_USERNAME / DB_PASSWORD).
-- CHANGE the password below on your machine. Do not commit a real password.
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'portal_app')
    CREATE LOGIN portal_app WITH PASSWORD = 'ticketingsystem';
GO

USE TicketingSystem;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'portal_app')
    CREATE USER portal_app FOR LOGIN portal_app;
GO