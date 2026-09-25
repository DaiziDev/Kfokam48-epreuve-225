package com.kfokam48.kfokam48.relecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Path;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.api.Test;

/** V7 must keep already graded work when upgrading a populated v6 database. */
class MigrationDoubleRelectureTest {

    @Test
    void conserveLaPremiereNoteEtAjouteUnSecondPairDisponible() throws Exception {
        String url = "jdbc:h2:file:" + Path.of(System.getProperty("java.io.tmpdir"),
                "migration_double_relecture_" + UUID.randomUUID());
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        long exerciceId;
        Flyway.configure().dataSource(dataSource).target(MigrationVersion.fromVersion("6"))
                .load().migrate();
        try (var connexion = DriverManager.getConnection(url, "sa", "");
                Statement statement = connexion.createStatement()) {
            statement.executeUpdate("insert into promotion(nom) values ('Migration test')", Statement.RETURN_GENERATED_KEYS);
            ResultSet promotionKeys = statement.getGeneratedKeys();
            promotionKeys.next();
            long promotionId = promotionKeys.getLong(1);
            statement.execute("insert into session_cours(promotion_id,titre,code,ouverture_at,expiration_at,statut) "
                    + "values (" + promotionId + ",'Session test','MIGR01',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'OUVERTE')");
            ResultSet sessionResult = statement.executeQuery("select id from session_cours where code = 'MIGR01'");
            sessionResult.next();
            long sessionId = sessionResult.getLong(1);
            long auteurId = insererEtudiant(statement, promotionId, "Auteur");
            long pairAId = insererEtudiant(statement, promotionId, "Pair A");
            long pairBId = insererEtudiant(statement, promotionId, "Pair B");
            statement.execute("insert into presence(session_id,etudiant_id,source,marquee_at) "
                    + "values (" + sessionId + "," + auteurId + ",'FORMATEUR',CURRENT_TIMESTAMP),"
                    + "(" + sessionId + "," + pairAId + ",'FORMATEUR',CURRENT_TIMESTAMP),"
                    + "(" + sessionId + "," + pairBId + ",'FORMATEUR',CURRENT_TIMESTAMP)");
            statement.execute("insert into exercice(session_id,etudiant_id,lien,statut,depose_at) "
                    + "values (" + sessionId + "," + auteurId + ",'https://example.test/exercice','EN_ATTENTE_RELECTURE',CURRENT_TIMESTAMP)");
            ResultSet exerciceResult = statement.executeQuery("select id from exercice where session_id = " + sessionId);
            exerciceResult.next();
            exerciceId = exerciceResult.getLong(1);
            statement.execute("insert into relecture(exercice_id,relecteur_id,note,commentaire,rendue_at) "
                    + "values (" + exerciceId + "," + pairAId + ",14,'Note historique',CURRENT_TIMESTAMP)");
        }
        Flyway.configure().dataSource(dataSource).load().migrate();
        try (var connexion = DriverManager.getConnection(url, "sa", "");
                Statement statement = connexion.createStatement();
                ResultSet resultat = statement.executeQuery(
                        "select count(*), min(rang), max(rang), max(case when rang=1 then note end), "
                                + "max(case when rang=1 then commentaire end) from relecture where exercice_id = "
                                + exerciceId)) {
            resultat.next();
            assertThat(resultat.getInt(1)).isEqualTo(2);
            assertThat(resultat.getInt(2)).isEqualTo(1);
            assertThat(resultat.getInt(3)).isEqualTo(2);
            assertThat(resultat.getInt(4)).isEqualTo(14);
            assertThat(resultat.getString(5)).isEqualTo("Note historique");
        }
    }

    private long insererEtudiant(Statement statement, long promotionId, String nom) throws Exception {
        statement.executeUpdate("insert into etudiant(promotion_id,nom) values (" + promotionId + ", '" + nom + "')",
                Statement.RETURN_GENERATED_KEYS);
        try (ResultSet keys = statement.getGeneratedKeys()) {
            keys.next();
            return keys.getLong(1);
        }
    }
}
