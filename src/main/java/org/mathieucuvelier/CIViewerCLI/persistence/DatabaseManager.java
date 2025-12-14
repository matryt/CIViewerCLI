package org.mathieucuvelier.CIViewerCLI.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DatabaseManager {
    private static final String DB_PATH = ".civiewer/state.db";
    private final Connection connection;

    public DatabaseManager() throws IOException, SQLException {
        Path civiewerDir = Path.of(".civiewer");
        if (!Files.exists(civiewerDir)) {
            Files.createDirectories(civiewerDir);
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        initDatabase();
    }

    private void initDatabase() throws SQLException {
        String createTables = """
            CREATE TABLE IF NOT EXISTS repo_state (
                owner TEXT NOT NULL,
                repo TEXT NOT NULL,
                last_check_timestamp INTEGER NOT NULL,
                PRIMARY KEY (owner, repo)
            );

            CREATE TABLE IF NOT EXISTS run_state (
                owner TEXT NOT NULL,
                repo TEXT NOT NULL,
                run_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                conclusion TEXT,
                last_updated INTEGER NOT NULL,
                PRIMARY KEY (owner, repo, run_id)
            );

            CREATE TABLE IF NOT EXISTS job_state (
                owner TEXT NOT NULL,
                repo TEXT NOT NULL,
                run_id INTEGER NOT NULL,
                job_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                conclusion TEXT,
                last_updated INTEGER NOT NULL,
                PRIMARY KEY (owner, repo, run_id, job_id)
            );
        """;

        connection.createStatement().executeUpdate(createTables);
    }

    public Connection getConnection() {
        return connection;
    }

    public <T> List<T> preparedQuery(
            PreparedStatement preparedStatement,
            Function<ResultSet, T> mapper
    ) throws SQLException {

        List<T> results = new ArrayList<>();

        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                results.add(mapper.apply(resultSet));
            }
        }

        return results;
    }

    public void executePreparedUpdate(PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.executeUpdate();
    }

    public void close() throws SQLException {
        connection.close();
    }
}
