package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;
import edu.princeton.cs.introcs.Stopwatch;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Engine {
    /* Feel free to change the width and height. */
    public static final int WIDTH = 80;
    public static final int HEIGHT = 30;
    /** The {@link TETile} board */
    public static final TETile[][] WORLD = new TETile[WIDTH][HEIGHT];
    /** The designated game folder */
    public static final String GAME_FOLDER = "byow/Game/";
    /** Flood status */
    boolean[][] flooded = new boolean[WIDTH][HEIGHT];
    /** Random seed */
    Random random;
    /** Check if the seed has been entered or not */
    boolean hasSeed;
    /** Position of the AVATAR */
    Point avatar;
    /** Position of the LOCKED_DOOR */
    Point door;
    /** Quit game */
    boolean quit;
    /** Loaded game */
    boolean loaded;
    /** water breaks in */
    boolean flown;
    /** Has brick */
    boolean hasBrick;
    /** Current position */
    boolean position;
    /** Taken pill */
    boolean takePill;
    /** Visible points by water */
    HashSet<Point> visible = new HashSet<>();
    /** Stopwatch for timing */
    Stopwatch timer;
    /** Switch to a different ground, re-calibrating time */
    double start;
    /** Health bar */
    int health = Constants.DEFAULT_HEALTH;
    /** Medicine position */
    Point pill;
    /** Brick position */
    Point brick;


    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() throws IOException {
        if (!hasSeed)
            displayMenu();
        while (true) {
            char c = getNextKey();
            if (hasSeed) {
                if (processChr(c))
                    visibleArea();
                    //new TERenderer().renderFrame(WORLD, health, inWater(), hasBrick);
            } else {
                processMenu(c);
                //visibleArea();
                new TERenderer().renderFrame(WORLD, health, inWater(), hasBrick);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Engine e = new Engine();
        e.interactWithKeyboard();
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                                                    //
//                                                 GAME PLAY FEATURES                                                 //
//                                                                                                                    //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean inWater() {
        return flooded[avatar.x][avatar.y] == Constants.HAS_WATER;
    }

    /**
     * Get a key from the keyboard in uppercase
     * @return The typed key
     */
    private char getNextKey() {
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                return Character.toUpperCase(StdDraw.nextKeyTyped());
            }
            // Water starts to flow
            if (timer != null && !flown) {
                if (timer.elapsedTime() == 5.0) {
                    WORLD[door.x][door.y] = Tileset.WARNING;
                    visible.add(door);
                    visibleArea();
                } else if (timer.elapsedTime() == 7.0) {
                    flown = true;
                    WORLD[door.x][door.y] = Tileset.WATER;
                    flooded[door.x][door.y] = true;
                    timer = new Stopwatch();
                    start = 0;
                    visibleArea();
                }
            }
            // Update water flown area
            else if (timer != null) {
                if (timer.elapsedTime() % 2 == 0) {
                    flow();
                    if (inWater() && position == Constants.NO_WATER) {
                        position = Constants.HAS_WATER;
                        start = timer.elapsedTime();
                    }
                    visibleArea();
                }
            }
            // Health regen and loss
            if (flown && timer != null && position == Constants.HAS_WATER &&
                timer.elapsedTime() - start == 3.0) {
                start += 3.0;
                health = Math.max(0, health - 1);
            } else if (flown && timer != null && position == Constants.NO_WATER &&
                timer.elapsedTime() - start == 5.0) {
                start += 5.0;
                if (!takePill)
                    health = Math.min(Constants.DEFAULT_HEALTH, health + 1);
                else
                    health = Math.min(Constants.DEFAULT_HEALTH + Constants.PILL_BUFF,
                            health + 1);
            }
            if (health == 0)
                lose();
        }
    }

    private void flow() {
        HashSet<Point> flowAreas = new HashSet<>();
        for (Point p: visible) {
            if (p.x != 0 && !flooded[p.x - 1][p.y]) {
                if (WORLD[p.x - 1][p.y].equals(Tileset.WALL)) {
                    flowAreas.add(new Point(p.x - 1, p.y));
                    flooded[p.x - 1][p.y] = true;
                }
                else if (WORLD[p.x - 1][p.y].equals(Tileset.FLOOR)) {
                    flowAreas.add(new Point(p.x - 1, p.y));
                    WORLD[p.x - 1][p.y] = Tileset.WATER;
                    flooded[p.x - 1][p.y] = true;
                } else if (WORLD[p.x - 1][p.y].equals(Tileset.AVATAR)) {
                    flowAreas.add(new Point(p.x - 1, p.y));
                    flooded[p.x - 1][p.y] = true;
                }
            }
            if (p.x != WIDTH - 1 && !flooded[p.x + 1][p.y]) {
                if (WORLD[p.x + 1][p.y].equals(Tileset.WALL)) {
                    flowAreas.add(new Point(p.x + 1, p.y));
                }
                else if (WORLD[p.x + 1][p.y].equals(Tileset.FLOOR)) {
                    flowAreas.add(new Point(p.x + 1, p.y));
                    WORLD[p.x + 1][p.y] = Tileset.WATER;
                    flooded[p.x + 1][p.y] = true;
                } else if (WORLD[p.x + 1][p.y].equals(Tileset.AVATAR)) {
                    flowAreas.add(new Point(p.x + 1, p.y));
                    flooded[p.x + 1][p.y] = true;
                }
            }
            if (p.y != 0 && !flooded[p.x][p.y - 1]) {
                if (WORLD[p.x][p.y - 1].equals(Tileset.WALL)) {
                    flowAreas.add(new Point(p.x, p.y - 1));
                }
                else if (WORLD[p.x][p.y - 1].equals(Tileset.FLOOR)) {
                    flowAreas.add(new Point(p.x, p.y - 1));
                    WORLD[p.x][p.y - 1] = Tileset.WATER;
                    flooded[p.x][p.y - 1] = true;
                } else if (WORLD[p.x][p.y - 1].equals(Tileset.AVATAR)) {
                    flowAreas.add(new Point(p.x, p.y - 1));
                    flooded[p.x][p.y - 1] = true;
                }
            }
            if (p.y != HEIGHT - 1 && !flooded[p.x][p.y + 1]) {
                if (WORLD[p.x][p.y + 1].equals(Tileset.WALL)) {
                    flowAreas.add(new Point(p.x, p.y + 1));
                } else if (WORLD[p.x][p.y + 1].equals(Tileset.FLOOR)) {
                    flowAreas.add(new Point(p.x, p.y + 1));
                    WORLD[p.x][p.y + 1] = Tileset.WATER;
                    flooded[p.x][p.y + 1] = true;
                } else if (WORLD[p.x][p.y + 1].equals(Tileset.AVATAR)) {
                    flowAreas.add(new Point(p.x, p.y + 1));
                    flooded[p.x][p.y + 1] = true;
                }
            }
        }
        visible.addAll(flowAreas);
    }

    /**
     * Make a section of world near the avatar visible.
     */
    private void visibleArea() {
        int l = 0, r = 0, t = 0, d = 0; // Indicating left, right, top, down
        int viewSize = 3;
        // Right
        if (avatar.x + viewSize >= WIDTH) r = WIDTH - 1;
        else r = avatar.x + viewSize;
        // Left
        if (avatar.x - viewSize < 0) l = 0;
        else l = avatar.x - viewSize;
        // Up
        if (avatar.y + viewSize >= HEIGHT) t = HEIGHT - 1;
        else t = avatar.y + viewSize;
        // Down
        if (avatar.y - viewSize < 0) d = 0;
        else d = avatar.y - viewSize;
        new TERenderer().renderArea(l, r, t, d, WORLD, inWater(), health, hasBrick, visible);
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                                                    //
//                                                 COMMANDS PROCESSING                                                //
//                                                                                                                    //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Display menu when {@link #interactWithKeyboard} is called
     */
    private void displayMenu() {
        UISetup();
        StdDraw.text(0.5, 0.7, "CS61B: The World");
        // Draw instructions
        StdDraw.text(0.5, 0.4, "New Game (N)");
        StdDraw.text(0.5, 0.3, "Load Game (L)");
        StdDraw.text(0.5, 0.2, "Help Menu (H)");
        StdDraw.text(0.5, 0.1, "Quit (Q)");

        StdDraw.show();
    }

    private void displayHelpMenu() throws IOException {
        UISetup();
        StdDraw.text(0.5, 0.8, "Game Rules ");
        StdDraw.setFont(new Font("Marker Felt", Font.PLAIN, 20));
        StdDraw.textLeft(0.1, 0.7, "1. Player starts with 10 air supply. " +
                "Die if air supply drops to 0.");
        StdDraw.textLeft(0.1, 0.6, "2. On floor player gains 1 air supply per " +
                "5 seconds, in water loses 1 air supply per 3 seconds");
        StdDraw.textLeft(0.1, 0.5, "3. You can have at most 10 air supply. " +
                "There is a medicine, take it you'll gain 3 air supply instantly, " +
                "and you can have at most 13 air supply now!");
        StdDraw.textLeft(0.1, 0.4, "4. Water flows from the locked door after 7 " +
                "seconds. At 5 seconds a warning is displayed. " +
                " Every 2 seconds water flows to nearby areas.");
        StdDraw.textLeft(0.1, 0.3, "5. Find the brick to block the door. Luckily, " +
                "water flown area will be visible.");
        StdDraw.text(0.5, 0.2, "Happy gaming! Press 'R' to return to the main " +
                "menu.");
        StdDraw.show();
        while (true) {
            char c = getNextKey();
            if (c == 'R') interactWithKeyboard();
        }
    }

    /**
     * Prompt user to enter a key.
     */
    private void promptSeed() {
        StdDraw.setCanvasSize(1200, 600);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Helvetica", Font.PLAIN, 40));
        StringBuilder promptArea = new StringBuilder();
        StringBuilder seed = new StringBuilder();
        // Set input text field
        promptArea.append("_".repeat(Math.max(0, 19 - "".length())));
        int start = "".length();
        while (true) {
            StdDraw.clear(Color.BLACK);
            // Draw title
            StdDraw.text(0.5, 0.4, "Enter seed, press 'S' when finished");
            StdDraw.text(0.5, 0.3, promptArea.toString());
            StdDraw.show();
            char c = getNextKey();
            // Add a digit
            if (Character.isDigit(c)) {
                promptArea.setCharAt(start++, c);
                seed.append(c);
            }
            // Seed entered
            else if (c == 'S') {
                random = new Random(Long.parseLong(seed.toString()));
                hasSeed = true;
                makeWorld();
                return;
            }
            // Support backspace
            else if ((int) c == 127) {
                promptArea.setCharAt(--start, '_');
                seed.deleteCharAt(seed.length() - 1);
            }
        }
    }

    private void processMenu(char c) throws IOException {
        switch (c) {
            case 'N':
                promptSeed();
                break;
            case 'L': // Load File
                // TODO: Load a File
                if (!loaded) {
                    promptLoad();
                }
                break;
            case 'Q':
                System.exit(0);
            case 'H':
                displayHelpMenu();
                return;
            default:
                return;
        }
        timer = new Stopwatch();
    }

    /**
     * process a input character
     * @param c character
     */
    private boolean processChr(char c) throws IOException {
        switch (c) {
            case 'W':
                moveUp();
                break;
            case 'A':
                moveLeft();
                break;
            case 'S':
                moveDown();
                break;
            case 'D':
                moveRight();
                break;
            case ':': // Quit
                quit = true;
                break;
            case 'Q':
                if (quit) promptSave();
            default:
                return false;
        }
        if (hasBrick && avatar.x == door.x && avatar.y == door.y)
            win();
        return true;
    }

    private void win() {
        UISetup();
        StdDraw.text(0.5, 0.5, "Congratulations! YOU WIN");
        StdDraw.show();
        StdDraw.pause(3000);
        System.exit(0);
    }

    private void lose() {
        UISetup();
        StdDraw.text(0.5, 0.5, "Oops! YOU LOSE");
        StdDraw.show();
        StdDraw.pause(3000);
        System.exit(0);
    }

    /**
     * Move avatar to the left
     */
    private void moveLeft() {
        if (!hasSeed) return;
        if (WORLD[avatar.x - 1][avatar.y].equals(Tileset.FLOOR)) {
            changeGround(-1, 0);
            WORLD[avatar.x - 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x -= 1;
        } else if (WORLD[avatar.x - 1][avatar.y].equals(Tileset.WATER)) {
            changeGround(-1, 0);
            WORLD[avatar.x - 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x -= 1;
        } else if (WORLD[avatar.x - 1][avatar.y].equals(Tileset.PILL)) {
            changeGround(-1, 0);
            WORLD[avatar.x - 1][avatar.y] = Tileset.AVATAR;
            health += Constants.PILL_BUFF;
            takePill = true;
            move();
            avatar.x -= 1;
        } else if (WORLD[avatar.x - 1][avatar.y].equals(Tileset.BRICK)) {
            changeGround(-1, 0);
            WORLD[avatar.x - 1][avatar.y] = Tileset.AVATAR;
            hasBrick = true;
            move();
            avatar.x -= 1;
        }
    }

    /**
     * Move the avatar right
     */
    private void moveRight() {
        if (!hasSeed) return;
        if (WORLD[avatar.x + 1][avatar.y].equals(Tileset.FLOOR)) {
            changeGround(1, 0);
            WORLD[avatar.x + 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x += 1;
        } else if (WORLD[avatar.x + 1][avatar.y].equals(Tileset.WATER)) {
            changeGround(1, 0);
            WORLD[avatar.x + 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x += 1;
        } else if (WORLD[avatar.x + 1][avatar.y].equals(Tileset.PILL)) {
            changeGround(1, 0);
            WORLD[avatar.x + 1][avatar.y] = Tileset.AVATAR;
            health += Constants.PILL_BUFF;
            takePill = true;
            move();
            avatar.x += 1;
        } else if (WORLD[avatar.x + 1][avatar.y].equals(Tileset.BRICK)) {
            changeGround(1, 0);
            WORLD[avatar.x + 1][avatar.y] = Tileset.AVATAR;
            hasBrick = true;
            move();
            avatar.x += 1;
        }
    }

    /**
     * Move the avatar up
     */
    private void moveUp() {
        if (!hasSeed) return;
        if (WORLD[avatar.x][avatar.y + 1].equals(Tileset.FLOOR)) {
            changeGround(0, 1);
            WORLD[avatar.x][avatar.y + 1] = Tileset.AVATAR;
            move();
            avatar.y += 1;
        } else if (WORLD[avatar.x][avatar.y + 1].equals(Tileset.WATER)) {
            changeGround(0, 1);
            WORLD[avatar.x][avatar.y + 1] = Tileset.AVATAR;
            move();
            avatar.y += 1;
        } else if (WORLD[avatar.x][avatar.y + 1].equals(Tileset.PILL)) {
            changeGround(0, 1);
            WORLD[avatar.x][avatar.y + 1] = Tileset.AVATAR;
            takePill = true;
            health += Constants.PILL_BUFF;
            move();
            avatar.y += 1;
        } else if (WORLD[avatar.x][avatar.y + 1].equals(Tileset.BRICK)) {
            changeGround(0, 1);
            WORLD[avatar.x][avatar.y + 1] = Tileset.AVATAR;
            hasBrick = true;
            move();
            avatar.y += 1;
        }
    }

    /**
     * Move the avatar down
     */
    private void moveDown() {
        if (!hasSeed) return;
        if (WORLD[avatar.x][avatar.y - 1].equals(Tileset.FLOOR)) {
            changeGround(0, -1);
            WORLD[avatar.x][avatar.y - 1] = Tileset.AVATAR;
            move();
            avatar.y -= 1;
        } else if (WORLD[avatar.x][avatar.y - 1].equals(Tileset.WATER)) {
            changeGround(0, -1);
            WORLD[avatar.x][avatar.y - 1] = Tileset.AVATAR;
            move();
            avatar.y -= 1;
        } else if (WORLD[avatar.x][avatar.y - 1].equals(Tileset.PILL)) {
            changeGround(0, -1);
            WORLD[avatar.x][avatar.y - 1] = Tileset.AVATAR;
            health += Constants.PILL_BUFF;
            takePill = true;
            move();
            avatar.y -= 1;
        } else if (WORLD[avatar.x][avatar.y - 1].equals(Tileset.BRICK)) {
            changeGround(0, -1);
            WORLD[avatar.x][avatar.y - 1] = Tileset.AVATAR;
            move();
            hasBrick = true;
            avatar.y -= 1;
        }
    }

    /**
     * Determine where the place that avatar leaves should be.
     */
    private void move() {
        if (!flooded[avatar.x][avatar.y])
            WORLD[avatar.x][avatar.y] = Tileset.FLOOR;
        else
            WORLD[avatar.x][avatar.y] = Tileset.WATER;
    }

    private void changeGround(int dx, int dy) {
        if (flooded[avatar.x + dx][avatar.y + dy] != flooded[avatar.x][avatar.y]) {
            System.out.println("Go");
            if (flooded[avatar.x][avatar.y] == Constants.HAS_WATER)
                position = Constants.NO_WATER;
            else
                position = Constants.HAS_WATER;
            start = timer.elapsedTime();
        }
    }

    /**
     * Get key from keyboard.
     * @return The typed key
     */
    private char getKey() {
        while (true) {
            if (StdDraw.hasNextKeyTyped())
                return StdDraw.nextKeyTyped();
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                                                    //
//                                      SAVING AND LOADING GAME                                                       //
//                                                                                                                    //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * UI window for saving the file
     */
    private void promptSave() throws IOException {
        UISetup();
        StdDraw.text(0.5, 0.6, "Do you want to save your game?");
        StdDraw.text(0.5, 0.4, "Press 'S' to save or 'N' to leave.");
        StdDraw.show();
        while (true) {
            char save = getNextKey();
            if (save == 'N') System.exit(0);
            else if (save == 'S') {
                StringBuilder display = new StringBuilder();
                StringBuilder filename = new StringBuilder();
                int length = 0;
                // Set max name length to be 15
                display.append("_".repeat(Math.max(0, 15)));
                while (true) {
                    StdDraw.clear(Color.BLACK);
                    StdDraw.text(0.5, 0.6, "Enter the name of your game");
                    StdDraw.text(0.5, 0.4, display.toString());
                    StdDraw.show();
                    char c = getKey();
                    if ((int) c == 10)
                        saveFile(filename.toString());
                    else if ((int) c == 127) {
                        display.setCharAt(length - 1, '_');
                        filename.deleteCharAt(length-- - 1);
                    }
                    else {
                        display.setCharAt(length++, c);
                        filename.append(c);
                    }
                }
            }
        }
    }

    /**
     * Display loading menu
     */
    private void promptLoad() throws IOException {
        UISetup();
        File dir = new File(GAME_FOLDER);
        File[] txtFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.endsWith(".txt");
            }
        });

        double gap = (1.0 - 0.2) / txtFiles.length;
        StringBuilder display = new StringBuilder(
                "_".repeat(15)
        );
        StringBuilder filename = new StringBuilder();
        while (true) {
            StdDraw.clear(Color.BLACK);
            for (int i = 0; i < txtFiles.length; i++) {
                StdDraw.text(0.5, 0.2 + gap * i,
                        getFileName(txtFiles[i].toString()));
            }
            StdDraw.text(0.5, 0.1,
                    "Enter the file you want to load: " + display);
            StdDraw.show();
            char c = getKey();
            if ((int) c == 10) {
                try {
                    loaded = true;
                    loadFile(getFilePath(txtFiles, filename.toString()));
                    initialize();
                    return;
                } catch (Exception ex) {
                    StdDraw.setFont(new Font("Helvetica", Font.PLAIN, 10));
                    StdDraw.text(0.5, 0.05,
                            "File Name invalid, try again");
                    StdDraw.pause(2000);
                    StdDraw.setFont(new Font("Helvetica", Font.PLAIN, 40));
                }
            } else if ((int) c == 127) {
                display.setCharAt(filename.length() - 1, '_');
                filename.deleteCharAt(filename.length() - 1);
            } else {
                display.setCharAt(filename.length(), c);
                filename.append(c);
            }
        }
    }

    /**
     * Saving file to the designated folder: {@link #GAME_FOLDER}
     * @param filename Input filename
     */
    private void saveFile(String filename) throws IOException {
        FileWriter fw = new FileWriter( GAME_FOLDER + filename + ".txt");
        for (int i = 0; i < WIDTH; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < HEIGHT; j++) {
               line.append(encode(WORLD[i][j])).append(" ");
            }
            line.append("\n");
            fw.write(line.toString());
        }
        fw.close();
        System.exit(0);
    }

    /**
     * Load a game file to UI.
     * @param filename Absolute path
     */
    private void loadFile(String filename) throws FileNotFoundException {
        File file = new File(filename);
        Scanner read = new Scanner(file);
        int i = 0;
        while (read.hasNextLine()) {
            String[] column = read.nextLine().split(" ");
            for (int j = 0; j < HEIGHT; j++) {
                if (Integer.parseInt(column[j]) == Constants.AVATAR)
                    avatar = new Point(i, j);
                if (Integer.parseInt(column[j]) == Constants.LOCKED_DOOR)
                    door = new Point(i, j);
                WORLD[i][j] = decode(Integer.parseInt(column[j]));
            }
            i++;
        }
        read.close();
    }

    /**
     * Encode a given tile with a constant in {@link Constants}
     * @param t Tile
     * @return Code
     */
    private int encode(TETile t) {
        if (Tileset.NOTHING.equals(t)) {
            return Constants.NOTHING;
        } else if (Tileset.AVATAR.equals(t)) {
            return Constants.AVATAR;
        } else if (Tileset.FLOOR.equals(t)) {
            return Constants.FLOOR;
        } else if (Tileset.WALL.equals(t)) {
            return Constants.WALL;
        } else if (Tileset.LOCKED_DOOR.equals(t)) {
            return Constants.LOCKED_DOOR;
        } else if (Tileset.WATER.equals(t)) {
            return Constants.WATER;
        } else if (Tileset.PILL.equals(t)) {
            return Constants.PILL;
        } else if (Tileset.BRICK.equals(t)) {
            return Constants.BRICK;
        }
        return 100;
    }

    /**
     * Decode a given constant representation of a tile to its {@link TETile}
     * corresponding object
     * @param n The code
     * @return Original tile
     */
    private TETile decode(int n) {
        if (n == Constants.NOTHING) {
            return Tileset.NOTHING;
        } else if (n == Constants.AVATAR) {
            return Tileset.AVATAR;
        } else if (n == Constants.FLOOR) {
            return Tileset.FLOOR;
        } else if (n == Constants.WALL) {
            return Tileset.WALL;
        } else if (n == Constants.LOCKED_DOOR) {
            return Tileset.LOCKED_DOOR;
        } else if (n == Constants.WATER) {
            return Tileset.WATER;
        } else if (n == Constants.PILL) {
            return Tileset.PILL;
        } else if (n == Constants.BRICK) {
            return Tileset.BRICK;
        }
        return null;
    }

    /**
     * Set up the default user interface including view port size, background
     * color, font style and color.
     */
    private void UISetup() {
        StdDraw.setCanvasSize(1200, 600);
        StdDraw.clear(Color.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new Font("Helvetica", Font.PLAIN, 40));
    }

    /**
     * Get the raw file name from absolute path.
     * E.g. for "test/library/doc1.txt", the output will be "doc1"
     * @param filename Absolute path
     * @return Raw file name
     */
    private String getFileName(String filename) {
        int lastDelimiter = filename.lastIndexOf('/');
        int lastFileDelimiter = filename.lastIndexOf('.');
        return filename.substring(lastDelimiter + 1, lastFileDelimiter);
    }

    /**
     * Get absolute path from the raw file name which is specified in
     * {@link #getFileName}. Search in the designated file folder for any
     * match.
     * @param files All the files in the folder
     * @param abbr Raw file name
     * @throws IllegalArgumentException If the raw name does not correspond
     * to a legal file path
     * @return The absolute path.
     */
    private String getFilePath(File[] files, String abbr) {
        for (File file: files)
            if (getFileName(file.toString()).equals(abbr))
                return file.toString();
        throw new IllegalArgumentException();
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                                                    //
//                               METHODS USED FOR GENERATING RANDOM WORLD                                             //
//                                                                                                                    //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Generate a random world
     */
    private void makeWorld() {
        setup();
        List<List<Room>> rooms = fillRoom();
        linkRooms(rooms);
        placeWalls();
        placeLockedDoor();
        items();
        initialize();
    }

    /**
     * Initialize the game window
     */
    private void initialize() {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);
    }

    /**
     * Put walls near the floor area. For rule reference {@link #nearFloor}
     */
    private void placeWalls() {
        for (int i = 0; i < WIDTH; i++)
            for (int j = 0; j < HEIGHT; j++)
                if (nearFloor(i, j))
                    WORLD[i][j] = Tileset.WALL;
    }

    /**
     * Place a locked door.
     * <p>
     *     First decide whether the door will be placed on top or below.
     *     Then pick a starting point and go from that until it reaches a wall, which
     *     will be made door.
     * </p>
     */
    private void placeLockedDoor() {
        int choose = RandomUtils.uniform(random, 2);
        int start = RandomUtils.uniform(random, 20, 60);
        if (choose == 0) {
            while (true) {
                for (int i = 0; i < HEIGHT; i++) {
                    if (WORLD[start][i].equals(Tileset.WALL)) {
                        if (WORLD[start][i + 1].equals(Tileset.FLOOR)) {
                            WORLD[start][i] = Tileset.LOCKED_DOOR;
                            door = new Point(start, i);
                            return;
                        } else {
                            start++;
                            break;
                        }
                    }
                }
            }
        } else {
            while (true) {
                for (int i = HEIGHT - 1; i >= 0; i--) {
                    if (WORLD[start][i].equals(Tileset.WALL)) {
                        if (WORLD[start][i - 1].equals(Tileset.FLOOR)) {
                            WORLD[start][i] = Tileset.LOCKED_DOOR;
                            door = new Point(start, i);
                            return;
                        } else {
                            start--;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate a list of random rooms. Generation Rule:
     * <li>
     *     Start from bottom left, generate rooms on the right.
     * </li>
     * <li>
     *     If reaches right bound, start from left again, this time
     *     from a new room above the previous one.
     * </li>
     */
    private List<List<Room>> fillRoom() {
        int y = 0;
        int i = 0;
        List<List<Room>> roomsInWorld = new ArrayList<>();
        while (y < HEIGHT) {
            int[] startSize = generateRoomSize();
            int[] startLLCorner = generateLLCorner(0, y, startSize[1]);
            // Checks
            if (startLLCorner[1] + startSize[1] >= HEIGHT-1) break;
            roomsInWorld.add(new ArrayList<>());
            Room start = new Room(startSize[0], startSize[1],
                    new Point(startLLCorner[0], startLLCorner[1]));
            roomsInWorld.get(i).add(start);
            int x = startSize[0] + startLLCorner[0];
            // Add rooms until reaches right border
            while (x < WIDTH-1) {
                int[] size = generateRoomSize();
                int[] LLCorner = generateLLCorner(x, y, size[1]);
                x = size[0] + LLCorner[0];
                // Checks
                if (x >= WIDTH-1) break;
                if (size[1] + LLCorner[1] >= HEIGHT-1) break;
                Room room = new Room(size[0], size[1],
                        new Point(LLCorner[0], LLCorner[1]));
                roomsInWorld.get(i).add(room);
            }
            y = startLLCorner[1] + startSize[1];
            i += 1;
        }
        return roomsInWorld;
    }

    /**
     * Create links between rooms. The rule is:
     * <li>Fill the bottom row with right links and left column with
     *     up links</li>
     * <li>For each room that was not linked yet, choose randomly
     *     between the left or the bottom room of it to create a link.
     *     If one position is unavailable, link to the available one.</li>
     * @param world The grid of all rooms need to be linked
     */
    private void linkRooms(List<List<Room>> world) {
        // Rule 1
        for (int i = 0; i < world.size() - 1; i++) {
            Room r1 = world.get(i).get(0);
            Room r2 = world.get(i+1).get(0);
            new Link(random, r1, r2, Link.VERTICAL_TURN);
        }
        for (int i = 0; i < world.get(0).size() - 1; i++) {
            Room r1 = world.get(0).get(i);
            Room r2 = world.get(0).get(i+1);
            new Link(random, r1, r2, Link.HORIZONTAL_TURN);
        }
        // Rule 2
        for (int i = 1; i < world.size(); i++) {
            for (int j = 1; j < world.get(i).size(); j++) {
                List<Object> pick = pickRoom(world, i, j);
                Room r1 = (Room) pick.get(1);
                Room r2 = world.get(i).get(j);
                new Link(random, r1, r2, (String) pick.get(0));
            }
        }
    }

    /**
     * Set the world to tiles of nothing
     */
    private void setup() {
        for (int i = 0; i < WIDTH; i++)
            for (int j = 0; j < HEIGHT; j++) {
                WORLD[i][j] = Tileset.NOTHING;
                flooded[i][j] = Constants.NO_WATER;
            }
    }

    /**
     * Return a random room size.
     * <p>
     * Width ranges from 2-8, Height ranges from 2-6
     * </p>
     * @return Room size
     */
    private int[] generateRoomSize() {
        int x = RandomUtils.uniform(random, 2, 9);
        int y = RandomUtils.uniform(random, 2, 7);
        return new int[] {x, y};
    }

    /**
     * Generate random LLCorner for the next room adjacent to the previous one.
      */
    private int[] generateLLCorner(int x, int y, int h) {
        int rx = RandomUtils.uniform(random, x+2, x+8);
        // Compute lower bound so bottom will not be out of range
        int ry = RandomUtils.uniform(random, y+2, y+h+2);
        return new int[] {rx, ry};
    }

    /**
     * Check whether a given position should be placed with walls or nothing.
     */
    private boolean nearFloor(int x, int y) {
        if (WORLD[x][y].equals(Tileset.FLOOR)) return false;

        if (x != 0) {
            if (WORLD[x - 1][y].equals(Tileset.FLOOR)) return true;
            if (y != 0 && WORLD[x-1][y-1].equals(Tileset.FLOOR)) return true;
            if (y != HEIGHT - 1 && WORLD[x-1][y+1].equals(Tileset.FLOOR)) return true;
        }
        if (x != WIDTH - 1) {
            if (WORLD[x + 1][y].equals(Tileset.FLOOR)) return true;
            if (y != 0 && WORLD[x+1][y-1].equals(Tileset.FLOOR)) return true;
            if (y != HEIGHT - 1 && WORLD[x+1][y+1].equals(Tileset.FLOOR)) return true;
        }
        if (y != 0)
            if (WORLD[x][y-1].equals(Tileset.FLOOR)) return true;
        if (y != HEIGHT - 1)
            if (WORLD[x][y+1].equals(Tileset.FLOOR)) return true;


        return false;
    }

    /**
     * Pick a room on the left or below the target room.
     */
    private List<Object> pickRoom(List<List<Room>> rooms, int i, int j) {
        List<String> info = new ArrayList<>();
        List<Room> slots = new ArrayList<>();
        try {
            slots.add(rooms.get(i).get(j-1));
            info.add(Link.HORIZONTAL_TURN);
        } catch (Exception ignored) {}
        try {
            slots.add(rooms.get(i-1).get(j));
            info.add(Link.VERTICAL_TURN);
        } catch (Exception ignored) {}
        int pick = RandomUtils.uniform(random, info.size());
        List<Object> all = new ArrayList<>();
        all.add(info.get(pick));
        all.add(slots.get(pick));
        return all;
    }

    /**
     * Choose the position of the avatar.
     */
    private void items() {
        // Initialise all possible places for the avatar
        Map<Integer, Point> floorMap = new HashMap<>();
        int count = 0;
        for (int i = 0; i < WIDTH; i++)
            for (int j = 0; j < HEIGHT; j++)
                if (WORLD[i][j].equals(Tileset.FLOOR))
                    floorMap.put(count++, new Point(i, j));
        // pick avatar
        Point pickAvatar = floorMap.get(RandomUtils.uniform(random, count));
        WORLD[pickAvatar.x][pickAvatar.y] = Tileset.AVATAR;
        avatar = pickAvatar;
        // pick medicine
        Point pickMedicine = floorMap.get(RandomUtils.uniform(random, count));
        while (pickMedicine.equals(pickAvatar))
            pickMedicine = floorMap.get(RandomUtils.uniform(random, count));
        WORLD[pickMedicine.x][pickMedicine.y] = Tileset.PILL;
        pill = pickMedicine;
        // pick brick
        Point pickBrick = floorMap.get(RandomUtils.uniform(random, count));
        while (pickMedicine.equals(pickBrick) || pickAvatar.equals(pickBrick))
            pickBrick = floorMap.get(RandomUtils.uniform(random, count));
        WORLD[pickBrick.x][pickBrick.y] = Tileset.BRICK;
        brick = pickBrick;
    }
}
