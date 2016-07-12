package assyst.attachments;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("res/LoadIncidentAttachments.fxml"));
        primaryStage.setTitle("Assyst Incident Attachments Loader (version 0.1)");
        primaryStage.setScene(new Scene(root, 300, 90));
        primaryStage.setAlwaysOnTop(true);
        primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
