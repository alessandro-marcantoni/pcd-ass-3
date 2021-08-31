package puzzle.rmi;

public class DistributedPuzzle {

	public static void main(final String[] args) {
		final int n = 3;
		final int m = 5;
		
		final String imagePath = "res/bletchley-park-mansion.jpg";
		
		final PuzzleBoard puzzle = new PuzzleBoard(n, m, imagePath);
        puzzle.setVisible(true);
	}
}
