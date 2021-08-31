package puzzle.rmi;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;

public class JTileButton extends JButton{

	public JTileButton(final JTile JTile) {
		super(new ImageIcon(JTile.getImage()));
		
		addMouseListener(new MouseAdapter() {            
            @Override
            public void mouseClicked(MouseEvent e) {
            	setBorder(BorderFactory.createLineBorder(Color.red));
            }
        });
	}
}
