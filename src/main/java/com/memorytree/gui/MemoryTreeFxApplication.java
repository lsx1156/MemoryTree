package com.memorytree.gui;

import com.memorytree.MemoryTreeApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;

public class MemoryTreeFxApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private MainController mainController;

    @Override
    public void init() throws Exception {
        springContext = SpringApplication.run(MemoryTreeApplication.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
        loader.setControllerFactory(springContext::getBean);
        
        Parent root = loader.load();
        mainController = loader.getController();
        
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        primaryStage.setTitle("MemoryTree - 记忆树AI框架");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        InputStream iconStream = getClass().getResourceAsStream("/icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        }
        
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            if (springContext != null) {
                springContext.close();
            }
        });
        
        primaryStage.show();
        
        mainController.initializeAfterStageShow();
    }

    @Override
    public void stop() throws Exception {
        if (springContext != null) {
            springContext.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        System.setProperty("prism.allowhidpi", "true");
        System.setProperty("prism.forcehidpi", "true");
        System.setProperty("javafx.allowHighDpi", "true");
        launch(args);
    }
}
