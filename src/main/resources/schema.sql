-- Drop the Hibernate-generated enum check constraint so it gets recreated
-- with the current set of TournamentStatus values on next startup.
-- Using IF EXISTS on both table and constraint makes this safe to run repeatedly.
ALTER TABLE IF EXISTS tournament DROP CONSTRAINT IF EXISTS tournament_status_check;
