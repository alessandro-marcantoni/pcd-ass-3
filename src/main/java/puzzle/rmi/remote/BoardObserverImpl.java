package puzzle.rmi.remote;

import puzzle.rmi.GameManager;
import puzzle.rmi.SerializableTile;

import java.rmi.RemoteException;
import java.util.List;

public class BoardObserverImpl implements BoardObserver {

    private final GameManager gameManager;

    public BoardObserverImpl(final GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void update(List<SerializableTile> tiles) throws RemoteException {
        this.gameManager.update(tiles);
    }

}
