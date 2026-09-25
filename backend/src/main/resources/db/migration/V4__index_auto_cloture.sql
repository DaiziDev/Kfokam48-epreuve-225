-- EF14/RG14 : l'auto-clôture cherche chaque minute les sessions OUVERTE dont
-- expiration_at + 24h est dépassé. L'index évite un parcours complet de la
-- table à chaque passage (ENF2).
CREATE INDEX idx_session_cours_statut_expiration ON session_cours (statut, expiration_at);
