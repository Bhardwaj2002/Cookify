-- V1: baseline. Enables PostgreSQL extensions used by later migrations.
--   citext  : case-insensitive text for usernames / emails
--   pg_trgm : trigram indexes for fuzzy ingredient / title search
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
