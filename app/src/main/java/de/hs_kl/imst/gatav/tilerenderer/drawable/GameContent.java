package de.hs_kl.imst.gatav.tilerenderer.drawable;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;

import androidx.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import de.hs_kl.imst.gatav.tilerenderer.util.Direction;

public class GameContent implements Drawable {

    public Thread timeThread;
    public volatile boolean runningTimeThread = false; // access to elementary data types (not double or long) are atomic and should be volatile to synchronize content
    private volatile double elapsedTime = 0.0;
    synchronized private void resetElapsedTime() { elapsedTime = 0.0; }
    synchronized private double getElapsedTime() { return elapsedTime; }
    synchronized private void increaseElapsedTime(double increment) { elapsedTime += increment; }

    /**
     * Breite und Höhe des Spielfeldes in Pixel
     */
    private int gameWidth = -1;
    private int gameHeight = -1;

    public int getGameWidth() { return gameWidth; }
    public int getGameHeight() { return gameHeight; }

    /**
     * Beinhaltet alle Tiles, die das Spielfeld als solches darstellen. Diese werden als erstes
     * gezeichnet und bilden somit die unterste Ebene.
     */
    private TileGraphics[][] tiles;         // [zeilen][spalten]

    /**
     * Beinhaltet Referenzen auf alle dynamischen Kacheln, deren {@link Drawable#update(float)} Methode
     * aufgerufen werden muss. Damit lassen sich Kachel-Animationen durchführen.
     */
    private ArrayList<TileGraphics> dynamicTiles = new ArrayList<>();

    /**
     * Beinhaltet alle Ziele. Diese werden als zweites und somit über die in {@link GameContent#tiles}
     * definierten Elemente gezeichnet.
     */
    private TileGraphics[][] targetTiles;   // [zeilen][spalten]

    /**
     * Bevor die Explosion auf das targetTile gezeichnet wird, wird das Item das davor drin war gemerkt
     */
    private TileGraphics[][] targetTilesBeforeExplosion;

    /**
     * Beinhaltet Referenzen auf alle Ziele (studenten)
     */
    private ArrayList<Student> studentTargets = new ArrayList<>();
    public boolean isStudentsAllDead() { return studentTargets.size() > 0 ? false : true; }

    /**
     * Beinhaltet Referenzen auf Kacheln (hier alle vom Typ {@link Floor}), auf welchen ein Ziel
     * erscheinen kann.
     */
    private ArrayList<TileGraphics> possibleTargets = new ArrayList<>();

    private Map<Exam, Double> detonationTimes = new HashMap<>();
    private Map<Explosion, Double> explosionTimes = new HashMap<>();
    private Map<Grade, Double> gradeTimes = new HashMap<>();

    private boolean isPlayerDead = false;
    public boolean isPlayerDead() { return isPlayerDead; }

    /**
     * Beinhaltet Referenz auf Spieler, der bewegt wird.
     */
    private Player player = null;

    /**
     * Wird in {@link GameContent#movePlayer(Direction)} verwendet, um dem Game Thread
     * die Bewegungsrichtung des Players zu übergeben.
     * Wird vom Game Thread erst auf IDLE zurückgesetzt, sobald die Animation abgeschlossen ist
     */
    private volatile Direction playerDirection = Direction.IDLE;
    synchronized public void resetPlayerDirection() { playerDirection = Direction.IDLE;}
    synchronized public boolean isPlayerDirectionIDLE() { return playerDirection == Direction.IDLE; }
    synchronized public void setPlayerDirection(Direction newDirection) { playerDirection = newDirection;}
    synchronized public Direction getPlayerDirection() { return playerDirection; }

    private volatile boolean plantBomb = false;
    synchronized public void resetPlantBomb() { plantBomb = false; }
    synchronized public void activatePlantBomb() { plantBomb = true; }

    /**
     * Zufallszahlengenerator für zufällige Dinge (Upgrade spawn aus Kiste oder nicht)
     */
    private Random random = new Random();

    private Context context;

    /**
     * {@link AssetManager} über den wir unsere Leveldaten beziehen
     */
    private AssetManager assetManager;

    /**
     * Name des Levels
     */
    private String levelName;

    /**
     * Gamesound
     */
    private GameSound gamesound;

    /**
     * @param context context :)>
     * @param levelName Name des zu ladenden Levels
     */
    public GameContent(Context context, String levelName) {
        this.context = context;
        this.assetManager = context.getAssets();
        this.levelName = levelName;
        //sound
        gamesound = new GameSound(context);
        // Level laden mit Wall (W), Floor (F) und Player (P)
        // Target wird im geladenen Level zum Schluss zusätzlich gesetzt
        try {
            loadLevel(assetManager.open(String.format("levels/%s.txt", levelName)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Player ist animiert und muss deshalb updates auf seine Position erfahren
        dynamicTiles.add(player);
    }

    /**
     * Überprüfung der Möglichkeit einer Verschiebung des Players in eine vorgegebene Richtung
     * Geprüft wird auf Spielfeldrand und Hindernisse.
     * Falls das zulässige Zielfeld ein Target ist, wird dieses konsumiert und ein neues Target gesetzt.
     * Dann wird die Bewegung des Players durchgeführt bzw. angestoßen (Animation)
     *
     * @param direction Richtung in die der Player bewegt werden soll
     * @return true falls Zug erfolgreich durchgeführt bzw. angestoßen, false falls Zug nicht durchgeführt
     */
    public boolean movePlayer(Direction direction) {
        // Erster Schritt: Basierend auf Zugrichtung die Zielposition bestimmen
        int newX = -1;
        int newY = -1;
        switch(direction) {
            case UP: newX = player.getX(); newY = player.getY() - 1; break;
            case DOWN: newX = player.getX(); newY = player.getY() + 1; break;
            case RIGHT: newX = player.getX() + 1; newY = player.getY(); break;
            case LEFT: newX = player.getX() - 1; newY = player.getY(); break;
        }
        if ((!(newX >= 0 && newX < gameWidth && newY >= 0 && newY < gameHeight)))
            throw new AssertionError("Spieler wurde außerhalb des Spielfeldes bewegt. Loch im Level?");

        // Zweiter Schritt: Prüfen ob Spieler sich an Zielposition bewegen kann (Zielkachel.isPassable())
        TileGraphics targetTile = tiles[newY][newX];
        TileGraphics targetTargetTile = targetTiles[newY][newX];
        if(targetTargetTile != null && !targetTargetTile.isPassable())
            return false;
        if(targetTile == null || !targetTile.isPassable())
            return false;

        // Dritter Schritt: Spieler verschieben bzw. Verschieben starten.
        // Hinterher steht der Spieler logisch bereits auf der neuen Position
        player.move(newX, newY);

        // Vierter Schritt: Check ob Zielkachel upgrade ist, wenn ja, increment
        if(targetTiles[newY][newX] != null && targetTiles[newY][newX] instanceof Upgrade) {
            player.incrementExplosionRadius();
            targetTiles[newY][newX] = null;
        }

        return true;
    }

    public void placeExam() {
        int x = player.getX();
        int y = player.getY();

        if(targetTiles[y][x] instanceof Exam) {
            //do nothing
        }
        else {
            Exam exam = new Exam(x, y, getGraphicsStream(levelName, "exam"));
            targetTiles[exam.getY()][exam.getX()] = exam;
            detonationTimes.put(exam, getElapsedTime()+exam.getDetonationTime());
        }
    }

    private void checkAndTriggerExams() {
        ArrayList<Exam> toRemove = new ArrayList<>(); //avoid ConcurrentModificationException
        if(detonationTimes != null && detonationTimes.size() > 0)
        {
            for (Map.Entry<Exam, Double> entry : detonationTimes.entrySet()) {
                if(getElapsedTime() >= entry.getValue()) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        if(toRemove.size() > 0) {
            for(Exam exam : toRemove) {
                detonationTimes.remove(exam);
                targetTiles[exam.getY()][exam.getX()] = null;
                int explosionRadius = player.getExplosionRadius();
                int startX = exam.getX();
                int startY = exam.getY();
                int lowX = exam.getX() - explosionRadius;
                int highX = exam.getX() + explosionRadius;
                int lowY = exam.getY() - explosionRadius;
                int highY = exam.getY() + explosionRadius;

                //Set new borders if out of map, add 1 because there is a wall around the map
                if(lowX <= 0) lowX = 1;
                if(highX > targetTiles[exam.getY()].length - 1) highX = targetTiles[exam.getY()].length - 2;
                if(lowY <= 0) lowY = 1;
                if(highY > targetTiles.length - 1) highY = targetTiles.length - 2;

                for(int x = startX; x <= highX; ++x) {
                    if(!isTileWall(exam.getY(), x)) {
                        if (isTileDrawable(exam.getY(), x)) {
                            handleExplosionOnTargetTile(exam.getY(), x);
                        }
                    }
                    else break; //Break the Loop if tile is a wall
                }

                for(int x = startX; x >= lowX; --x) {
                    if(!isTileWall(exam.getY(), x)) {
                        if (isTileDrawable(exam.getY(), x)) {
                            handleExplosionOnTargetTile(exam.getY(), x);
                        }
                    }
                    else break;
                }

                for(int y = startY; y <= highY; ++y) {
                    if(!isTileWall(y, exam.getX())) {
                        if (isTileDrawable(y, exam.getX())) {
                            handleExplosionOnTargetTile(y, exam.getX());
                        }
                    }
                    else break;
                }

                for(int y = startY; y >= lowY; --y) {
                    if(!isTileWall(y, exam.getX())) {
                        if (isTileDrawable(y, exam.getX())) {
                            handleExplosionOnTargetTile(y, exam.getX());
                        }
                    }
                    else break;
                }
            }
        }
    }

    private void handleExplosionOnTargetTile(int y, int x)
    {
        //Dont draw explosion on existing exam
        if(targetTiles[y][x] instanceof Exam) {
            //Do nothing
        }
        else {
            targetTilesBeforeExplosion[y][x] = targetTiles[y][x];
            Explosion exp = new Explosion(x, y, getGraphicsStream(levelName, "explosion"));
            targetTiles[y][x] = exp;
            explosionTimes.put(exp, getElapsedTime() + exp.getExplosionTime());
            if (player != null && samePosition(exp, player)) {
                dynamicTiles.remove(player);
                player = null;
                gamesound.playOhNoSound();
            }
            //sound
            gamesound.playExplosionSound();

            ArrayList<Student> toRemove = new ArrayList<>();
            for (Student st : studentTargets) {
                if (st != null && samePosition(exp, st)) {
                    //new grade dickt wenn der student tot ist
                    Grade grade = new Grade(x, y, getGraphicsStream(levelName, "grade"));
                    targetTiles[y][x] = grade;
                    gradeTimes.put(grade, getElapsedTime() + grade.getRemoveTime());
                    toRemove.add(st);
                }
            }
            for (Student st : toRemove) {
                if (st != null && samePosition(exp, st)) {
                    studentTargets.remove(st);
                    dynamicTiles.remove(st);
                    gamesound.playMistSound();
                }
            }
        }
    }

    private void checkAndRemovePlayer() {
        for(Student st : studentTargets) {
            if(player != null && samePosition(st, player)) {
                dynamicTiles.remove(player);
                player = null;
            }
            if(player == null) {
                isPlayerDead = true;
                gamesound.playOhNoSound();
            }
        }
    }

    private boolean isTileWall(int y, int x) {
        if(tiles[y][x] instanceof Wall)
            return true;
        return false;
    }

    private boolean isTileDrawable(int y, int x) {
        if(tiles[y][x] instanceof Floor ||
            targetTiles[y][x] instanceof Chest ||
            targetTiles[y][x] instanceof Student ||
            targetTiles[y][x] instanceof Upgrade
        ) {
            return true;
        }
        return false;
    }

    private void checkAndRemoveExplosions() {
        ArrayList<Explosion> toRemove = new ArrayList<>(); //avoid ConcurrentModificationException
        if(explosionTimes != null && explosionTimes.size() > 0)
        {
            for (Map.Entry<Explosion, Double> entry : explosionTimes.entrySet()) {
                if(getElapsedTime() >= entry.getValue()) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        if(toRemove.size() > 0) {

            for(Explosion exp : toRemove) {
                int x = exp.getX();
                int y = exp.getY();
                explosionTimes.remove(exp);
                targetTiles[y][x] = null;

                if(targetTilesBeforeExplosion[y][x] instanceof Chest) {
                    double rand = random.nextDouble();
                    if(rand < Upgrade.getDropChance()) {
                        targetTiles[y][x] = new Upgrade(x, y, getGraphicsStream(levelName, "upgrade"));
                        targetTilesBeforeExplosion[y][x] = null;
                        gamesound.playUpgradeSound();
                    }
                }

                if(player == null) {
                    isPlayerDead = true;

                }
            }
        }
    }

    private void checkAndRemoveGrades() {
        ArrayList<Grade> toRemove = new ArrayList<>(); //avoid ConcurrentModificationException
        if(gradeTimes != null && gradeTimes.size() > 0)
        {
            for (Map.Entry<Grade, Double> entry : gradeTimes.entrySet()) {
                if(getElapsedTime() >= entry.getValue()) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        if(toRemove.size() > 0) {
            for(Grade grade : toRemove) {
                int x = grade.getX();
                int y = grade.getY();
                gradeTimes.remove(grade);
                targetTiles[y][x] = null;
                targetTilesBeforeExplosion[y][x] = null;
            }
        }
    }

    /**
     * Spielinhalt zeichnen
     * @param canvas Zeichenfläche, auf die zu Zeichnen ist
     */
    @Override
    public void draw(Canvas canvas) {
        // Erste Ebene zeichnen (Wände und Boden)
        for (int yIndex = 0; yIndex < tiles.length; yIndex++)
            for (int xIndex = 0; xIndex < tiles[yIndex].length; xIndex++) {
                if(tiles[yIndex][xIndex] == null) continue;
                tiles[yIndex][xIndex].draw(canvas);
            }
        // Zweite Ebene zeichnen
        for (int yIndex = 0; yIndex < targetTiles.length; yIndex++)
            for (int xIndex = 0; xIndex < targetTiles[yIndex].length; xIndex++) {
                if(targetTiles[yIndex][xIndex] == null) continue;
                targetTiles[yIndex][xIndex].draw(canvas);
            }

        for(Student st :studentTargets ) {
            if (st != null)
                st.draw(canvas);
        }

        if(player != null)
            player.draw(canvas);
    }

    /**
     * Spielinhalt aktualisieren (hier Player und Animation dynamischer Kacheln)
     * @param fracsec Teil einer Sekunde, der seit dem letzten Update des gesamten Spielzustandes vergangen ist
     */
    @Override
    public void update(float fracsec) {
        if(plantBomb)
        {
            placeExam();
            resetPlantBomb();
        }

        checkAndTriggerExams();
        checkAndRemoveExplosions();
        checkAndRemoveGrades();
        checkAndRemovePlayer();
        // 1. Schritt: Auf mögliche Player Bewegung prüfen und ggf. durchführen/anstoßen
        // vorhandenen Player Move einmalig ausführen bzw. anstoßen, falls
        // PlayerDirection nicht IDLE ist und Player aktuell nicht in einer Animation
        if(player != null && !isPlayerDirectionIDLE() && !player.isMoving())
            movePlayer(getPlayerDirection());

        // 2. Schritt: Updates bei allen dynamischen Kacheln durchführen (auch Player)
        for(TileGraphics dynamicTile : dynamicTiles)
            dynamicTile.update(fracsec);

        // 3. Schritt: Animationen auf Ende überprüfen und ggf. wieder freischalten
        // Player Move fertig ausgeführt => Sperre für neues Player Event freischalten
        if(player != null && !player.isMoving())
            resetPlayerDirection();
        // Animation des dynamischen Ziels abgeschlossen
        for(Student st :studentTargets ) {
            if (st != null && !st.isMoving()) {
                 MoveDynamicTarget(st);
            }
        }
    }

    /**
     * Level aus Stream laden und Datenstrukturen entsprechend initialisieren
     * @param levelIs InputStream von welchem Leveldaten gelesen werden sollen
     * @throws IOException falls beim Laden etwas schief geht (IO Fehler, Fehler in Leveldatei)
     */
    private void loadLevel(InputStream levelIs) throws IOException {
        int i=0;
        int l=0;
        switch(levelName) {
            case "level1": l=3; break;
            case "level2": l=4; break;
            case "level3": l=5; break;
        }

        // Erster Schritt: Leveldatei zeilenweise lesen und Inhalt zwischenspeichern. Zudem ermitteln, wie breit der Level maximal ist.
        // Spielfeldgröße ermitteln
        ArrayList<String> levelLines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(levelIs));
        int maxLineLength = 0;
        String currentLine;
        while((currentLine = br.readLine()) != null) {
            maxLineLength = Math.max(maxLineLength, currentLine.length());
            levelLines.add(currentLine);
        }
        br.close();
        gameWidth = (int)(maxLineLength * TileGraphics.getTileSize());
        gameHeight = (int)(levelLines.size() * TileGraphics.getTileSize());

        // Zweiter Schritt: basierend auf dem Inhalt der Leveldatei die Datenstrukturen befüllen
        tiles = new TileGraphics[levelLines.size()][];
        targetTiles = new TileGraphics[levelLines.size()][];
        targetTilesBeforeExplosion = new TileGraphics[levelLines.size()][];

        for(int yIndex = 0; yIndex < levelLines.size(); yIndex++) {
            tiles[yIndex] = new TileGraphics[maxLineLength];
            targetTiles[yIndex] = new TileGraphics[maxLineLength];
            targetTilesBeforeExplosion[yIndex] = new TileGraphics[maxLineLength];
            String line = levelLines.get(yIndex);
            for(int xIndex = 0; xIndex < maxLineLength && xIndex < line.length(); xIndex++) {
                TileGraphics tg = getTileByCharacter(line.charAt(xIndex), xIndex, yIndex);
                // Floor Tiles sind gleichzeitig Kacheln, auf denen Ziele erscheinen können
                if(tg instanceof Floor) {
                    possibleTargets.add(tg);
                    tiles[yIndex][xIndex] = tg;
                } else if(tg instanceof Player) {   // auch auf der Player Kachel können Ziele erscheinen, zusätzlich ist sie eine Floor Kachel
                    tiles[yIndex][xIndex] = getTileByCharacter('f', xIndex, yIndex);
                    possibleTargets.add(tiles[yIndex][xIndex]);
                    if (player != null)
                        throw new IOException("Invalid level file, contains more than one player!");
                    player = (Player) tg;
                }
                else if(tg instanceof Student || tg instanceof Chest) {
                    possibleTargets.add(tg);
                    tiles[yIndex][xIndex] = getTileByCharacter('f', xIndex, yIndex);
                    targetTiles[yIndex][xIndex] = tg;
                }
                else {
                    tiles[yIndex][xIndex] = tg;
                }
                if(!(tg instanceof Player)&&!(isTileWall(yIndex,xIndex))&&i<l) {
                    Student st = new Student(xIndex, yIndex, getGraphicsStream(levelName, "student"));
                    studentTargets.add(st);
                    dynamicTiles.add(st);
                    i+=1;
                }
            }
        }
    }

    /**
     * Erzeugt ein dynamisches Ziel, sofern das Ziel passable ist.
     *
     *
     * Ansonsten befindet sich das dynamische Ziel logisch "über" der Ebene der anderen Ziele.
     * Nach erfolgreichem Anlegen wird der Move direkt initiiert.
     * @return dynamisches Ziel, kann null sein, falls es von einer gewählten Source nicht erzeugt werden konnte
     */

    @Nullable
    public void MoveDynamicTarget(Student st) {
        // Destination bestimmen, falls möglich, ansonsten Abbruch
        // 0 left, 1 right, 2 up, 3 down
        ArrayList<Integer> dl = new ArrayList<Integer>();
        dl.add(0);
        dl.add(1);
        dl.add(2);
        dl.add(3);
        Collections.shuffle(dl);

        TileGraphics destinationTile = null;
        int newX = -1, newY = -1;
        // alle vier Richtungen zufällig durchgehen, bis die erste passt oder eben keine

        for (int i = 0; i < 4; i++) {
            switch (dl.get(i)) {
                case 0:
                    newX = st.getX() - 1;
                    newY = st.getY();
                    break;
                case 1:
                    newX = st.getX() + 1;
                    newY = st.getY();
                    break;
                case 2:
                    newX = st.getX();
                    newY = st.getY() - 1;
                    break;
                case 3:
                    newX = st.getX();
                    newY = st.getY() + 1;
                    break;
            }
            if ((!(newX >= 0 && newX < gameWidth && newY >= 0 && newY < gameHeight)))
                continue;
            destinationTile = tiles[newY][newX];
            if (destinationTile == null || !destinationTile.isPassable()) {
                destinationTile = null;
                continue;
            }
            break;
        }

        if (destinationTile == null)
            return;

        st.move(newX, newY);
        st.setSpeed(0.5f);
    }

    /**
     * Prüft ob zwei Kacheln auf den gleichen Koordinaten liegen
     * @param a erste Kachel
     * @param b zweite Kachel
     * @return true wenn Position gleich, andernfalls false
     */
    private boolean samePosition(TileGraphics a, TileGraphics b) {
        if(a.getX() == b.getX() && a.getY() == b.getY())
            return true;
        return false;
    }

    /**
     * Besorgt Inputstream einer Grafikdatei eines bestimmten Levels aus den Assets
     * @param levelName     Levelname
     * @param graphicsName  Grafikname
     * @return Inputstream
     */
    private InputStream getGraphicsStream(String levelName, String graphicsName) {
        try {
            return assetManager.open("levels/" + levelName + "/" + graphicsName + ".png");
        }catch(IOException e){
            try {
                return assetManager.open("levels/default/" + graphicsName + ".png");
            }catch(IOException e2){
                return null;
            }
        }
    }

    public  void startTimeThread() {
        if(runningTimeThread) return;
        runningTimeThread = true;
        resetElapsedTime();
        timeThread = new Thread(new Runnable() {
            public void run() {
                while (runningTimeThread) {
                    increaseElapsedTime(0.01);

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        runningTimeThread=false;
                    }
                }
            }});
        timeThread.start();
    }

    @Nullable
    private TileGraphics getTileByCharacter(char c, int xIndex, int yIndex) {
        switch(c) {
            case 'w':
            case 'W': return new Wall(xIndex, yIndex, getGraphicsStream(levelName, "wall"));
            case 'f':
            case 'F': return new Floor(xIndex, yIndex, null);
            case 'p':
            case 'P': return new Player(xIndex, yIndex, getGraphicsStream(levelName, "professor"));
            case 'c':
            case 'C': return new Chest(xIndex, yIndex, getGraphicsStream(levelName, "chest"));
            case 'g':
            case 'G': return new Grade(xIndex, yIndex, getGraphicsStream(levelName, "grade"));
            case 'e':
            case 'E': return new Exam(xIndex, yIndex, getGraphicsStream(levelName, "exam"));
            case 's':
            case 'S': return new Student(xIndex, yIndex, getGraphicsStream(levelName, "student"));
            case 'u':
            case 'U': return new Upgrade(xIndex, yIndex, getGraphicsStream(levelName, "upgrade"));
            case 'x':
            case 'X': return new Explosion(xIndex, yIndex, getGraphicsStream(levelName, "explosion"));
        }
        return null;
    }
}
