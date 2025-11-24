package nl.isy_games;

import java.util.ArrayList;

public class ReversiRules{

    //haal alle legal moves op en return ze
    public void getLegalMoves(){

    }

    //return of player een move kan doen
    public boolean hasLegalMove(){
        return false;
    }

    public void applyMove(int[][] board){

    }

    //check x, y en diagonalen op het bord
    public void checkAllDirections(String[][] board, int row, int col){
        ArrayList<String> tiles = new ArrayList<String>();
        
        //horizontaal check
        //check alle x voor startpunt
        for(int i = col-1; i >= 0; i--){
            tiles.add(board[row][i]);
        }
        //check alle x na startpunt
        for(int i = col+1; i <= 7; i++){
            tiles.add(board[row][i]);
        }
        //verticaal check
        //check alle y boven startpunt
        for(int i = row-1; i >= 0; i--){
            tiles.add(board[i][col]);
        }
        //check alle y onder startpunt
        for(int i = row+1; i<= 7; i++){
            tiles.add(board[i][col]);
        }
        //diagonaal check
        //-x -y check

        //+x -y check

        //-x +y check

        //-x -y check
        
        System.out.println(tiles);
    }

    //return score van beide kleuren
    public void getScore(){
        
    }

    //return als er geen valid moves meer zijn
    public void checkGameOver(){

    }
}
