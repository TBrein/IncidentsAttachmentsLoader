/*
 * Данная программа разработана Бедаревым Вячеславом специально для аналитиков L3 проекта МВИДЕО "ФОБО".
 *
 * Использование вне проекта запрещено!
 */

package assyst.attachments;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.*;
import java.sql.*;
import java.util.Optional;
import java.util.Properties;

public class Controller {

    public Button downloadButton;
    public Button changePathBtn;
    public TextField incidentNumber;
    public TextField pathTextField;
    public ProgressBar downloadBar;
    public Label statusLabel;
    public CheckBox useIncidentNumAsFolder;

    private FadeTransition ft;
    private String login;
    private String password;
    private String saveDir;

    @FXML
    void initialize() {
        login = "";
        password = "";
        initProgramSettings();

        String numberMatcher = "[0-9]+";
        //t1 - новый текст, s - старый текст.
        incidentNumber.textProperty().addListener((observableValue, s, t1) -> {
            t1 = t1.trim();
            if (!t1.isEmpty()) {
                if (!t1.matches(numberMatcher)) {
                    incidentNumber.setText(s);
                } else {
                    if (t1.length() > 7) {
                        incidentNumber.setText(s);
                    } else incidentNumber.setText(t1);
                }
            }
        });
        pathTextField.setText(saveDir);

        ft = new FadeTransition(Duration.millis(900), statusLabel);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
    }

    private Properties readPropertiesFile() {
        Properties props = new Properties();
        File f = new File("./settings.xml");
        if (f.exists()) {
            try {
                InputStream is = new BufferedInputStream(new FileInputStream(f));
                props.loadFromXML(is);
                is.close();
                return props;
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private boolean savePropertiesToFile(Properties props) {
        try {
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(new File("./settings.xml")));
            props.storeToXML(outputStream, "", "UTF-8");
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void makePath(File path) {
        if (!path.mkdirs() && !path.exists()) {
            Platform.runLater(() -> {
                Alert viewError = new Alert(Alert.AlertType.ERROR);
                Stage viewErrorStage = (Stage) viewError.getDialogPane().getScene().getWindow();
                viewErrorStage.setAlwaysOnTop(true);
                viewError.initModality(Modality.APPLICATION_MODAL);
                viewErrorStage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));

                viewError.setTitle("Ошибка");
                viewError.setHeaderText(null);
                viewError.setContentText("Не удалось создать папку \"" + saveDir + "\"");
                viewError.showAndWait();
            });
        }
    }

    private void initProgramSettings() {
        Properties props = readPropertiesFile();

        Alert viewError = new Alert(Alert.AlertType.ERROR);
        Stage viewErrorStage = (Stage) viewError.getDialogPane().getScene().getWindow();
        viewErrorStage.setAlwaysOnTop(true);
        viewError.initModality(Modality.APPLICATION_MODAL);
        viewErrorStage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));

        if (props != null) {
            login = props.getProperty("Login");
            password = decrypt(props.getProperty("Password").getBytes(), login);
            saveDir = props.getProperty("Path");
            useIncidentNumAsFolder.setSelected(Boolean.parseBoolean(props.getProperty("useIncidentNumAsFolder")));
            File myPath = new File(saveDir);
            makePath(myPath);
        } else {
            saveDir = "C:\\Download\\";
            File myPath = new File(saveDir);
            makePath(myPath);
            saveProgramSettings();
        }
    }

    private void saveProgramSettings() {
        Properties props = readPropertiesFile();
        if (props != null) {
            props.setProperty("Login", login);
            props.setProperty("Password", new String(encrypt(password, login)));
            props.setProperty("Path", saveDir);
            props.setProperty("useIncidentNumAsFolder", String.valueOf(useIncidentNumAsFolder.isSelected()));
            savePropertiesToFile(props);
        } else {
            props = new Properties();
            props.setProperty("Login", login);
            props.setProperty("Password", new String(encrypt(password, login)));
            props.setProperty("Path", saveDir);
            props.setProperty("useIncidentNumAsFolder", String.valueOf(useIncidentNumAsFolder.isSelected()));
            savePropertiesToFile(props);
        }
    }

    private void setDisableToControls(boolean enable) {
        downloadButton.setDisable(enable);
        incidentNumber.setDisable(enable);
        changePathBtn.setDisable(enable);
    }

    private void stopBlinking() {
        ft.stop();
        ft.setCycleCount(1);
        ft.play();
    }

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

        viewErrorStage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));
        viewInfoStage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));

        stopBlinking();
        if (incidentNumber.getText().length() > 0) {
            int b;
            try {
                b = Integer.parseInt(incidentNumber.getText());
            } catch (NumberFormatException e) {
                b = -1;
            }
            if ((incidentNumber.getText().length() == 7) && (b > 1000000)) {
                if (login.isEmpty() | password.isEmpty()) getLoginAndPassword();
                setDisableToControls(true);
                try {
                    int res = checkIncidentNumber(incidentNumber.getText());
                    if (res > 0) {
                        downloadBar.setVisible(true);
                        new Thread(() -> {
                            try {
                                fc[0] = saveBLOBDataToFiles(incidentNumber.getText());
                                if (fc[0] > 0) {
                                    Platform.runLater(() -> {
                                        viewInfo.setTitle("Успешное сохранение");
                                        viewInfo.setHeaderText(null);
                                        viewInfo.setContentText("Из инцидента " + incidentNumber.getText() + " выгружено " + fc[0] + " вложений.");
                                        viewInfo.showAndWait();
                                    });
                                }
                                if (fc[0] == 0) {
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
                                incidentNumber.setText("");
                                setDisableToControls(false);
                            });
                        }).start();
                    }
                    if ((res != -1) && (res == 0)) {
                        setDisableToControls(false);
                        viewError.setTitle("Ошибка");
                        viewError.setHeaderText("В БД АССИСТ не существует инцидента с номером " + incidentNumber.getText());
                        viewError.setContentText("Проверьте правильность ввода номера инцидента.\nНомер инцидента должен содержать 7 цифр.\nПример: 2046188 или 2041272");
                        viewError.showAndWait();
                    } else {
                        setDisableToControls(false);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                viewError.setTitle("Ошибка");
                viewError.setHeaderText("Похоже, Вы ввели некорректный номер инцидента!");
                viewError.setContentText("Проверьте правильность ввода номера инцидента. Номер инцидента должен содержать 7 цифр.\nПример: 2046188 или 2041272");
                viewError.showAndWait();
            }
        } else {
            viewError.setTitle("Ошибка");
            viewError.setHeaderText("Введите номер инцидента!");
            viewError.setContentText("Номер инцидента должен содержать 7 цифр.\nПример, 2046188 или 2041272");
            viewError.showAndWait();
        }
    }

    private boolean getLoginAndPassword() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Аутентификация БД АССИСТ");
        dialog.setHeaderText("Пройдите аутентификацию");

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));
        stage.setAlwaysOnTop(true);

        dialog.initModality(Modality.APPLICATION_MODAL);

        dialog.setGraphic(new ImageView(this.getClass().getResource("res/login.png").toString()));

        ButtonType loginButtonType = new ButtonType("Войти", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Введите логин");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Введите пароль");
        CheckBox saveCredBox = new CheckBox("Сохранить");

        grid.add(new Label("Логин:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(pass, 1, 1);
        grid.add(saveCredBox, 1, 3);

        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        pass.textProperty().addListener((observable, oldValue, newValue) -> loginButton.setDisable(oldValue.trim().isEmpty()));

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
//            todo Добавить проверку на успешное подключение к БД с выбранным логином и паролем
            if (saveCredBox.isSelected()) saveProgramSettings();
        });
        return false;
    }

    private int checkIncidentNumber(String incident) throws SQLException {
        Connection con;
        ResultSet rs;
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

    private String getFilename(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    private String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.'), fileName.length());
    }

    private int saveBLOBDataToFiles(String incident) throws SQLException, FileNotFoundException {
        File myPath;
        int filesCount = 0;
        String sqlQuery = "SELECT ole_filename, ole_item FROM ole_items WHERE aux_source_id = 1%s;";
        sqlQuery = String.format(sqlQuery, incident);
        ResultSet rs = executeSQLQuery(getDBConnection(login, password), sqlQuery);
        while (rs != null && rs.next()) {
            filesCount++;
            try {
                Blob blob = rs.getBlob("ole_item");
                InputStream inputStream = blob.getBinaryStream();
                if (useIncidentNumAsFolder.isSelected()) {
//                  создаем папку для выгрузки вложений
                    myPath = new File(saveDir + incident + "\\");
                    makePath(myPath);
//                  проверяем есть ли уже такой файл
                    myPath = new File(saveDir + incident + "\\" + "[" + incident + "] " + rs.getString("ole_filename"));
                    int i = 1;
                    while (myPath.exists()) {
                        myPath = new File(saveDir + incident + "\\" + "[" + incident + "] " + getFilename(rs.getString("ole_filename")) + "_" + i + "_" + getFileExt(rs.getString("ole_filename")));
                        i++;
                    }
                } else {
//                  создаем папку для выгрузки вложений
                    myPath = new File(saveDir);
                    makePath(myPath);
//                  проверяем есть ли уже такой файл
                    myPath = new File(saveDir + "[" + incident + "] " + rs.getString("ole_filename"));
                    int i = 1;
                    while (myPath.exists()) {
                        myPath = new File(saveDir + "[" + incident + "] " + getFilename(rs.getString("ole_filename")) + "_" + i + "_" + getFileExt(rs.getString("ole_filename")));
                        i++;
                    }
                }
                OutputStream outputStream = new FileOutputStream(myPath, false);

                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                Platform.runLater(() -> {
                    Alert viewError = new Alert(Alert.AlertType.ERROR);
                    Stage viewErrorStage = (Stage) viewError.getDialogPane().getScene().getWindow();
                    viewErrorStage.setAlwaysOnTop(true);
                    viewError.initModality(Modality.APPLICATION_MODAL);
                    viewErrorStage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));

                    viewError.setTitle("Ошибка");
                    viewError.setHeaderText(null);
                    viewError.setContentText("При создании файла произошла ошибка. Нажмите OK для повторной попытки записи.");
                    viewError.showAndWait();
                });
                return -1;
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
            saveProgramSettings();
            Alert viewError = new Alert(Alert.AlertType.ERROR);

            Stage viewErrorStage = (Stage) viewError.getDialogPane().getScene().getWindow();
            viewErrorStage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));
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

    public void changePath() {
        stopBlinking();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(saveDir));
        directoryChooser.setTitle("Выберите папку для сохранения выгружаемых вложений");
        File selectedDirectory = directoryChooser.showDialog(null);
        saveDir = selectedDirectory.getAbsolutePath() + "\\";
        pathTextField.setText(saveDir);
        saveProgramSettings();
    }

    private byte[] encrypt(String text, String keyWord) {
        byte[] arr = text.getBytes();
        byte[] keyarr = keyWord.getBytes();
        byte[] result = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = (byte) (arr[i] ^ keyarr[i % keyarr.length]);
        }
        return result;
    }

    private String decrypt(byte[] text, String keyWord) {
        byte[] result = new byte[text.length];
        byte[] keyarr = keyWord.getBytes();
        for (int i = 0; i < text.length; i++) {
            result[i] = (byte) (text[i] ^ keyarr[i % keyarr.length]);
        }
        return new String(result);
    }

    public void saveOption() {
        saveProgramSettings();
    }
}