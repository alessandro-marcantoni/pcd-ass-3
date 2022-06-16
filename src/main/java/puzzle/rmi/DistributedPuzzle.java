package puzzle.rmi;

import puzzle.rmi.remote.RemoteBoard;
import puzzle.rmi.remote.RemoteBoardImpl;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class DistributedPuzzle {

	public static final int REGISTRY_PORT = 1099;
	public static final int ROWS = 3;
	public static final int COLS = 5;
	public static final String IMAGE_PATH = "res/bletchley-park-mansion.jpg";

	public static void main(final String[] args) throws RemoteException, NotBoundException {

		if (args.length > 0) {
			LocateRegistry.createRegistry(REGISTRY_PORT);

			RemoteBoard board = new RemoteBoardImpl();
			RemoteBoard boardStub = (RemoteBoard) UnicastRemoteObject.exportObject(board, 0);

			Registry registry = LocateRegistry.getRegistry(REGISTRY_PORT);
			registry.rebind("remoteBoard", boardStub);

			new GameManager(REGISTRY_PORT, board);
		} else {
			Registry registry = LocateRegistry.getRegistry(REGISTRY_PORT);

			// RETRIEVE REMOTE BOARD
			RemoteBoard board = (RemoteBoard) registry.lookup("remoteBoard");
			RemoteBoard remoteBoardStub = (RemoteBoard) UnicastRemoteObject.exportObject(board, 0);

			final int port = REGISTRY_PORT + board.nextId();
			LocateRegistry.createRegistry(port);
			LocateRegistry.getRegistry(port).rebind("remoteBoard", remoteBoardStub);

			new GameManager(port, remoteBoardStub);
		}
	}
}
