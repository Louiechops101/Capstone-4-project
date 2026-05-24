package com.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javax.sound.sampled.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.util.ArrayList;

public class Main extends Application
{
    private Circle bird;
    private double velocity = 0;

    private ImageView deathGif;

    private int score = 0;

    private Label scoreLabel;
    private ListView<String> highScores;

    private final String FILE_NAME = "score.txt";

    private int frameCounter = 0;
    private AnimationTimer timer;

    private boolean gameStarted = false;
    private boolean isGameOver = false;

    private double pipeSpeed = 2.0;
    private double gapSize = 160;

    private int spawnRate = 180;
    private int pipesPassed = 0;

    private Pane gamePane;

   
    class PipePair
    {
        Rectangle top;
        Rectangle bottom;

        double baseY;
        double offset = 0;
        boolean moving;

        PipePair(Rectangle top, Rectangle bottom, double baseY, boolean moving)
        {
            this.top = top;
            this.bottom = bottom;
            this.baseY = baseY;
            this.moving = moving;
        }
    }

    private ArrayList<PipePair> pipes = new ArrayList<>();

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage)
    {
        gamePane = new Pane();

        
        gamePane.setMinHeight(400);
        gamePane.setPrefHeight(400);
        gamePane.setMaxHeight(400);

        
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(gamePane.widthProperty());
        clip.heightProperty().bind(gamePane.heightProperty());
        gamePane.setClip(clip);

        bird = new Circle(50, 200, 15);

        scoreLabel = new Label("Score: 0");

        Button startButton = new Button("Start Game");
        Button resetButton = new Button("Reset Leaderboard");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter Name");

        highScores = new ListView<>();
        highScores.setPrefHeight(150);

        Label leaderboardTitle = new Label("Leaderboard");

        loadScores();

        startButton.setOnAction(e -> startGame(gamePane, nameField.getText()));
        resetButton.setOnAction(e -> clearScores());

        VBox root = new VBox(10,
                nameField,
                startButton,
                resetButton,
                scoreLabel,
                leaderboardTitle,
                highScores,
                gamePane
        );

        VBox.setVgrow(gamePane, Priority.ALWAYS);

        Scene scene = new Scene(root, 500, 700);

        gamePane.setFocusTraversable(true);

        gamePane.setOnKeyPressed(e -> {
            if (!gameStarted) gameStarted = true;
            velocity = -5;
        });

        gamePane.setOnMouseClicked(e -> {
            if (!gameStarted) gameStarted = true;
            velocity = -5;
        });

        stage.setScene(scene);
        stage.setTitle("Flappy Bird FX");
        stage.show();
    }

    private void startGame(Pane pane, String playerName)
    {
        if (timer != null)
        {
            timer.stop();
        }

        gameStarted = false;
        isGameOver = false;

        pane.getChildren().clear();
        pipes.clear();

        bird.setCenterY(200);
        velocity = 0;

        pane.getChildren().add(bird);

        score = 0;
        frameCounter = 0;
        pipesPassed = 0;

        pipeSpeed = 2.0;
        gapSize = 160;
        spawnRate = 180;

        pane.requestFocus();

        timer = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                update(pane, playerName);
            }
        };

        timer.start();
    }

    private void update(Pane pane, String playerName)
    {
        frameCounter++;

        if (gameStarted)
        {
            velocity += 0.17;
            bird.setCenterY(bird.getCenterY() + velocity);
        }

        double paneHeight = pane.getHeight();
        double paneWidth = pane.getWidth();

        // SPAWN
        if (frameCounter % spawnRate == 0)
        {
            double gapStart = 50 + Math.random() * (paneHeight - gapSize - 100);

            Rectangle topPipe = new Rectangle(paneWidth, 0, 40, gapStart);

            Rectangle bottomPipe = new Rectangle(
                    paneWidth,
                    gapStart + gapSize,
                    40,
                    paneHeight - (gapStart + gapSize)
            );

            boolean move = score >= 20 && Math.random() < 0.3;

            PipePair pair = new PipePair(topPipe, bottomPipe, gapStart, move);

            pipes.add(pair);
            pane.getChildren().addAll(topPipe, bottomPipe);
        }

       
        for (PipePair pair : pipes)
        {
            pair.top.setX(pair.top.getX() - pipeSpeed);
            pair.bottom.setX(pair.bottom.getX() - pipeSpeed);

            if (pair.moving)
            {
                pair.offset += 0.03;

                double newY = pair.baseY + Math.sin(pair.offset) * 15;

                pair.top.setY(newY - pair.top.getHeight());
                pair.bottom.setY(newY + gapSize);
            }

            if (pair.top.getBoundsInParent().intersects(bird.getBoundsInParent()) ||
                pair.bottom.getBoundsInParent().intersects(bird.getBoundsInParent()))
            {
                gameOver(playerName);
                return;
            }

            if (pair.top.getX() + pair.top.getWidth() < bird.getCenterX())
            {
                if (pair.top.getProperties().get("scored") == null)
                {
                    pair.top.getProperties().put("scored", true);

                    pipesPassed++;

                    if (pipesPassed % 1 == 0)
                    {
                        score++;
                        scoreLabel.setText("Score: " + score);
                    }
                }
            }
        }

        
        pipes.removeIf(pair -> {
            boolean remove = pair.top.getX() < -50;

            if (remove)
            {
                pane.getChildren().removeAll(pair.top, pair.bottom);
            }

            return remove;
        });

        
        if (bird.getCenterY() > paneHeight || bird.getCenterY() < 0)
        {
            gameOver(playerName);
        }
    }

    private void gameOver(String playerName)
    {
        playDeathSound();

        showDeathGif();
        playDeathSound();

        if (isGameOver) return;
        isGameOver = true;

        if (timer != null)
        {
            timer.stop();
        }

        if (playerName != null && !playerName.trim().isEmpty())
        {
            saveScore(playerName, score);
        }

        loadScores();
        scoreLabel.setText("Game Over! Score: " + score);
    }

    private void saveScore(String name, int newScore)
    {
        ArrayList<String> scores = new ArrayList<>();
        boolean playerExists = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split(": ");
                String existingName = parts[0];
                int existingScore = Integer.parseInt(parts[1]);

                if (existingName.equalsIgnoreCase(name))
                {
                    playerExists = true;

                    if (newScore > existingScore)
                    {
                        scores.add(name + ": " + newScore);
                    }
                    else
                    {
                        scores.add(line);
                    }
                }
                else
                {
                    scores.add(line);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("No existing scores.");
        }

        if (!playerExists)
        {
            scores.add(name + ": " + newScore);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME)))
        {
            for (String s : scores)
            {
                writer.println(s);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void clearScores()
    {
        try (PrintWriter writer = new PrintWriter(FILE_NAME))
        {
            writer.print("");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        highScores.getItems().clear();
    }

    private void loadScores()
    {
        ArrayList<String> scores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                scores.add(line);
            }
        }
        catch (IOException e)
        {
            System.out.println("No scores yet.");
        }

        scores.sort((a, b) ->
        {
            int sa = Integer.parseInt(a.split(": ")[1]);
            int sb = Integer.parseInt(b.split(": ")[1]);
            return sb - sa;
        });

        highScores.getItems().setAll(scores);
    }

    private void playDeathSound()
    {
        try
        {
            File file = new File("death.wav");
           System.out.println("Looking for: " + file.getAbsolutePath());

            AudioInputStream audio = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();
        }
        catch (Exception e)
        {
            System.out.println("Sound error: " + e.getMessage());
        }
    }

    private void showDeathGif()
    {
        try
        {
           Image gif = new Image("file:death.gif");

            deathGif = new ImageView(gif);
            deathGif.setFitWidth(200);
            deathGif.setFitHeight(200);

            deathGif.setX(bird.getCenterX() - 100);
            deathGif.setY(bird.getCenterY() - 100);

            gamePane.getChildren().add(deathGif);
            deathGif.toFront();

       
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));

            delay.setOnFinished(e -> gamePane.getChildren().remove(deathGif));
            delay.play();
        }
        catch (Exception e)
        {
            System.out.println("GIF error: " + e.getMessage());
        }
    }
}