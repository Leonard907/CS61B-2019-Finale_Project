package byow.Core;

import byow.TileEngine.Tileset;

/**
 * Utility class for a room in the world
 */
public class Room {

    int x;
    int y;
    Point LLCorner;

    /**
     * Room constructor.
     */
    public Room(int x, int y, Point LLCorner) {
        this.x = x;
        this.y = y;
        this.LLCorner = LLCorner;
        fill();
    }

    private void fill() {
        for (int i = LLCorner.x; i < LLCorner.x + x; i++)
            for (int j = LLCorner.y; j < LLCorner.y + y; j++)
                Engine.WORLD[i][j] = Tileset.FLOOR;
    }

    @Override
    public String toString() {
        return "Room{" +
                "x=" + x +
                ", y=" + y +
                ", LLCorner=" + LLCorner +
                '}';
    }
}
