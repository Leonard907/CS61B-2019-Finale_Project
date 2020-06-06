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
    /** Health bar */
    int health = Constants.DEFAULT_HEALTH;
    /** Medicine position */
    Point pill;
    /** Brick position */
    Point brick;
    /** Times water have flown */
    int flowCount;
    /** The starting count of the flow */
    int start;


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
                if (hasSeed)
                    visibleArea();
                //new TERenderer().renderFrame(WORLD, health, inWater(), hasBrick);
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
                    visibleArea();
                }
            }
            // Update water flown area
            else if (timer != null) {
                if (timer.elapsedTime() % 2 == 0) {
                    flow();
                    visibleArea();
                }
            }
            if (flown && inWater() && position == Constants.NO_WATER) {
                position = Constants.HAS_WATER;
                start = flowCount;
            } else if (flown && !inWater() && position == Constants.HAS_WATER) {
                position = Constants.NO_WATER;
                start = flowCount;
            }

            if (flown && position == Constants.HAS_WATER && flowCount > start &&
                flowCount - start == 1) {
                start += 1;
                health = Math.max(0, health - 1);
            } else if (flown && position == Constants.NO_WATER && flowCount > start &&
                        flowCount - start == 4) {
                start += 4;
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
        flowCount++;
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
                } else if (WORLD[p.x - 1][p.y].equals(Tileset.PILL)) {
                    flowAreas.add(new Point(p.x - 1, p.y));
                    flooded[p.x - 1][p.y] = true;
                } else if (WORLD[p.x - 1][p.y].equals(Tileset.BRICK)) {
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
                } else if (WORLD[p.x + 1][p.y].equals(Tileset.PILL)) {
                    flowAreas.add(new Point(p.x + 1, p.y));
                    flooded[p.x + 1][p.y] = true;
                } else if (WORLD[p.x + 1][p.y].equals(Tileset.BRICK)) {
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
                } else if (WORLD[p.x][p.y - 1].equals(Tileset.PILL)) {
                    flowAreas.add(new Point(p.x, p.y - 1));
                    flooded[p.x][p.y - 1] = true;
                } else if (WORLD[p.x][p.y - 1].equals(Tileset.BRICK)) {
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
                } else if (WORLD[p.x][p.y + 1].equals(Tileset.PILL)) {
                    flowAreas.add(new Point(p.x, p.y + 1));
                    flooded[p.x][p.y + 1] = true;
                } else if (WORLD[p.x][p.y + 1].equals(Tileset.BRICK)) {
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
        int l, r, t, d; // Indicating left, right, top, down
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
        StdDraw.text(0.5, 0.3, "New Game (N)");
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
                "8 seconds, in water loses 1 air supply per 2 seconds");
        StdDraw.textLeft(0.1, 0.5, "3. You can have at most 5 air supply. " +
                "There is a medicine, take it and gain 3 air supply instantly, " +
                "and you can have at most 8 air supply now!");
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
            WORLD[avatar.x - 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x -= 1;
        } else if (WORLD[avatar.x - 1][avatar.y].equals(Tileset.WATER)) {
            WORLD[avatar.x - 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x -= 1;
        } else if (WORLD[avatar.x - 1][avatar.y].equals(Tileset.PILL)) {
            WORLD[avatar.x - 1][avatar.y] = Tileset.AVATAR;
            health += Constants.PILL_BUFF;
            takePill = true;
            move();
            avatar.x -= 1;
        } else if (WORLD[avatar.x - 1][avatar.y].equals(Tileset.BRICK)) {
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
            WORLD[avatar.x + 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x += 1;
        } else if (WORLD[avatar.x + 1][avatar.y].equals(Tileset.WATER)) {
            WORLD[avatar.x + 1][avatar.y] = Tileset.AVATAR;
            move();
            avatar.x += 1;
        } else if (WORLD[avatar.x + 1][avatar.y].equals(Tileset.PILL)) {
            WORLD[avatar.x + 1][avatar.y] = Tileset.AVATAR;
            health += Constants.PILL_BUFF;
            takePill = true;
            move();
            avatar.x += 1;
        } else if (WORLD[avatar.x + 1][avatar.y].equals(Tileset.BRICK)) {
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
            WORLD[avatar.x][avatar.y + 1] = Tileset.AVATAR;
            move();
            avatar.y += 1;
        } else if (WORLD[avatar.x][avatar.y + 1].equals(Tileset.WATER)) {
            WORLD[avatar.x][avatar.y + 1] = Tileset.AVATAR;
            move();
            avatar.y += 1;
        } else if (WORLD[avatar.x][avatar.y + 1].equals(Tileset.PILL)) {
            WORLD[avatar.x][avatar.y + 1] = Tileset.AVATAR;
            takePill = true;
            health += Constants.PILL_BUFF;
            move();
            avatar.y += 1;
        } else if (WORLD[avatar.x][avatar.y + 1].equals(Tileset.BRICK)) {
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
            WORLD[avatar.x][avatar.y - 1] = Tileset.AVATAR;
            move();
            avatar.y -= 1;
        } else if (WORLD[avatar.x][avatar.y - 1].equals(Tileset.WATER)) {
            WORLD[avatar.x][avatar.y - 1] = Tileset.AVATAR;
            move();
            avatar.y -= 1;
        } else if (WORLD[avatar.x][avatar.y - 1].equals(Tileset.PILL)) {
            WORLD[avatar.x][avatar.y - 1] = Tileset.AVATAR;
            health += Constants.PILL_BUFF;
            takePill = true;
            move();
            avatar.y -= 1;
        } else if (WORLD[avatar.x][avatar.y - 1].equals(Tileset.BRICK)) {
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
