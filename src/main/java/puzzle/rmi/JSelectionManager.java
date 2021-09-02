package puzzle.rmi;

import java.rmi.RemoteException;

public class JSelectionManager {

	private boolean selectionActive = false;
	private JTile selectedJTile;

	public void selectTile(final JTile tile, final Listener listener) throws RemoteException {
		
		if(selectionActive) {
			selectionActive = false;
			
			swap(selectedJTile, tile);
			
			listener.onSwapPerformed();
		} else {
			selectionActive = true;
			selectedJTile = tile;
		}
	}

	private void swap(final JTile t1, final JTile t2) {
		int pos = t1.getCurrentPosition();
		t1.setCurrentPosition(t2.getCurrentPosition());
		t2.setCurrentPosition(pos);
	}
	
	@FunctionalInterface
	public interface Listener{
		void onSwapPerformed() throws RemoteException;
	}
}
