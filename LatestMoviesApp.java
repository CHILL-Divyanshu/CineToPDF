// LatestMoviesApp.java
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;
import java.util.Properties;

public class LatestMoviesApp extends Application {

    private TextField queryField;
    private TextField yearField;
    private TextField outputField;
    private TextArea logArea;
    private String apiKey;

    @Override
    public void start(Stage stage) {
        // Load API key
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(".env"));  // Or config.properties
            apiKey = props.getProperty("omdb.api.key");
        } catch (Exception e) {
            showError("Could not load .env file with omdb.api.key");
            return;
        }

        Label title = new Label("🎬 OMDb Movie to PDF Generator");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        queryField = new TextField();
        queryField.setPromptText("Enter movie keyword (e.g. Batman)");

        yearField = new TextField();
        yearField.setPromptText("Year (default: current year)");

        outputField = new TextField("latest-movies.pdf");
        outputField.setPromptText("Output PDF filename");

        Button generateBtn = new Button("Generate PDF");
        generateBtn.setOnAction(e -> runGeneration());

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(12);

        VBox root = new VBox(10, title, queryField, yearField, outputField, generateBtn, logArea);
        root.setPadding(new Insets(15));

        Scene scene = new Scene(root, 500, 400);
        stage.setScene(scene);
        stage.setTitle("OMDb → PDF Generator");
        stage.show();
    }

    private void runGeneration() {
        String query = queryField.getText().trim();
        String year = yearField.getText().trim().isEmpty() ? String.valueOf(Year.now().getValue()) : yearField.getText().trim();
        String outFile = outputField.getText().trim();

        if (query.isEmpty()) {
            showError("Please enter a movie keyword.");
            return;
        }

        appendLog("[*] Fetching movies for: " + query + " (" + year + ")");
        try {
            List<LatestMoviesService.Movie> movies = LatestMoviesService.fetchMovies(apiKey, query, year);
            if (movies.isEmpty()) {
                appendLog("[!] No movies found.");
                return;
            }
            LatestMoviesService.createPdf(outFile, movies, query, year);
            appendLog("[✔] PDF created: " + Path.of(outFile).toAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Error: " + ex.getMessage());
        }
    }

    private void appendLog(String msg) {
        logArea.appendText(msg + "\n");
    }

    private void showError(String msg) {
        appendLog("[!] " + msg);
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
