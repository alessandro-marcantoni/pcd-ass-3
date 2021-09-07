package puzzle.rmi;

import puzzle.rmi.remote.RemoteBoard;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PuzzleBoard extends JFrame implements Remote {
	
	final int rows, columns;
	private List<JTile> tiles;
	private RemoteBoard remoteBoard;
	private final int id;

    final JPanel board = new JPanel();
	
    public PuzzleBoard(final int rows, final int columns, final int id) {
    	this.rows = rows;
		this.columns = columns;
		this.id = id;
    	
    	setTitle("Puzzle");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        

        this.board.setBorder(BorderFactory.createLineBorder(Color.gray));
        this.board.setLayout(new GridLayout(rows, columns, 0, 0));
        getContentPane().add(this.board, BorderLayout.CENTER);
        
        //createTiles();
        //paintPuzzle(board);
    }

    public PuzzleBoard(final int id) {
        this(DistributedPuzzle.ROWS, DistributedPuzzle.COLS, id);
    }
    
    public void createTiles(final List<SerializableTile> t) {
		final BufferedImage image;
        try {
            image = ImageIO.read(new File(DistributedPuzzle.IMAGE_PATH));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load image", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int imageWidth = image.getWidth(null);
        final int imageHeight = image.getHeight(null);
        int position = 0;

        final List<Integer> positions;
        if (t.isEmpty()) {
            positions = new ArrayList<>();
            IntStream.range(0, rows*columns).forEach(positions::add);
            Collections.shuffle(positions);
        } else {
            positions = t.stream()
                    .sorted()
                    .map(SerializableTile::getCurrentPosition)
                    .collect(Collectors.toList());
        }

        this.tiles = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
            	final Image imagePortion = createImage(new FilteredImageSource(image.getSource(),
                        new CropImageFilter(j * imageWidth / columns, 
                        					i * imageHeight / rows, 
                        					(imageWidth / columns), 
                        					imageHeight / rows)));

                this.tiles.add(new JTile(imagePortion, position, positions.get(position)));
                position++;
            }
        }
	}

	public List<JTile> getTiles() {
        return List.copyOf(this.tiles);
    }
    
    public void paintPuzzle() {
    	this.board.removeAll();
    	
    	Collections.sort(this.tiles);
    	
    	this.tiles.forEach(t -> {
    		final JTileButton btn = new JTileButton(t);
            board.add(btn);
            btn.setBorder(BorderFactory.createLineBorder(Color.gray));
            btn.addActionListener(actionListener -> {
                try {
                    this.remoteBoard.select(
                            new SerializableTile(
                                    t.getOriginalPosition(),
                                    t.getCurrentPosition(),
                                    true,
                                    this.id));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
    	});
    	
    	pack();
    	this.setVisible(true);
        //setLocationRelativeTo(null);
    }

    public void setRemoteBoard(final RemoteBoard remoteBoard) {
        this.remoteBoard = remoteBoard;
    }

    public void updateBoard(List<SerializableTile> newTiles) {
        this.tiles.forEach(t -> t.setCurrentPosition(newTiles.stream()
                        .filter(st -> st.getOriginalPosition() == t.getOriginalPosition())
                        .collect(Collectors.toList())
                        .get(0)
                        .getCurrentPosition()));
        paintPuzzle();
        checkSolution();
    }

    private void checkSolution() {
    	if(this.tiles.stream().allMatch(JTile::isInRightPlace)) {
    		JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE);
    	}
    }
}
