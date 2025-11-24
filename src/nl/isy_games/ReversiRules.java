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
    public ArrayList<int[]> checkAllDirections(String[][] board, int row, int col, String piece){
        ArrayList<int[]> tiles = new ArrayList<int[]>();

        //horizontaal check
        //check alle -x voor startpunt
        for(int i = col-1; i >= 0; i--){
            int[] arr = {row, i};
            if(board[row][i] != piece && board[row][i] != ""){
                tiles.add(arr);
            }
            if(board[row][i] == piece){
                break;
            }
           
        }
        //check alle +x na startpunt
        for(int i = col+1; i <= 7; i++){
            int[] arr = {row, i};
            if(board[row][i] != piece && board[row][i] != ""){
                tiles.add(arr);
            }
            if(board[row][i] == piece){
                break;
            }
        }
        //verticaal check
        //check alle -y boven startpunt
        for(int i = row-1; i >= 0; i--){
            int[] arr = {i, col};
            if(board[i][col] != piece && board[i][col] != ""){
                tiles.add(arr);
            }
            if(board[i][col] == piece){
                break;
            }
        }
        //check alle +y onder startpunt
        for(int i = row+1; i<= 7; i++){
            int[] arr = {i, col};
            if(board[i][col] != piece && board[i][col] != ""){
                tiles.add(arr);
            }
            if(board[i][col] == piece){
                break;
            }
        }
        //diagonaal check
        //-x -y check
        for(int i = col-1, j = row-1; i >= 0 && j >= 0; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tiles.add(arr);
            }
            if(board[j][i] == piece){
                break;
            }
        }
        //+x -y check
        for(int i = col+1, j = row-1; i <= 7 && j >= 0; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tiles.add(arr);
            }
            if(board[j][i] == piece){
                break;
            }
        }
        //-x +y check
        for(int i = col-1, j = row+1; i >= 0 && j <= 7; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tiles.add(arr);
            }
            if(board[j][i] == piece){
                break;
            }
        }
        //+x +y check
        for(int i = col+1, j = row+1; i <= 7 && j <= 7; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tiles.add(arr);
            }
            if(board[j][i] == piece){
                break;
            }
        }
        return tiles;
    }

    //return score van beide kleuren
    public void getScore(){
        
    }

    //return als er geen valid moves meer zijn
    public void checkGameOver(){

    }
}
