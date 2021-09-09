package puzzle.rmi.remote;

import puzzle.rmi.DistributedPuzzle;
import puzzle.rmi.SerializableTile;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RemoteBoardImpl implements RemoteBoard {

    private List<SerializableTile> tiles;
    private final AtomicInteger id = new AtomicInteger(0);
    private final List<BoardObserver> observers = new ArrayList<>();

    @Override
    public void setTiles(List<SerializableTile> tiles) throws RemoteException {
        this.tiles = tiles;
    }

    @Override
    public void select(SerializableTile tile, List<SerializableTile> selected) throws RemoteException {
        if(selected.stream().noneMatch(t -> t.getCurrentPosition() == tile.getCurrentPosition())) {
            if (tile.getPlayer().isPresent() && playerHasAlreadySelected(tile.getPlayer().get())) {
                SerializableTile first = this.tiles.stream()
                        .filter(t -> t.getPlayer().isPresent())
                        .filter(t -> t.getPlayer().get().equals(tile.getPlayer().get()))
                        .collect(Collectors.toList())
                        .get(0);
                SerializableTile second = this.tiles.stream()
                        .filter(t -> t.getCurrentPosition() == tile.getCurrentPosition())
                        .collect(Collectors.toList())
                        .get(0);
                first.deselect();
                swap(first, second);
            } else {
                this.tiles.stream()
                        .filter(t -> t.getCurrentPosition() == tile.getCurrentPosition())
                        .collect(Collectors.toList())
                        .get(0)
                        .select(tile.getPlayer().get());
            }
        } else {
            if(tile.getPlayer().get() == (this.id.get() + DistributedPuzzle.REGISTRY_PORT)) {
                this.tiles.stream()
                        .filter(t -> t.getPlayer().isPresent())
                        .filter(t -> t.getPlayer().get().equals(tile.getPlayer().get()))
                        .filter(t -> t.getCurrentPosition() == tile.getCurrentPosition())
                        .collect(Collectors.toList())
                        .get(0)
                        .deselect();
            }
        }

        Collections.sort(this.tiles);
        this.updateObservers();
    }

    private void updateObservers() {
        this.observers.forEach(o -> {
            try {
                o.update(List.copyOf(this.tiles));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void addObserver(BoardObserver observer) throws RemoteException {
        this.observers.add(observer);
    }

    @Override
    public List<SerializableTile> getTiles() throws RemoteException {
        return List.copyOf(this.tiles);
    }

    @Override
    public int nextId() throws RemoteException {
        return this.id.incrementAndGet();
    }

    private boolean playerHasAlreadySelected(final int player) {
        return this.tiles.stream()
                .filter(SerializableTile::isSelected)
                .filter(t -> t.getPlayer().isPresent())
                .anyMatch(t -> t.getPlayer().get().equals(player));
    }

    private void swap(final SerializableTile t1, final SerializableTile t2) {
        int pos = t1.getCurrentPosition();
        t1.setCurrentPosition(t2.getCurrentPosition());
        t2.setCurrentPosition(pos);
    }
}
