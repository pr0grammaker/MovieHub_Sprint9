package baseclasses;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesStore {

    private final List<Movie> movies = new ArrayList<>();
    private int nextId = 1;

    public Movie addMovie(Movie movie) {
        movie.setId(nextId++);
        movies.add(movie);
        return movie;
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movies);
    }

    public Optional<Movie> findById(int id) {
        return movies.stream()
                .filter(m -> m.getId() == id)
                .findFirst();
    }

    public boolean removeById(int id) {
        return movies.removeIf(m -> m.getId() == id);
    }

    public void clear() {
        movies.clear();
        nextId = 1; // сброс для тестов
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }
}