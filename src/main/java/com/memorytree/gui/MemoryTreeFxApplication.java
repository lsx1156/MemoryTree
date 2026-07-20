/*
 * Copyright 2026 lsx1156
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.memorytree.gui;

import com.memorytree.MemoryTreeApplication;
import com.memorytree.config.GlobalExceptionHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
        GlobalExceptionHandler exceptionHandler = springContext.getBean(GlobalExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Platform.runLater(() -> {
                String errorMsg = exceptionHandler.handle(throwable);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setHeaderText(null);
                alert.setContentText(errorMsg);
                alert.showAndWait();
            });
        });

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
        loader.setControllerFactory(springContext::getBean);
        
        Parent root = loader.load();
        mainController = loader.getController();
        
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        primaryStage.setTitle("记忆树认知推理平台V1.0");
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
