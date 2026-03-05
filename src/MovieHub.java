import baseclasses.MoviesServer;
import baseclasses.MoviesStore;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Year;
import java.util.Scanner;

public class MovieHub {
    private final static int PORT = 8080;
    private final static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        MoviesStore moviesStore = new MoviesStore();

        MoviesServer moviesServer = new MoviesServer(moviesStore);
        System.out.println("Добро пожаловать в MovieHub: кинотеатр у вас в браузере");

        moviesServer.start();
        System.out.println("Сервер запущен на порту " + PORT);

        while (true) {


            printmenu();
            int command = sc.nextInt();
            sc.nextLine();

            switch (command) {
                case 1 -> System.out.println("""
                        Для получения списка всех фильмов откройте поисковую строку браузера
                        и введите информацию следующего содержания:
                        http://localhost:8080/movies
                        """);
                case 2 -> {
                    System.out.println("""
                            Для того чтобы добавить фильм введите название и год
                            """);
                    String name = sc.nextLine();
                    int year = sc.nextInt();
                    sc.nextLine();

                    String json = """
                            { 
                              "title": "%s",
                              "year": %d
                            }
                            """.formatted(name, year);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/movies"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    try {
                        var client = java.net.http.HttpClient.newHttpClient();
                        var response = client.send(request,
                                java.net.http.HttpResponse.BodyHandlers.ofString());

                        System.out.println("Статус: " + response.statusCode());
                        System.out.println("JSON отправляется...");
                        System.out.println("Ответ сервера:");
                        System.out.println(response.body());

                    } catch (Exception e) {
                        System.out.println("Ошибка при отправке запроса: " + e.getMessage());
                    }
                }

                case 3 -> {
                    System.out.println("""
                            Для получения фильма по ID из списка всех фильмов введите его ID
                            """);
                    int id = sc.nextInt();
                    sc.nextLine();
                    while (id <= 0) {
                        id = sc.nextInt();
                        sc.nextLine();
                        System.out.println("ID не может быть нулем или отрицательным числом!");
                    }

                    System.out.printf("""
                            Перейдите по следующей ссылке чтобы получить фильм:
                            http://localhost:8080/movies/%d
                            """, id);
                }


                case 4 -> {
                    System.out.println("""
                            Для удаления фильма по ID из списка всех фильмов введите его ID
                            """);

                    int id = sc.nextInt();
                    sc.nextLine();
                    while (id <= 0) {
                        System.out.println("ID не может быть нулем или отрицательным числом!");
                        id = sc.nextInt();
                        sc.nextLine();
                    }

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/movies/" + id))
                            .DELETE()
                            .build();

                    try {
                        var client = java.net.http.HttpClient.newHttpClient();
                        var response = client.send(request,
                                java.net.http.HttpResponse.BodyHandlers.ofString());

                        System.out.println("Статус: " + response.statusCode());

                        if (response.statusCode() == 204){
                            System.out.println("Фильм успешно удален!");
                        } else {
                            System.out.println("Ответ сервера:");
                            System.out.println(response.body());
                        }

                    } catch (InterruptedException | IOException e) {
                        System.out.println("Ошибка при отправке запроса: " + e.getMessage());
                    }
                }

                case 5 -> {
                    System.out.println("""
                            Для фильтрации фильмов по году выпуска из списка всех фильмов введите год
                            """);
                    int year = sc.nextInt();
                    sc.nextLine();
                    while (year <= 1888 || year >= Year.now().getValue()) {
                        year = sc.nextInt();
                        sc.nextLine();
                        System.out.println("Год не может быть нулем или отрицательным числом, а также не позже 1888 и нынешнего года!");
                    }
                    System.out.printf("""
                            Перейдите по следующей ссылке чтобы получить список отфильтрованных фильмов:
                            http://localhost:8080/movies?year=%d
                            """, year);
                }

                case 6 -> {
                    System.out.println("Завершение работы...");
                    moviesServer.stop();
                    return;
                }
                default -> {
                    System.out.println("Неизвестная команда");
                    moviesServer.stop();
                    return;
                }
            }


        }


    }

    public static void printmenu() {
        System.out.println("""
                Для получения инструкции для выполнения функции выберите операцию
                Что желаете сделать из перечня функций?
                1. Получение всех фильмов
                2. Добавление фильма
                3. Получение фильма по ID
                4. Удаление фильма
                5. Фильтрация по году выпуска
                6. Выход
                """);
    }
}
