package puzzle.rmi.remote;

import puzzle.rmi.PuzzleBoard;
import puzzle.rmi.SerializableTile;

import java.rmi.RemoteException;
import java.util.List;

public class BoardObserverImpl implements BoardObserver {

    private final PuzzleBoard puzzleBoard;

    public BoardObserverImpl(final PuzzleBoard puzzleBoard) {
        this.puzzleBoard = puzzleBoard;
    }

    @Override
    public void update(List<SerializableTile> tiles) throws RemoteException {
        this.puzzleBoard.updateBoard(tiles);
    }

}
