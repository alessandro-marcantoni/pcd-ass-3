package puzzle.rmi;

import java.awt.Image;

public class JTile implements Comparable<JTile>{
	private final Image image;
	private final int originalPosition;
	private int currentPosition;

    public JTile(final Image image, final int originalPosition, final int currentPosition) {
        this.image = image;
        this.originalPosition = originalPosition;
        this.currentPosition = currentPosition;
    }
    
    public Image getImage() {
    	return image;
    }
    
    public boolean isInRightPlace() {
    	return currentPosition == originalPosition;
    }
    
    public int getCurrentPosition() {
    	return currentPosition;
    }
    
    public void setCurrentPosition(final int newPosition) {
    	currentPosition = newPosition;
    }

	@Override
	public int compareTo(JTile other) {
		return Integer.compare(this.currentPosition, other.currentPosition);
	}
}
