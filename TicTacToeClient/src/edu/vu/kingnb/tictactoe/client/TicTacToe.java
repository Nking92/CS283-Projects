package edu.vu.kingnb.tictactoe.client;

public class TicTacToe {

	private static final char EMPTY_SYMBOL = 'e';
	private static final char TIE_SYMBOL = 't';

	/** The symbol that owns the current turn */
	private char mTurn = 'O';
	private char mWinner = EMPTY_SYMBOL;
	private char[][] mBoard = new char[3][3];

	public TicTacToe() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				mBoard[i][j] = EMPTY_SYMBOL;
			}
		}
	}

	public char getTurn() {
		return mTurn;
	}

	public boolean isEmpty(int i, int j) {
		return mBoard[i][j] == 'e';
	}

	public boolean makeTurn(int i, int j) {
		if (!isEmpty(i, j) || isGameOver()) {
			return false;
		} else {
			mBoard[i][j] = mTurn;
			swapTurns();
			return true;
		}
	}

	public char getSymbol(int i, int j) {
		return mBoard[i][j];
	}

	private void swapTurns() {
		if (getTurn() == 'O') {
			mTurn = 'X';
		} else {
			mTurn = 'O';
		}
	}

	public boolean isGameOver() {
		if (mWinner != EMPTY_SYMBOL)
			return true;
		// Check rows and cols
		for (int i = 0; i < 3; i++) {
			if (isMatchingRow(i) && !isEmpty(i, 0)) {
				mWinner = mBoard[i][0];
				return true;
			} else if (isMatchingCol(i) && !isEmpty(0, i)) {
				mWinner = mBoard[0][i];
				return true;
			}
		}
		// Check diagonals
		if (mBoard[0][0] == mBoard[1][1] && mBoard[1][1] == mBoard[2][2]
				&& !isEmpty(0, 0)) {
			mWinner = mBoard[0][0];
			return true;
		} else if (mBoard[0][2] == mBoard[1][1] && mBoard[1][1] == mBoard[2][0]
				&& !isEmpty(0, 2)) {
			mWinner = mBoard[0][2];
			return true;
		}
		// Check for a tie
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (mBoard[i][j] == EMPTY_SYMBOL)
					return false;
			}
		}
		// There's a tie
		mWinner = TIE_SYMBOL;
		return true;
	}

	public boolean isTie() {
		return isGameOver() && getWinner() == TIE_SYMBOL;
	}

	private boolean isMatchingRow(int row) {
		return mBoard[row][0] == mBoard[row][1]
				&& mBoard[row][1] == mBoard[row][2];
	}

	private boolean isMatchingCol(int col) {
		return mBoard[0][col] == mBoard[1][col]
				&& mBoard[1][col] == mBoard[2][col];
	}

	public char getWinner() {
		return mWinner;
	}

}