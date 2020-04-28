package byow.Core;

import byow.TileEngine.Tileset;

import java.util.Random;

/**
 * Utility class for a hallway in the world
 */
public class Link {

    public static final String VERTICAL_TURN = "VTurn";
    public static final String HORIZONTAL_TURN = "HTurn";

    Room r1;
    Room r2;
    Random random;

    String type;

    public Link(Random random, Room r1, Room r2, String type) {
        this.random = random;
        this.r1 = r1;
        this.r2 = r2;
        this.type = type;
        switch (type) {
            case VERTICAL_TURN:
                VTurn(); return;
            case HORIZONTAL_TURN:
                HTurn(); return;
            default:
                return;
        }
    }

    /* Below are all methods used to construct different shapes of links. */

    /**
     * Generate a Vertical Link with a turn in the middle. Method:
     * <li>Pick start point in r1 and r2</li>
     * <li>Pick a break point indicating turn in the middle</li>
     */
    public void VTurn() {
        swapV();
        int[] f1 = roomFeature(r1);
        int[] f2 = roomFeature(r2);
        // Generate start and end points
        int x1 = RandomUtils.uniform(random, f1[2], f1[2] + f1[0]);
        int x2 = RandomUtils.uniform(random, f2[2], f2[2] + f2[0]);
        // Generate turn point
        int turn;
        if (f1[3] + f1[1] + 1 == f2[3])
            turn = f2[3];
        else if (f1[3] + f1[1] + 1 < f2[3])
            turn = RandomUtils.uniform(random, f1[3] + f1[1] + 1, f2[3]);
        else
            turn = RandomUtils.uniform(random, f2[3], f1[3] + f1[1] + 1);
        // Fill the parts
        for (int i = turn; i <= f2[3]; i++)
            Engine.WORLD[x2][i] = Tileset.FLOOR;
        for (int i = turn; i >= f1[3]+f1[1]; i--)
            Engine.WORLD[x1][i] = Tileset.FLOOR;
        if (x1 < x2)
            for (int i = x1; i <= x2; i++)
                Engine.WORLD[i][turn] = Tileset.FLOOR;
        else
            for (int i = x2; i <= x1; i++)
                Engine.WORLD[i][turn] = Tileset.FLOOR;
    }

    /**
     * Generate a Horizontal Link with a turn in the middle. Method:
     * <li>Pick start point in r1 and r2</li>
     * <li>Pick a break point indicating turn in the middle</li>
     */
    public void HTurn() {
        swapH();
        int[] f1 = roomFeature(r1);
        int[] f2 = roomFeature(r2);
        // Generate start and end points
        int y1 = RandomUtils.uniform(random, f1[3], f1[3] + f1[1]);
        int y2 = RandomUtils.uniform(random, f2[3], f2[3] + f2[1]);
        // Generate turn point
        int turn = RandomUtils.uniform(random, f1[2] + f1[0] + 1, f2[2]);
        // Fill the link
        for (int i = turn; i <= f2[2]; i++)
            Engine.WORLD[i][y2] = Tileset.FLOOR;
        for (int i = turn; i >= f1[2]+f1[0]; i--)
            Engine.WORLD[i][y1] = Tileset.FLOOR;
        if (y1 < y2)
            for (int i = y1; i <= y2; i++)
                Engine.WORLD[turn][i] = Tileset.FLOOR;
        else
            for (int i = y2; i <= y1; i++)
                Engine.WORLD[turn][i] = Tileset.FLOOR;
    }

    /**
     * Return the features of a room, listed as:
     * <p>
     *     [width, height, leftCorner.x, leftCorner.y]
     * </p>
     */
    public int[] roomFeature(Room r) {
        return new int[] {r.x, r.y, r.LLCorner.x, r.LLCorner.y};
    }

    /**
     * Swap order of rooms to make sure r1 has smaller LLCorner-y coordinate than r2
     */
    public void swapV() {
        if (r2.LLCorner.y < r1.LLCorner.y) {
            Room temp = r1;
            r1 = r2;
            r2 = temp;
        }
    }

    /**
     * Swap order of rooms to make sure r1 has smaller LLCorner-x coordinate than r2
     */
    public void swapH() {
        if (r2.LLCorner.x < r1.LLCorner.x) {
            Room temp = r1;
            r1 = r2;
            r2 = temp;
        }
    }
}
