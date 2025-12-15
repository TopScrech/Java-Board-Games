package nl.isy_games;

import java.util.ArrayList;

public class ReversiRules{

    public ArrayList<int[]> getLegalMoves(String[][] board, String piece){
        ArrayList<int[]> tiles = new ArrayList<int[]>();
        ArrayList<int[]> legalMoves = new ArrayList<int[]>();
        String pieceCheck = piece.equals("X") ? "O" : "X";

        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if(board[i][j].equals("")){
                    if(i+1 < 8 && board[i+1][j].equals(pieceCheck)) tiles.add(new int[]{i,j});
                    if(i-1 >= 0 && board[i-1][j].equals(pieceCheck)) tiles.add(new int[]{i,j});
                    if(j+1 < 8 && board[i][j+1].equals(pieceCheck)) tiles.add(new int[]{i,j});
                    if(j-1 >= 0 && board[i][j-1].equals(pieceCheck)) tiles.add(new int[]{i,j});
                    if(i-1 >= 0 && j-1 >= 0 && board[i-1][j-1].equals(pieceCheck)) tiles.add(new int[]{i,j});
                    if(i+1 < 8 && j-1 >= 0 && board[i+1][j-1].equals(pieceCheck)) tiles.add(new int[]{i,j});
                    if(i-1 >= 0 && j+1 < 8 && board[i-1][j+1].equals(pieceCheck)) tiles.add(new int[]{i,j});
                    if(i+1 < 8 && j+1 < 8 && board[i+1][j+1].equals(pieceCheck)) tiles.add(new int[]{i,j});
                }
            }
        }

        for(int[] e : tiles){
            ArrayList<int[]> tempArray = checkAllDirections(board, e[0], e[1], piece);
            if(!tempArray.isEmpty()){
                legalMoves.add(e);
            }
        }

        return legalMoves;
    }

    public boolean hasLegalMove(int legalMoves){
        return legalMoves > 0;
    }

    public void applyMove(int[][] board){ }

    public ArrayList<int[]> checkAllDirections(String[][] board, int row, int col, String piece){
        ArrayList<int[]> tiles = new ArrayList<>();

        int[][] directions = {
                {-1,0},{1,0},{0,-1},{0,1},
                {-1,-1},{-1,1},{1,-1},{1,1}
        };

        for(int[] dir : directions){
            int r = row + dir[0];
            int c = col + dir[1];
            ArrayList<int[]> temp = new ArrayList<>();

            while(r >= 0 && r < 8 && c >= 0 && c < 8){
                if(board[r][c].equals("")) break;
                if(board[r][c].equals(piece)){
                    tiles.addAll(temp);
                    break;
                }
                temp.add(new int[]{r,c});
                r += dir[0];
                c += dir[1];
            }
        }

        return tiles;
    }

    public void getScore(){ }
    public void checkGameOver(){ }
}

