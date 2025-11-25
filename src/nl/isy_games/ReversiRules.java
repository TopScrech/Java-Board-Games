package nl.isy_games;

import java.util.ArrayList;

public class ReversiRules{

    //haal alle legal moves op en return ze
     //haal alle legal moves op en return ze
    public ArrayList<int[]> getLegalMoves(String[][] board, String piece){
        ArrayList<int[]> tiles = new ArrayList<int[]>();
        ArrayList<int[]> legalMoves = new ArrayList<int[]>();
        String pieceCheck;
        //check alle tiles waar een legal move gedaan kan worden.
        //return alle tiles die voldoen aan eisen
        if(piece == "X"){
            pieceCheck = "O";
        }else{
            pieceCheck = "X";
        }
        for(int i = 0; i < 7; i++){
            for(int j = 0; j < 7; j++){
                if(board[i][j] == ""){
                    if(i+1 <= 7 && board[i+1][j] == pieceCheck){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }   
                    if(i-1 > 0 && board[i-1][j] == pieceCheck ){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }
                    if(j+1 <= 7 && board[i][j+1] == pieceCheck){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }
                    if(j-1 > 0 && board[i][j-1] == pieceCheck){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }
                    if(i-1 > 0 && j-1 > 0 && board[i-1][j-1] == pieceCheck){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }
                    if(i+1 <= 7 && j-1 > 0 && board[i+1][j-1] == pieceCheck){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }
                    if(i-1 > 0 && j+1 <= 7 && board[i-1][j+1] == pieceCheck){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }
                    if(i+1 <= 7 && j+1 <= 7 && board[i+1][j+1] == pieceCheck ){
                        int[] arr = {i, j};
                        tiles.add(arr);
                    }
                }
            }
        }
        for(int[] e : tiles){
            ArrayList<int[]> tempArray = new ArrayList<int[]>();
            tempArray.addAll(checkAllDirections(board, e[0], e[1], piece));
            System.out.println(tempArray.size());
        }
        return tiles;
    }

    //return of player een move kan doen
    public boolean hasLegalMove(int legalMoves){
        if(legalMoves > 0){
            return true;
        }
        return false;
    }

    public void applyMove(int[][] board){

    }

    //check x, y en diagonalen op het bord
    public ArrayList<int[]> checkAllDirections(String[][] board, int row, int col, String piece){
        ArrayList<int[]> tiles = new ArrayList<int[]>();
        ArrayList<int[]> tempArray = new ArrayList<int[]>();

        //horizontaal check
        //check alle -x voor startpunt
        for(int i = col-1; i >= 0; i--){
            int[] arr = {row, i};
            if(board[row][i] != piece && board[row][i] != ""){
                tempArray.add(arr);
            }   
            if(board[row][i] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            }
            if(board[row][i] == ""){
                tempArray.clear();
                break;
            }        
        }
        //check alle +x na startpunt
        for(int i = col+1; i <= 7; i++){
            int[] arr = {row, i};
            if(board[row][i] != piece && board[row][i] != ""){
                tempArray.add(arr);
            }
            if(board[row][i] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            }
            if(board[row][i] == ""){
                tempArray.clear();
                break;
            }   
        }
        //verticaal check
        //check alle -y boven startpunt
        for(int i = row-1; i >= 0; i--){
            int[] arr = {i, col};
            if(board[i][col] != piece && board[i][col] != ""){
                tempArray.add(arr);
            }
            if(board[i][col] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            } 
            if(board[row][i] == ""){
                tempArray.clear();
                break;
            }   
        }
        //check alle +y onder startpunt
        for(int i = row+1; i<= 7; i++){
            int[] arr = {i, col};
            if(board[i][col] != piece && board[i][col] != ""){
                tempArray.add(arr);
            }
            if(board[i][col] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            }
            if(board[row][i] == ""){
                tempArray.clear();
                break;
            }   
        }
        //diagonaal check
        //-x -y check
        for(int i = col-1, j = row-1; i >= 0 && j >= 0; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tempArray.add(arr);
            }
            if(board[j][i] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            } 
            if(board[row][i] == ""){
                tempArray.clear();
                break;
            }   
        }
        //+x -y check
        for(int i = col+1, j = row-1; i <= 7 && j >= 0; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tempArray.add(arr);
            }
            if(board[j][i] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            }
            if(board[row][i] == ""){
                tempArray.clear();
                break;
            }   
        }
        //-x +y check
        for(int i = col-1, j = row+1; i >= 0 && j <= 7; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tempArray.add(arr);
            }
            if(board[j][i] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            }
            if(board[row][i] == ""){
                tempArray.clear();
                break;
            }   
        }
        //+x +y check
        for(int i = col+1, j = row+1; i <= 7 && j <= 7; i--, j--){
            int[] arr = {j, i};
            if(board[j][i] != piece && board[j][i] != ""){
                tempArray.add(arr);
            }
            if(board[j][i] == piece){
                for(int[] e : tempArray){
                    tiles.add(e);
                }
                tempArray.clear();
                break;
            }
            if(board[row][i] == ""){
                tempArray.clear();
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
