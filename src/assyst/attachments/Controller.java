package assyst.attachments;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import javax.swing.JOptionPane;
import java.io.*;
import java.sql.*;

public class Controller {

    public Button downloadButton;
    public TextField incidentNumber;
    public ProgressBar downloadBar;
    private Connection con;
    private String login = "";
    private String password = "";

    public void downloadAction() {
        final int[] fc = new int[1];
        if (login.isEmpty()) {
            login = JOptionPane.showInputDialog(null, "Введите логин от ASSYST", "Введите логин", JOptionPane.INFORMATION_MESSAGE);
        }
        if (password.isEmpty()) {
            password = JOptionPane.showInputDialog(null, "Введите пароль от ASSYST", "Введите пароль", JOptionPane.INFORMATION_MESSAGE);
        }
        if (incidentNumber.getText().length() > 0) {
            int b;
            try {
                b = Integer.parseInt(incidentNumber.getText());
            } catch (NumberFormatException e) {
                b = -1;
            }
            if ((incidentNumber.getText().length() > 6) && (b > 1000000)) {
                downloadButton.setDisable(true);
                incidentNumber.setDisable(true);
                try {
                    if (checkIncidentNumber(incidentNumber.getText())) {
                        downloadBar.setVisible(true);
                        new Thread(() -> {
                            try {
                                fc[0] = saveBLOBdataToFiles(incidentNumber.getText());
                                if (fc[0] > 0) {
                                    JOptionPane.showMessageDialog(null, "Из инцидента " + incidentNumber.getText() + " сохранено " + fc[0] + " файла/ов.", "Успешное сохранение", JOptionPane.INFORMATION_MESSAGE);
                                } else
                                    JOptionPane.showMessageDialog(null, "В инциденте " + incidentNumber.getText() + " не найдено файлов!", "Ошибка при сохранении", JOptionPane.ERROR_MESSAGE);

                            } catch (SQLException | FileNotFoundException e) {
                                JOptionPane.showMessageDialog(null, "Ай, яй! Ошибочка вышла! (" + e.getLocalizedMessage() + ")", "Ошибка при выполнении", JOptionPane.ERROR_MESSAGE);
                            }
                            Platform.runLater(() -> {
                                downloadBar.setVisible(false);
                                downloadButton.setDisable(false);
                                incidentNumber.setText("");
                                incidentNumber.setDisable(false);
                            });
                        }).start();
                    } else {
                        downloadButton.setDisable(true);
                        incidentNumber.setDisable(true);
                        JOptionPane.showMessageDialog(null, "Похоже Вы ввели некорректный номер инцидента!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else
                JOptionPane.showMessageDialog(null, "Похоже Вы ввели некорректный номер инцидента!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        } else
            JOptionPane.showMessageDialog(null, "Введите номер инцидента! (Например, 2046188 или T2041272)", "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    public void checkInput(KeyEvent event) {
/*        if (event.getEventType() == KEY_TYPED) {
            if (Objects.equals("T", event.getCharacter())) {

            }
            System.out.println("Key typed: " + incidentNumber.getText());
        }
*/
    }

    private boolean checkIncidentNumber(String incident) throws SQLException {
        String sqlQuery = "SELECT count(*) AS \"count\" FROM incident WHERE incident_ref = %s;";
        sqlQuery = String.format(sqlQuery, incident);
        ResultSet rs = executeSQLQuery(getDBConnection(login, password), sqlQuery);
        return rs != null && rs.next() && (Integer.parseInt(rs.getString("count")) > 0);
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
                File myPath = new File("C:\\TMP\\Download\\" + incident + "\\");
                myPath.mkdirs();
                OutputStream outputStream = new FileOutputStream("C:\\TMP\\Download\\" + incident + "\\" + rs.getString("ole_filename"));

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
        SQLServerDataSource ds = new SQLServerDataSource();
        ds.setServerName("10.95.1.56");
        ds.setPortNumber(1433);
        ds.setDatabaseName("mvideorus_db");
        ds.setUser(login);
        ds.setPassword(pass);
        try {
            return ds.getConnection();
        } catch (SQLServerException e) {
            JOptionPane.showMessageDialog(null, "Не удалось подключиться к БД ASSYST под логином \"" + login + "\"", "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
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
