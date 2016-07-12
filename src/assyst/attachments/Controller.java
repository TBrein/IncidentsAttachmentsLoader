/*
 * Данная программа разработана Бедаревым Вячеславом специально для аналитиков L3 проекта МВИДЕО "ФОБО".
 *
 * Использование вне проекта запрещено!
 */

package assyst.attachments;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.*;
import java.sql.*;
import java.util.Optional;

public class Controller {

    public Button downloadButton;
    public TextField incidentNumber;
    public ProgressBar downloadBar;
    public Label statusLabel;
    private String login = "";
    private String password = "";
    private String saveDir = "C:\\TMP\\Download\\";

    public void downloadAction() {
        final int[] fc = new int[1];
        Alert viewError = new Alert(Alert.AlertType.ERROR);
        Alert viewInfo = new Alert(Alert.AlertType.INFORMATION);
        Stage viewErrorStage = (Stage) viewError.getDialogPane().getScene().getWindow();
        viewErrorStage.setAlwaysOnTop(true);
        Stage viewInfoStage = (Stage) viewInfo.getDialogPane().getScene().getWindow();
        viewInfoStage.setAlwaysOnTop(true);

        viewError.initModality(Modality.APPLICATION_MODAL);
        viewInfo.initModality(Modality.APPLICATION_MODAL);

        viewErrorStage.getIcons().add(new Image(this.getClass().getResource("res/icon16.png").toString()));
        viewInfoStage.getIcons().add(new Image(this.getClass().getResource("res/icon16.png").toString()));

        if (incidentNumber.getText().length() > 0) {
            int b;
            try {
                b = Integer.parseInt(incidentNumber.getText());
            } catch (NumberFormatException e) {
                b = -1;
            }
            if ((incidentNumber.getText().length() == 7) && (b > 1000000)) {
                if (login.isEmpty() | password.isEmpty()) getLoginAndPassword();

                downloadButton.setDisable(true);
                incidentNumber.setDisable(true);
                try {
                    int res = checkIncidentNumber(incidentNumber.getText());
                    if (res > 0) {
                        //todo Доделать окно выбора папки
/*                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        directoryChooser.setInitialDirectory(new File(saveDir));
                        directoryChooser.setTitle("Выберите папку для сохранения выгружаемых вложений");
                        File selectedDirectory = directoryChooser.showDialog(null);
                        System.out.println(selectedDirectory.getAbsolutePath());
*/
                        downloadBar.setVisible(true);
                        new Thread(() -> {
                            try {
                                fc[0] = saveBLOBdataToFiles(incidentNumber.getText());
                                if (fc[0] > 0) {
                                    Platform.runLater(() -> {
                                        viewInfo.setTitle("Успешное сохранение");
                                        viewInfo.setHeaderText(null);
                                        viewInfo.setContentText("Из инцидента " + incidentNumber.getText() + " выгружено " + fc[0] + " вложений.");
                                        viewInfo.showAndWait();
                                    });
                                } else {
                                    Platform.runLater(() -> {
                                        viewError.setTitle("Ошибка при сохранении");
                                        viewError.setHeaderText(null);
                                        viewError.setContentText("В инциденте " + incidentNumber.getText() + " нет ни одного вложения!");
                                        viewError.showAndWait();
                                    });
                                }
                            } catch (SQLException | FileNotFoundException e) {
                                Platform.runLater(() -> {
                                    viewError.setTitle("Ошибка при выполнении");
                                    viewError.setHeaderText("Ай, яй! Ошибочка вышла! Сообщите о ней на vbedarev@luxoft.com. Спасибо.");
                                    viewError.setContentText(e.getLocalizedMessage());
                                    viewError.showAndWait();
                                });
                            }
                            Platform.runLater(() -> {
                                downloadBar.setVisible(false);
                                downloadButton.setDisable(false);
                                incidentNumber.setText("");
                                incidentNumber.setDisable(false);
                            });
                        }).start();
                    }
                    if ((res != -1) && (res == 0)) {
                        downloadButton.setDisable(false);
                        incidentNumber.setDisable(false);
                        viewError.setTitle("Ошибка");
                        viewError.setHeaderText("Похоже, Вы ввели некорректный номер инцидента!");
                        viewError.setContentText("Проверьте правильность ввода номера инцидента. Не вводите преффиксы (T, R и др.). Номер инцидента должен содержать 7 цифр.\nПример: 2046188 или 2041272");
                        viewError.showAndWait();
                    } else {
                        downloadButton.setDisable(false);
                        incidentNumber.setDisable(false);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                viewError.setTitle("Ошибка");
                viewError.setHeaderText("Похоже, Вы ввели некорректный номер инцидента!");
                viewError.setContentText("Проверьте правильность ввода номера инцидента. Не вводите преффиксы (T, R и др.). Номер инцидента должен содержать 7 цифр.\nПример: 2046188 или 2041272");
                viewError.showAndWait();
            }
        } else {
            viewError.setTitle("Ошибка");
            viewError.setHeaderText("Введите номер инцидента!");
            viewError.setContentText("Не вводите преффиксы (T, R и др.). Номер инцидента должен содержать 7 цифр.\nПример, 2046188 или 2041272");
            viewError.showAndWait();
        }
    }

    private boolean getLoginAndPassword() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Аутентификация БД АССИСТ");
        dialog.setHeaderText("Пройдите аутентификацию");

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(this.getClass().getResource("res/icon16.png").toString()));
        stage.setAlwaysOnTop(true);

        dialog.initModality(Modality.APPLICATION_MODAL);

        dialog.setGraphic(new ImageView(this.getClass().getResource("res/login.png").toString()));

        ButtonType loginButtonType = new ButtonType("Войти", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Введите логин");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Введите пароль");

        grid.add(new Label("Логин:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(pass, 1, 1);

        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        pass.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(oldValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // устанавливаем фокус в поле username
        Platform.runLater(username::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), pass.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> {
            login = usernamePassword.getKey();
            password = usernamePassword.getValue();
        });
        return false;
    }

    private int checkIncidentNumber(String incident) throws SQLException {
        Connection con = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT count(*) AS \"count\" FROM incident WHERE incident_ref = %s;";
        sqlQuery = String.format(sqlQuery, incident);
        con = getDBConnection(login, password);
        if (con != null) {
            rs = executeSQLQuery(con, sqlQuery);
            if (rs != null && rs.next()) return Integer.parseInt(rs.getString("count"));
            else return 0;
        }
        return -1;
    }

    private String getAssystUserNameByLogin(Connection con, String login) {
        ResultSet rs;
        if (con != null) {
            rs = executeSQLQuery(con, String.format("SELECT u.assyst_usr_n FROM assyst_usr u WHERE u.assyst_usr_sc = '%s';", login));
            try {
                if (rs != null && rs.next()) {
                    return rs.getString("assyst_usr_n");
                } else {
                    System.out.println("Ошибка!");
                }
            } catch (SQLException e) {
                System.out.println("Ошибка!");
            }
        }
        return "";
    }

    private int saveBLOBdataToFiles(String incident) throws SQLException, FileNotFoundException {
        int filesCount = 0;
        String sqlQuery = "SELECT ole_filename, ole_item FROM ole_items WHERE aux_source_id = 1%s;";
        sqlQuery = String.format(sqlQuery, incident);
        ResultSet rs = executeSQLQuery(getDBConnection(login, password), sqlQuery);
        while (rs != null && rs.next()) {
            filesCount++;
            try {
                Blob blob = rs.getBlob("ole_item");
                InputStream inputStream = blob.getBinaryStream();
                File myPath = new File(saveDir + incident + "\\");
                myPath.mkdirs();
                OutputStream outputStream = new FileOutputStream(saveDir + incident + "\\" + rs.getString("ole_filename"));

                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                return filesCount;
            }
        }
        if (rs != null) {
            rs.close();
        }
        return filesCount;
    }

    private Connection getDBConnection(String login, String pass) {
        Connection con;
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName("10.95.1.56");
        ds.setPortNumber(1433);
        ds.setDatabaseName("mvideorus_db");
        ds.setUser(login);
        ds.setPassword(pass);
        try {
            if (!login.isEmpty() && !pass.isEmpty()) {
                con = ds.getConnection();
                statusLabel.setText("Добро пожаловать, " + getAssystUserNameByLogin(con, login));
                return con;
            }
        } catch (SQLServerException e) {
            this.login = "";
            this.password = "";

            Alert viewError = new Alert(Alert.AlertType.ERROR);

            Stage viewErrorStage = (Stage) viewError.getDialogPane().getScene().getWindow();
            viewErrorStage.getIcons().add(new Image(this.getClass().getResource("res/icon16.png").toString()));
            viewErrorStage.setAlwaysOnTop(true);
            viewError.initModality(Modality.APPLICATION_MODAL);

            viewError.setTitle("Ошибка аутентификации");
            viewError.setHeaderText(null);
            viewError.setContentText("Произошла ошибка аутентификации при использовании логина \"" + login + "\".\nПроверьте верность ввода логина, пароля и попробуйте снова.");
            viewError.showAndWait();
        }
        return null;
    }

    private ResultSet executeSQLQuery(Connection con, String sqlQuery) {
        try {
            return con.prepareCall(sqlQuery).executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}