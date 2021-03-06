package puzzle.rmi;

import puzzle.rmi.remote.BoardObserver;
import puzzle.rmi.remote.BoardObserverImpl;
import puzzle.rmi.remote.RemoteBoard;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameManager {

    private final PuzzleBoard puzzle;
    private final RemoteBoard remoteBoard;
    private final int id;

    public GameManager(int id, RemoteBoard remoteBoard) throws RemoteException {
        this.id = id;
        this.remoteBoard = remoteBoard;
        this.puzzle = new PuzzleBoard(this);

        final BoardObserver observer = new BoardObserverImpl(this);
        UnicastRemoteObject.exportObject(observer, 0);
        this.remoteBoard.addObserver(observer);

        if (this.id == DistributedPuzzle.REGISTRY_PORT) {
            this.remoteBoard.setTiles(this.puzzle.getTiles().stream().map(SerializableTile::new).collect(Collectors.toList()));
        }
    }

    public void update(final List<SerializableTile> tiles) {
        this.puzzle.updateBoard(tiles);
    }

    public int getId() {
        return this.id;
    }

    public void select(SerializableTile tile) {
        try {
            this.remoteBoard.select(tile);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<SerializableTile> getRemoteTiles() throws RemoteException {
        return List.copyOf(this.remoteBoard.getTiles());
    }

}
