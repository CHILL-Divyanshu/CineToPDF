// LatestMoviesService.java
import com.google.gson.Gson;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class LatestMoviesService {

    // Fetch movies from OMDb
    public static List<Movie> fetchMovies(String apiKey, String query, String year) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        Gson gson = new Gson();

        String url = "http://www.omdbapi.com/?apikey=" + encode(apiKey)
                   + "&s=" + encode(query)
                   + "&type=movie"
                   + "&y=" + encode(year);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<java.io.InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());

        if (res.statusCode() != 200) {
            System.err.println("[!] OMDb HTTP error " + res.statusCode());
            return Collections.emptyList();
        }

        MovieSearchResponse resp = gson.fromJson(new InputStreamReader(res.body()), MovieSearchResponse.class);
        if (resp != null && resp.Search != null) {
            return resp.Search;
        }
        return Collections.emptyList();
    }

    // Create PDF with movie results
    public static void createPdf(String filename, List<Movie> movies, String query, String year) throws IOException {
        Path out = Path.of(System.getProperty("user.dir"), filename);
        try (PdfWriter writer = new PdfWriter(out.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            doc.add(new Paragraph("Movie Results for: " + query + " (Year: " + year + ")")
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Generated: " + ZonedDateTime.now()).setFontSize(9));

            if (movies == null || movies.isEmpty()) {
                doc.add(new Paragraph("No movies found."));
            } else {
                PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                for (Movie m : movies) {
                    doc.add(new Paragraph(m.Title + " (" + m.Year + ")")
                            .setFontSize(12).setFont(boldFont));
                    doc.add(new Paragraph("IMDb ID: " + m.imdbID).setFontSize(9));
                    doc.add(new Paragraph("Type: " + m.Type).setFontSize(9));
                    doc.add(new Paragraph("Poster: " + (m.Poster.equals("N/A") ? "Not available" : m.Poster)).setFontSize(9));
                    doc.add(new Paragraph(" "));
                }
            }
        }
    }

    private static String encode(String s) {
        return s == null ? "" : s.replace(" ", "%20");
    }

    // DTOs for JSON mapping
    public static class MovieSearchResponse {
        List<Movie> Search;
    }

    public static class Movie {
        String Title;
        String Year;
        String imdbID;
        String Type;
        String Poster;
    }
}
