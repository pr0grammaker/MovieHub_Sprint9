import baseclasses.Movie;
import baseclasses.MoviesServer;
import baseclasses.MoviesStore;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesStore moviesStore;
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();

        moviesStore = server.getMoviesStore();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    private String movieJson(String title, int year) {
        return """
                {
                  "title": "%s",
                  "year": %d
                }
                """.formatted(title, year);
    }


// Эндпоинт — GET /movies.

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + path))
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenMoviesStoreNotNull_returnsMovies() throws Exception {
        moviesStore.addMovie(new Movie("Interstellar", 2010));
        moviesStore.addMovie(new Movie("Harry Poter", 2014));

        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        assertTrue(body.contains("Interstellar"));
        assertTrue(body.contains("Harry Poter"));
    }


    // Эндпоинт — POST /movies.


    private HttpResponse<String> postMovie(String json) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void postMovies_whenEmpty_returnsMovie() throws Exception {
        String json = movieJson("Inception", 2010);

        HttpResponse<String> resp = postMovie(json);

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        assertTrue(body.contains("Inception"));
        assertTrue(body.contains("2010"));
        assertTrue(body.contains("\"id\": 1"));
    }

    @Test
    void postMovies_withIdProvided_returns422() throws Exception {

        String json = """
                {
                  "id": 100,
                  "title": "Inception",
                  "year": 2010
                }
                """;

        HttpResponse<String> resp = postMovie(json);

        assertEquals(422, resp.statusCode());

        assertTrue(resp.body().contains("ID не должен передаваться"));
    }

    @Test
    void postMovies_notCorrectValueContentType_returns415() throws Exception {

        String json = movieJson("Inception", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    void postMovies_emptyTitle_returns422() throws Exception {

        String json = movieJson("", 2010);

        HttpResponse<String> resp = postMovie(json);

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovies_lengthMoreThan100CharactersTitle_returns422() throws Exception {
        String json = movieJson("Incewfuiewfwofwifwfhwofiwfhiwhfiowfoiwfhiuwfhoiwfhwfiowfiowfhiowfoiwfhiowf" +
                "owfhwfwfiohwfiowfwfowfwfwfiwhfioption", 2010);

        HttpResponse<String> resp = postMovie(json);

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovies_yearOutsideTheInterval_returns422() throws Exception {
        String json1 = movieJson("Interstellar", 1880);
        String json2 = movieJson("Breaking Bad", 2030);

        HttpResponse<String> resp1 = postMovie(json1);

        HttpResponse<String> resp2 = postMovie(json2);

        assertEquals(422, resp1.statusCode());
        assertEquals(422, resp2.statusCode());
    }

    @Test
    void postMovies_MovieStoreHasMovies_returnsCorrectIdMovies() throws Exception {
        String json1 = movieJson("Inception", 2010);
        String json2 = movieJson("Interstellar", 2014);

        HttpResponse<String> resp1 = postMovie(json1);
        HttpResponse<String> resp2 = postMovie(json2);

        assertEquals(201, resp1.statusCode());
        assertEquals(201, resp2.statusCode());

        String body1 = resp1.body().trim();
        assertTrue(body1.contains("Inception"));
        assertTrue(body1.contains("2010"));
        assertTrue(body1.contains("\"id\": 1"));

        String body2 = resp2.body().trim();
        assertTrue(body2.contains("Interstellar"));
        assertTrue(body2.contains("2014"));
        assertTrue(body2.contains("\"id\": 2"));
    }


    // Эндпоинт — GET /movies/{id}.

    @Test
    void getMoviesOnID_whenNotEmpty_returnsMovieOnID() throws Exception {
        String json = movieJson("Inception", 2010);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> postResp =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, postResp.statusCode());


        Gson gson = new Gson();
        Movie createdMovie = gson.fromJson(postResp.body(), Movie.class);
        int id = createdMovie.getId();


        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, getResp.statusCode());


        Movie movieById = gson.fromJson(getResp.body(), Movie.class);


        assertEquals(id, movieById.getId());
        assertEquals("Inception", movieById.getTitle());
        assertEquals(2010, movieById.getYear());

    }

    @Test
    void getMoviesOnID_whenMovieNotFound_returns404() throws Exception {


        HttpResponse<String> resp = get("/movies/9999");

        assertEquals(404, resp.statusCode(), "GET /movies/{id} для несуществующего фильма должен вернуть 404");
        assertTrue(resp.body().contains("Фильм не найден"), "Ожидается сообщение об ошибке 'Фильм не найден'");
    }

    @Test
    void getMoviesOnID_whenIDNotDigit_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies/abc");

        assertEquals(400, resp.statusCode(), "GET /movies/{id} с нечисловым ID должен вернуть 400");
        assertTrue(resp.body().contains("Некорректный ID"), "Ожидается сообщение об ошибке 'Некорректный ID'");
    }

    // Эндпоинт — DELETE /movies/{id}.

    private HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + path))
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void deleteMovieOnID_NormalSituation() throws Exception {

        String json = """
                {
                  "title": "Inception",
                  "year": 2010
                }
                """;

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> postResp =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, postResp.statusCode());


        Gson gson = new Gson();
        Movie createdMovie = gson.fromJson(postResp.body(), Movie.class);
        int id = createdMovie.getId();


        HttpResponse<String> resp = delete("/movies/" + id);
        assertEquals(204, resp.statusCode());

    }

    @Test
    void deleteMoviesOnID_whenMovieNotFound_returns404() throws Exception {

        HttpResponse<String> resp = delete("/movies/" + 9999);

        assertEquals(404, resp.statusCode(), "Удаление несуществующего фильма должно вернуть 404");
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void deleteMovieOnID_WhenIDNotDigit_returns400() throws Exception {

        HttpResponse<String> delResp = delete("/movies/abc");

        assertEquals(400, delResp.statusCode(), "DELETE /movies/{id} с нечисловым ID должен вернуть 400");
        assertTrue(delResp.body().contains("Некорректный ID"), "Ожидается сообщение об ошибке 'Некорректный ID'");
    }

    // Эндпоинт — GET /movies?year=YYYY.


    public static void startForGET() throws IOException, InterruptedException {

        String movie1 = """
                {
                  "title": "Inception",
                  "year": 2010
                }
                """;

        String movie2 = """
                {
                  "title": "Interstellar",
                  "year": 2015
                }
                """;

        String movie3 = """
                {
                  "title": "Breaking Bad",
                  "year": 2012
                }
                """;

        String movie4 = """
                {
                  "title": "Game of Thrones",
                  "year": 2015
                }
                """;

        String[] movies = {movie1, movie2, movie3, movie4};

        for (String movie : movies) {
            client.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(BASE + "/movies"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(movie))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
        }


    }

    @Test
    void getListMoviesOnYear_returns200() throws Exception {
        startForGET();

        HttpRequest getReq = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=2015"))
                .build();

        HttpResponse<String> response = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

    }

    @Test
    void getListMoviesOnYear_WhenNotCorrectID_returns400() throws Exception {

        startForGET();

        HttpRequest getReq = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .build();

        HttpResponse<String> response = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, response.statusCode());

    }

    @Test
    void getListMoviesOnYear_WhenNotEmptyOrEmpty_returns200() throws Exception {

        HttpRequest getReq = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=2015"))
                .build();

        HttpResponse<String> response = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode());

    }


}

