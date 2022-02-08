package puzzle.rmi.remote;

import puzzle.rmi.SerializableTile;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BoardObserver extends Remote, Serializable {

    void update(List<SerializableTile> tiles) throws RemoteException;

    void check() throws RemoteException;

}
