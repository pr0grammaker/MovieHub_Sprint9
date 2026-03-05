package handlers;
import baseclasses.ErrorResponse;
import baseclasses.Movie;
import baseclasses.MoviesStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");

            if (contentType == null || !contentType.equalsIgnoreCase("application/json")) {
                sendJson(ex, 415, gson.toJson(new ErrorResponse(
                        "UnsupportedMediaType", "Ожидался Content-Type: application/json")));
                return;
            }
        }

        switch (method) {
            case "GET":
                if (query != null && query.startsWith("year=")) {
                    handleGetMoviesByYear(ex, query);
                } else if (path.startsWith("/movies/")) {
                    handleGetMovieById(ex, path);
                } else if (path.equals("/movies")) {
                    handleGetMovies(ex);
                }
                break;

            case "POST":
                if (path.equals("/movies")) {
                    handlePostMovies(ex);
                }
                break;

            case "DELETE":
                if (path.startsWith("/movies/")) {
                    handleDeleteMovie(ex, path);
                }
                break;
            default:
                sendJson(ex, 405, gson.toJson(new ErrorResponse("MethodNotAllowed", "Метод не поддерживается")));


        }


    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        String response = gson.toJson(moviesStore.getMovies());
        sendJson(ex, 200, response);
        System.out.println("Запрос выполнился успешно!");
    }

    private void handlePostMovies(HttpExchange ex) throws IOException {
        String body;
        try {
            body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            sendJson(ex, 422, gson.toJson(
                    new ErrorResponse("InvalidRequest", "Некорректное тело запроса")));
            return;
        }

        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (Exception e) {
            sendJson(ex, 422, gson.toJson(new ErrorResponse("ValidationError", "Некорректный JSON")));
            return;
        }

        if (movie.getId() != 0) {
            sendJson(ex, 422, gson.toJson(
                    new ErrorResponse("ValidationError", "ID не должен передаваться при создании фильма")));
            return;
        }

        if (movie.getTitle() == null || movie.getTitle().isBlank() || movie.getTitle().length() > 100) {
            sendJson(ex, 422, gson.toJson(
                    new ErrorResponse("ValidationError", "Название не должно быть пустым и ≤ 100 символов")));
            return;
        }

        int currentYear = java.time.Year.now().getValue();
        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            sendJson(ex, 422, gson.toJson(
                    new ErrorResponse("ValidationError", "Год должен быть между 1888 и " + (currentYear + 1))));
            return;
        }

        Movie created = moviesStore.addMovie(movie);
        sendJson(ex, 201, gson.toJson(created));
    }

    public void handleGetMovieById(HttpExchange ex, String path) throws IOException {


        String[] parts = path.split("/");

        if (parts.length < 3) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Bad Request", "Некорректный путь")));
            return;
        }

        int id;
        try {
            id = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Bad Request", "Некорректный ID")));
            return;
        }

        Optional<Movie> movieOptional = moviesStore.findById(id);
        if (movieOptional.isEmpty()) {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Not Found", "Фильм не найден")));
            return;
        }

        Movie movie = movieOptional.get();
        sendJson(ex, 200, gson.toJson(movie));

    }

    public void handleDeleteMovie(HttpExchange ex, String path) throws IOException {

        String[] parts = path.split("/");
        int id;
        try {
            id = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Bad Request", "Некорректный ID")));
            return;
        }

        Optional<Movie> movieOptional = moviesStore.findById(id);
        if (movieOptional.isEmpty()) {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Not Found", "Фильм не найден")));
            return;
        }

        boolean removeById = moviesStore.removeById(id);
        if (removeById) {
            sendNoContent(ex);
            System.out.println("Фильм с ID " + id + " удалён успешно.");
        } else {
            sendJson(ex, 500, gson.toJson(new ErrorResponse("InternalError",
                    "Не удалось удалить фильм")));
        }
    }

    public void handleGetMoviesByYear(HttpExchange ex, String query) throws IOException {
        String[] parts = query.split("=");

        if (parts.length != 2) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse(
                    "Bad Request", "Некорректный параметр запроса 'year'")));
            return;
        }

        int year;
        try {
            year = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Bad Request", "Некорректный параметр " +
                    "запроса — 'year'")));
            return;
        }

        List<Movie> movieOptional = moviesStore.getMoviesByYear(year);
        if (movieOptional.isEmpty()) {
            sendJson(ex, 404, gson.toJson(new ErrorResponse("Not Found", "Фильмы не найдены, " +
                    "список пуст")));
            return;
        }
        sendJson(ex, 200, gson.toJson(movieOptional));

    }

}
