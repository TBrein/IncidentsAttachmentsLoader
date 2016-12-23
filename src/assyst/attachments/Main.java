/*
 * Данная программа разработана Бедаревым Вячеславом специально для аналитиков L3 проекта МВИДЕО "ФОБО".
 *
 * Использование вне проекта запрещено!
 */

package assyst.attachments;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("res/LoadIncidentAttachments.fxml"));
        primaryStage.setTitle("Incident Attach's Loader");
        primaryStage.setScene(new Scene(root, 300, 150));
//        primaryStage.setAlwaysOnTop(true);
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.getIcons().add(new Image(this.getClass().getResource("res/icon32.png").toString()));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            launch(args);
        } else {
            System.out.println("Режим командной строки включен.");
            System.out.println("IAL версия 0.2");
            System.out.println("Переданный параметр: " + args[args.length - 1]);
            System.out.println("Работы окончены.");
            System.exit(0);
        }
    }
}
