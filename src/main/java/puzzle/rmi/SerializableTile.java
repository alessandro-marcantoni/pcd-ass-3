package puzzle.rmi;

import java.io.Serializable;
import java.util.Optional;

public class SerializableTile implements Comparable<SerializableTile>, Serializable {

    private final int originalPosition;
    private int currentPosition;
    private boolean selected;
    private Integer player;

    public SerializableTile(final int originalPosition, final int currentPosition,
                            final boolean selected, final Integer player) {
        this.originalPosition = originalPosition;
        this.currentPosition = currentPosition;
        this.selected = selected;
        this.player = player;
    }

    public SerializableTile(JTile t) {
        this(t.getOriginalPosition(), t.getCurrentPosition(), false, null);
    }

    public int getOriginalPosition() {
        return originalPosition;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public boolean isSelected() {
        return selected;
    }

    public Optional<Integer> getPlayer() {
        return this.player == null ? Optional.empty() : Optional.of(this.player);
    }

    public boolean isInRightPlace() {
        return this.currentPosition == this.originalPosition;
    }

    public void select(Integer player) {
        this.selected = true;
        this.player = player;
    }

    public void deselect() {
        this.selected = false;
        this.player = null;
    }

    @Override
    public int compareTo(SerializableTile o) {
        return Integer.compare(this.originalPosition, o.getOriginalPosition());
    }
}
