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
import javafx.scene.layout.HBox;

import java.io.*;
import java.util.ArrayList;

public class Main extends Application
{

    /** Indicates whether hard mode is enabled */
    private boolean hardMode = false;

    /** Power-up object (shield) */
    private Circle powerUp;

    /** Whether the player currently has a shield */
    private boolean hasShield = false;

    /** Duration remaining for shield */
    private int shieldTimer = 0;

    /** Duration remaining for invincibility after shield hit */
    private int invincibilityTimer = 0;

    /** Background music clip */
    private Clip backgroundMusic;

    /** Player character */
    private Circle bird;

    /** Vertical velocity of the bird */
    private double velocity = 0;

    /** Death animation GIF */
    private ImageView deathGif;

    /** Current score */
    private int score = 0;

    /** Score label UI */
    private Label scoreLabel;

    /** Leaderboard UI */
    private ListView<String> highScores;

    /** File for normal mode scores */
    private final String NORMAL_FILE = "score.txt";

    /** File for hard mode scores */
    private final String HARD_FILE = "hard_score.txt";

    /** Frame counter for timing events */
    private int frameCounter = 0;

    /** Game loop timer */
    private AnimationTimer timer;

    /** Whether the game has started */
    private boolean gameStarted = false;

    /** Whether the game is over */
    private boolean isGameOver = false;

    /** Speed of pipes */
    private double pipeSpeed = 2.0;

    /** Gap size between pipes */
    private double gapSize = 160;

    /** Rate at which pipes spawn */
    private int spawnRate = 180;

    /** Number of pipes passed */
    private int pipesPassed = 0;

    /** Main game pane */
    private Pane gamePane;

    
    private Image pipeBottomImage;


     /**
     * Represents a pair of pipes (top and bottom).
     * Can optionally move vertically.
     */
    class PipePair
    {
        ImageView top;
        ImageView bottom;

        Rectangle topHitbox;
        Rectangle bottomHitbox;

        double baseY;
        double offset = 0;
        boolean moving;

    PipePair(ImageView top, ImageView bottom, Rectangle topHitbox, Rectangle bottomHitbox, double baseY, boolean moving)
    {
        this.top = top;
        this.bottom = bottom;
        this.topHitbox = topHitbox;
        this.bottomHitbox = bottomHitbox;
        this.baseY = baseY;
        this.moving = moving;
    }
}
     /** List of active pipes */
    private ArrayList<PipePair> pipes = new ArrayList<>();

    /**
     * Launches the JavaFX application.
     */
    public static void main(String[] args)
    {
        launch(args);
    }
     
    /**
     * Initializes the UI and sets up event handlers.
     *
     * @param stage Primary application stage
     */
     @Override
    public void start(Stage stage)
    {
        
        pipeBottomImage = new Image(getClass().getResource("/com/example/pipe_bottom.png").toExternalForm());

        gamePane = new Pane();
        gamePane.setPrefHeight(400);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(gamePane.widthProperty());
        clip.heightProperty().bind(gamePane.heightProperty());
        gamePane.setClip(clip);

        bird = new Circle(50, 200, 15);

        scoreLabel = new Label("Score: 0");
        scoreLabel.setId("scoreLabel");

        Button startButton = new Button("Start Game");
        Button resetButton = new Button("Reset Leaderboard");
        Button instructionsButton = new Button("How to Play");
        Button modeButton = new Button("Normal Mode");

        modeButton.setId("modeButton");
        resetButton.setId("resetButton");

        HBox row1 = new HBox(10, startButton, modeButton);
        HBox row2 = new HBox(10, instructionsButton, resetButton);

        TextField nameField = new TextField();
        nameField.setPromptText("Enter Name");

        highScores = new ListView<>();
        highScores.setPrefHeight(150);

        Label leaderboardTitle = new Label("Normal Leaderboard");

        loadScores();

        startButton.setOnAction(e -> startGame(gamePane, nameField.getText()));
        resetButton.setOnAction(e -> clearScores());
        instructionsButton.setOnAction(e -> showInstructions());

        modeButton.setOnAction(e -> {
            hardMode = !hardMode;

            if (hardMode)
            {
                modeButton.setText("Hard Mode");
                leaderboardTitle.setText("Hard Mode Leaderboard");
            }
            else
            {
                modeButton.setText("Normal Mode");
                leaderboardTitle.setText("Normal Leaderboard");
            }

            loadScores();
        });

        VBox root = new VBox(10,
                nameField,
                row1,
                row2,
                scoreLabel,
                leaderboardTitle,
                highScores,
                gamePane
        );

        VBox.setVgrow(gamePane, Priority.ALWAYS);

        Scene scene = new Scene(root, 500, 700);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        gamePane.setFocusTraversable(true);

        gamePane.setOnKeyPressed(e -> {
            if (!gameStarted) gameStarted = true;
            velocity = hardMode ? -4.5 : -5;
        });

        gamePane.setOnMouseClicked(e -> {
            if (!gameStarted) gameStarted = true;
            velocity = hardMode ? -4.5 : -5;
        });

        stage.setScene(scene);
        stage.setTitle("Flappy Bird FX");
        stage.show();
    }

    /**
     * Starts a new game session.
     *
     * @param pane Game pane
     * @param playerName Player's name for leaderboard
     */
    private void startGame(Pane pane, String playerName)
    {
        if (timer != null) timer.stop();

        stopMusic();
        playMusic();

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

        hasShield = false;
        shieldTimer = 0;
        invincibilityTimer = 0;

        if (hardMode)
        {
            pipeSpeed = 2.5;
            spawnRate = 100;
            gapSize = 120;
        }
        else
        {
            pipeSpeed = 2.0;
            spawnRate = 180;
            gapSize = 160;
        }

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


      /**
     * Updates the game each frame.
     *  Handles movement, gravity, pipe spawning, collisions, scoring, power-ups, and game-over conditions.
     *
     * @param pane the game pane where objects are displayed
     * @param playerName the current player's name
     */
    private void update(Pane pane, String playerName)
    {
        frameCounter++;

        if (gameStarted)
        {
            velocity += hardMode ? 0.25 : 0.17;
            bird.setCenterY(bird.getCenterY() + velocity);
        }

        double paneHeight = pane.getHeight();
        double paneWidth = pane.getWidth();

        // SPAWN PIPES
        if (frameCounter % spawnRate == 0)
        {
            double gapStart = 20 + Math.random() * (paneHeight - gapSize - 40);

            double pipeWidth = 80; 

            
            ImageView topPipe = new ImageView(pipeBottomImage);
            ImageView bottomPipe = new ImageView(pipeBottomImage);

      
            topPipe.setPreserveRatio(false);
            bottomPipe.setPreserveRatio(false);

           
            topPipe.setFitWidth(pipeWidth);
            bottomPipe.setFitWidth(pipeWidth);

         
            double topHeight = gapStart;
            double bottomHeight = paneHeight - (gapStart + gapSize);

           
            topPipe.setFitHeight(topHeight);
            bottomPipe.setFitHeight(bottomHeight);

         
            topPipe.setX(paneWidth);
            topPipe.setY(0);

            bottomPipe.setX(paneWidth);
            bottomPipe.setY(gapStart + gapSize);

            
            topPipe.setScaleY(-1);
            topPipe.setY(topHeight);
            topPipe.setY(0);

            
            Rectangle topHitbox = new Rectangle();
            topHitbox.widthProperty().bind(topPipe.fitWidthProperty());
            topHitbox.heightProperty().bind(topPipe.fitHeightProperty());
            topHitbox.xProperty().bind(topPipe.xProperty());
            topHitbox.yProperty().bind(topPipe.yProperty());

            Rectangle bottomHitbox = new Rectangle();
            bottomHitbox.widthProperty().bind(bottomPipe.fitWidthProperty());
            bottomHitbox.heightProperty().bind(bottomPipe.fitHeightProperty());
            bottomHitbox.xProperty().bind(bottomPipe.xProperty());
            bottomHitbox.yProperty().bind(bottomPipe.yProperty());

            boolean move = score >= 10 && Math.random() < (hardMode ? 0.6 : 0.3);

            PipePair pair = new PipePair(
                topPipe, bottomPipe,
                topHitbox, bottomHitbox,
                gapStart, move
            );

            pipes.add(pair);

            pane.getChildren().addAll(topPipe, bottomPipe);
        }

        if (powerUp == null && Math.random() < 0.001) // ~random spawn chance
        {
    
            powerUp = new Circle(10);
            powerUp.setCenterX(pane.getWidth());

            // spawn in safe vertical range
            powerUp.setCenterY(50 + Math.random() * (paneHeight - 100));

            powerUp.setStyle("-fx-fill: gold;");

            gamePane.getChildren().add(powerUp);
        }

        // POWERUP MOVEMENT
        if (powerUp != null)
        {
            powerUp.setCenterX(powerUp.getCenterX() - pipeSpeed);

            if (powerUp.getBoundsInParent().intersects(bird.getBoundsInParent()))
            {
                hasShield = true;
                shieldTimer = 220;

                pane.getChildren().remove(powerUp);
                powerUp = null;
            }

            if (powerUp != null && powerUp.getCenterX() < -20)
            {
                pane.getChildren().remove(powerUp);
                powerUp = null;
            }
        }

        // TIMERS
        if (hasShield)
        {
            shieldTimer--;
            if (shieldTimer <= 0) hasShield = false;
        }

        if (invincibilityTimer > 0)
        {
            invincibilityTimer--;
        }

        // FLASH EFFECT
        if (hasShield)
        {
            bird.setStyle("-fx-fill: cyan;");
        }
        else if (invincibilityTimer > 0)
        {
            if (invincibilityTimer % 10 < 5)
                bird.setStyle("-fx-fill: white;");
            else
                bird.setStyle("-fx-fill: cyan;");
        }
        else
        {
            bird.setStyle("");
        }

        // MOVE PIPES
        for (int i = 0; i < pipes.size(); i++)
        {
            PipePair pair = pipes.get(i);

            pair.top.setX(pair.top.getX() - pipeSpeed);
            pair.bottom.setX(pair.bottom.getX() - pipeSpeed);

            
        if (pair.moving)
        {
            pair.offset += 0.03;

            double gapY = pair.baseY + Math.sin(pair.offset) * 15;

            double topHeight = gapY;
            double bottomHeight = paneHeight - (gapY + gapSize);

  
            pair.top.setFitHeight(topHeight);
            pair.top.setY(0);

    
            pair.bottom.setFitHeight(bottomHeight);
            pair.bottom.setY(gapY + gapSize);
        }

            // COLLISION
            if (pair.topHitbox.getBoundsInParent().intersects(bird.getBoundsInParent())|| pair.bottomHitbox.getBoundsInParent().intersects(bird.getBoundsInParent()))
            {
                if (hasShield || invincibilityTimer > 0)
                {
                    hasShield = false;
                    invincibilityTimer = 120;

                    pane.getChildren().removeAll(pair.top, pair.bottom);
                    pipes.remove(i);
                    i--;
                    continue;
                }
                else
                {
                    gameOver(playerName);
                    return;
                }
            }

            // SCORE
            if (pair.top.getX() + pair.top.getFitWidth() < bird.getCenterX())
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

        // REMOVE PIPES
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

     /**
     * Handles game over state.
     * Stops the game, plays effects, and saves score.
     *
     * @param playerName Player name
     */
    private void gameOver(String playerName)
    {
        if (isGameOver) return;
        isGameOver = true;

        stopMusic();
        playDeathSound();
        showDeathGif();

        if (timer != null) timer.stop();

        if (playerName != null && !playerName.trim().isEmpty())
        {
            saveScore(playerName, score);
        }

        loadScores();
        scoreLabel.setText("Game Over! Score: " + score);
    }

    /**
     * Returns the correct score file depending on mode.
     *
     * @return File name for current mode
     */
    private String getCurrentFile()
    {
        return hardMode ? HARD_FILE : NORMAL_FILE;
    }


     /**
     * Saves a player's score, keeping only their highest score.
     *
     * @param name Player name
     * @param newScore Score to save
     */
    private void saveScore(String name, int newScore)
    {
        ArrayList<String> scores = new ArrayList<>();
        boolean playerExists = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(getCurrentFile())))
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
                    scores.add(name + ": " + Math.max(existingScore, newScore));
                }
                else
                {
                    scores.add(line);
                }
            }
        }
        catch (IOException ignored) {}

        if (!playerExists)
        {
            scores.add(name + ": " + newScore);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(getCurrentFile())))
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

     /**
     * Loads scores from file and updates leaderboard UI.
     */
    private void loadScores()
    {
        ArrayList<String> scores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(getCurrentFile())))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                scores.add(line);
            }
        }
        catch (IOException ignored) {}

        scores.sort((a, b) ->
            Integer.parseInt(b.split(": ")[1]) -
            Integer.parseInt(a.split(": ")[1])
        );

        highScores.getItems().setAll(scores);
    }

    /**
     * Clears all saved scores.
     */
    private void clearScores()
    {
        try (PrintWriter writer = new PrintWriter(getCurrentFile()))
        {
            writer.print("");
        }
        catch (IOException ignored) {}

        highScores.getItems().clear();
    }

    /**
     * Plays looping background music.
     */
    private void playMusic()
    {
        try
        {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File("Save your grace.wav"));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audio);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
        }
        catch (Exception e)
        {
            System.out.println("Music error: " + e.getMessage());
        }
    }
    /**
     * Stops background music if playing.
     */
    private void stopMusic()
    {
        if (backgroundMusic != null)
        {
            backgroundMusic.stop();
            backgroundMusic.close();
        }
    }

     /**
     * Plays the death sound effect.
     */
    private void playDeathSound()
    {
        try
        {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File("death.wav"));
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();
        }
        catch (Exception e)
        {
            System.out.println("Sound error: " + e.getMessage());
        }
    }
    
    /**
     * Displays the death animation GIF briefly.
     */
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

            javafx.animation.PauseTransition delay =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));

            delay.setOnFinished(e -> gamePane.getChildren().remove(deathGif));
            delay.play();
        }
        catch (Exception e)
        {
            System.out.println("GIF error: " + e.getMessage());
        }
    }

    /**
     * Displays the instructions window.
     */
    private void showInstructions()
    {
        Stage window = new Stage();
        window.setTitle("How to Play");

        Label instructions = new Label(
            "HOW TO PLAY:\n\n" +
            "- Click or press to jump\n" +
            "- Avoid pipes\n" +
            "- Score by passing pipes\n\n" +

            "POWER-UP:\n\n" +
            "- Gold circle = Shield\n" +
            "- Blocks one hit\n" +
            "- Destroys pipe + invincibility\n\n" +

            "HARD MODE:\n\n" +
            "- Faster pipes\n" +
            "- Less time to react\n" +
            "- Smaller gaps\n" +
            "- More moving obstacles\n"
        );

        instructions.setWrapText(true);

        VBox layout = new VBox(15, instructions);
        layout.setStyle("-fx-padding: 20;");

        Scene scene = new Scene(layout, 350, 300);
        window.setScene(scene);
        window.show();
    }
}