// Gson for JSON parsing
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

// iText Core 9 essentials
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;

import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;

// Java standard libraries
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LatestMoviesToPdf {

    private static final String OMDB_URL = "https://www.omdbapi.com/";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) JavaHttpClient/11";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            String apiKey = props.getProperty("omdb.api.key");

            if (apiKey == null || apiKey.isBlank()) {
                System.err.println("API key not found in config.properties (key=omdb.api.key)");
                System.exit(2);
            }

            String query = args.length >= 1 ? args[0] : "2025";
            String out = args.length >= 2 ? args[1] : "latest-movies.pdf";

            System.out.println("[*] Searching movies with query: " + query);
            List<Movie> basic = fetchMovies(apiKey, query);

            List<Movie> detailedMovies = new ArrayList<>();
            for (Movie m : basic) {
                Movie full = fetchMovieDetailsWithRetry(m.imdbID, apiKey, 3, 250);
                if (full == null) {
                    full = m;
                } else {
                    if (full.Poster == null || "N/A".equalsIgnoreCase(full.Poster)) {
                        full.Poster = m.Poster;
                    }
                }
                detailedMovies.add(full);
            }

            createPdf(out, detailedMovies, query);
            System.out.println("[*] PDF created: " + out);

        } catch (Exception e) {
            System.err.println("[!] Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<Movie> fetchMovies(String apiKey, String search) throws IOException, InterruptedException {
        String url = OMDB_URL + "?apikey=" + encode(apiKey) + "&s=" + encode(search) + "&type=movie";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", UA)
                .GET()
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("OMDb HTTP error: " + res.statusCode());
        }

        SearchResult result = new Gson().fromJson(res.body(), SearchResult.class);
        if (result != null && "True".equalsIgnoreCase(result.Response) && result.Search != null) {
            return Arrays.asList(result.Search);
        }
        System.out.println("[!] OMDb said no results. Error: " + (result == null ? "null" : result.Error));
        return Collections.emptyList();
    }

    private static Movie fetchMovieDetailsWithRetry(String imdbID, String apiKey, int attempts, long sleepMs) {
        for (int i = 1; i <= attempts; i++) {
            try {
                Movie m = fetchMovieDetails(imdbID, apiKey);
                if (m != null && (m.imdbRating != null || m.Genre != null)) {
                    return m;
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(sleepMs * i); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        System.out.println("[!] Falling back after retries for " + imdbID);
        return null;
    }

    private static Movie fetchMovieDetails(String imdbID, String apiKey) throws IOException, InterruptedException {
        String url = OMDB_URL + "?apikey=" + encode(apiKey) + "&i=" + encode(imdbID) + "&plot=short";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", UA)
                .GET()
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("OMDb HTTP error: " + res.statusCode());
        }
        return new Gson().fromJson(res.body(), Movie.class);
    }

    private static void createPdf(String out, List<Movie> movies, String query) throws Exception {
        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Register footer event handler
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterEventHandler(regular));

            // --- Cover Page ---
            document.add(new Paragraph("Latest Movies Report").setFont(bold).setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Search Query: " + query).setFont(regular).setFontSize(14).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).setFont(regular).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            document.add(new AreaBreak());

            // --- Table ---
            float[] columnWidths = {80f, 120f, 50f, 60f, 80f, 80f, 60f, 60f, 50f, 100f, 100f, 120f};
            Table table = new Table(columnWidths).useAllAvailableWidth();

            String[] headers = {"Poster", "Title", "Year", "Runtime", "Language", "Country", "Type", "Rated", "IMDb", "Genre", "Director", "Actors"};
            for (String h : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold)));
            }

            for (Movie m : movies) {
                // Poster
                boolean posterAdded = false;
                if (m.Poster != null && !"N/A".equalsIgnoreCase(m.Poster)) {
                    byte[] imgBytes = fetchBytesPreferRastFormats(m.Poster);
                    if (imgBytes != null) {
                        try {
                            Image img = new Image(ImageDataFactory.create(imgBytes))
                                    .setAutoScale(false)
                                    .scaleToFit(60, 90)
                                    .setWidth(60).setHeight(90);
                            table.addCell(new Cell().add(img));
                            posterAdded = true;
                        } catch (Exception ignored) {}
                    }
                }
                if (!posterAdded) {
                    table.addCell(new Cell().add(new Paragraph("No Img").setFont(regular)));
                }

                // Title with link
                Link titleLink = new Link(safe(m.Title), com.itextpdf.kernel.pdf.action.PdfAction.createURI("https://www.imdb.com/title/" + safe(m.imdbID)));
                titleLink.setFontColor(ColorConstants.BLUE).setUnderline();
                table.addCell(new Cell().add(new Paragraph(titleLink).setFont(regular)));

                table.addCell(new Cell().add(new Paragraph(safe(m.Year)).setFont(regular)));
                table.addCell(new Cell().add(new Paragraph(safe(m.Runtime)).setFont(regular)));
                table.addCell(new Cell().add(new Paragraph(safe(m.Language)).setFont(regular)));
                table.addCell(new Cell().add(new Paragraph(safe(m.Country)).setFont(regular)));
                table.addCell(new Cell().add(new Paragraph(safe(m.Type)).setFont(regular)));
                table.addCell(new Cell().add(new Paragraph(safe(m.Rated)).setFont(regular)));
                table.addCell(ratingCell(safe(m.imdbRating), regular));
                table.addCell(new Cell().add(new Paragraph(safe(m.Genre)).setFont(regular)));
                table.addCell(new Cell().add(new Paragraph(safe(m.Director)).setFont(regular)));
                table.addCell(new Cell().add(new Paragraph(safe(m.Actors)).setFont(regular)));
            }

            document.add(table);
        }
    }

    private static Cell ratingCell(String ratingStr, PdfFont regular) {
        float rating = -1f;
        try { rating = Float.parseFloat(ratingStr); } catch (Exception ignored) {}
        Paragraph p = new Paragraph(ratingStr == null || ratingStr.isBlank() ? "N/A" : ratingStr).setFont(regular);
        Cell c = new Cell().add(p);

        if (rating >= 1 && rating < 5) {
            c.setBackgroundColor(ColorConstants.RED); p.setFontColor(ColorConstants.WHITE);
        } else if (rating >= 5 && rating < 7) {
            c.setBackgroundColor(ColorConstants.ORANGE); p.setFontColor(ColorConstants.BLACK);
        } else if (rating >= 7 && rating <= 10) {
            c.setBackgroundColor(ColorConstants.GREEN); p.setFontColor(ColorConstants.BLACK);
        } else {
            c.setBackgroundColor(ColorConstants.LIGHT_GRAY); p.setFontColor(ColorConstants.BLACK);
        }
        return c;
    }

    private static byte[] fetchBytesPreferRastFormats(String url) {
        return fetchBytesWithAccept(url, "image/jpeg,image/png,image/*;q=0.8,*/*;q=0.5");
    }

    private static byte[] fetchBytesWithAccept(String url, String accept) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", UA)
                    .header("Accept", accept)
                    .GET()
                    .build();
            HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (res.statusCode() == 200) {
                return res.body();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("\\p{Cntrl}", " ").trim();
    }

// --- Footer Handler ---
private static class FooterEventHandler extends AbstractPdfDocumentEventHandler {
    private final PdfFont font;
    FooterEventHandler(PdfFont font) { this.font = font; }

    @Override
    protected void onAcceptedEvent(AbstractPdfDocumentEvent event) {
        if (!(event instanceof PdfDocumentEvent)) {
            return;
        }
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        Rectangle rect = page.getPageSize();
        Canvas canvas = new Canvas(new PdfCanvas(page), rect);

        // Rating scale
        float[] scaleCols = new float[10];
        Arrays.fill(scaleCols, 25f);
        Table scaleTable = new Table(scaleCols).setMargin(0).setPadding(0);

        String[] labels = {"Terrible", "Awful", "Bad", "Pretty Bad", "Just OK", "Worth Watching", "Fairly Good", "Good", "Great", "Perfection"};
        for (String label : labels) {
            scaleTable.addCell(new Cell()
                    .add(new Paragraph(label).setFont(font).setFontSize(6).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(ColorConstants.YELLOW));
        }

        for (int i = 1; i <= 10; i++) {
            Cell c = new Cell().add(new Paragraph(String.valueOf(i))
                    .setFont(font).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
            if (i <= 6) c.setBackgroundColor(ColorConstants.RED);
            else if (i <= 8) c.setBackgroundColor(ColorConstants.ORANGE);
            else c.setBackgroundColor(ColorConstants.GREEN);
            scaleTable.addCell(c);
        }

        scaleTable.setFixedPosition(rect.getLeft() + 40, rect.getBottom() + 20, 250);
        canvas.add(scaleTable);

        // Signature
        String signature = "Generated by LatestMoviesToPdf • " + LocalDate.now();
        canvas.showTextAligned(signature,
                rect.getRight() - 40, rect.getBottom() + 25,
                TextAlignment.RIGHT).setFontSize(9).setFont(font);

        canvas.close();
    }
}

    private static class SearchResult {
        @SerializedName("Response") String Response;
        @SerializedName("Error") String Error;
        @SerializedName("Search") Movie[] Search;
    }

    static class Movie {
        @SerializedName("Title") String Title;
        @SerializedName("Year") String Year;
        @SerializedName("imdbID") String imdbID;
        @SerializedName("Type") String Type;
        @SerializedName("Poster") String Poster;
        @SerializedName("Genre") String Genre;
        @SerializedName("Director") String Director;
        @SerializedName("Actors") String Actors;
        @SerializedName("imdbRating") String imdbRating;
        @SerializedName("Runtime") String Runtime;
        @SerializedName("Language") String Language;
        @SerializedName("Country") String Country;
        @SerializedName("Rated") String Rated;
    }
}
