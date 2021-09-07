package puzzle.rmi.remote;

import puzzle.rmi.SerializableTile;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteBoard extends Remote {

    void select(SerializableTile tile) throws RemoteException;

    void setTiles(List<SerializableTile> tiles) throws RemoteException;

    void addObserver(BoardObserver observer) throws RemoteException;

    List<SerializableTile> getTiles() throws RemoteException;

    int nextId() throws RemoteException;
}
