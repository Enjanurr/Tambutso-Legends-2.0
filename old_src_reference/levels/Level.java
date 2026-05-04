package levels;

public class Level {
    private int[][] lvlData; // imagine this one

    public Level(int[][] levelData){
        this.lvlData = levelData;
    }

    public int getSpriteIndex(int row , int col ){
        return lvlData[row][col];
    }

    public int[][] getLevelData(){
        return lvlData;
    }
}
