package org.naarani.ecantonchiro.b64;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class Main64 {

	public static void main( String[] args ){
		try {
			JTextArea t1 = new JTextArea();
			JTextArea t2 = new JTextArea();
			JButton b1 = new JButton();
			b1.setAction( new AbstractAction(){
				
				private static final long serialVersionUID = 1L;
	
				@Override
				public void actionPerformed(ActionEvent e) {
					String src = t1.getText();
					String ou = Base64.getEncoder().encodeToString( src.getBytes() );
					t2.setText( ou );
				}
	
			});
			b1.setText( "RUN Encode" );
			JButton b2 = new JButton();
			b2.setAction( new AbstractAction(){
				
				private static final long serialVersionUID = 1L;
	
				@Override
				public void actionPerformed(ActionEvent e) {
					String src = t2.getText();
					String ou = new String( Base64.getDecoder().decode( src ) );
					t1.setText( ou );
				}
	
			});
			b2.setText( "RUN Decode" );
			//
			JPanel p1 = new JPanel();
			p1.setLayout( new GridBagLayout() );
			GridBagConstraints r = new GridBagConstraints(); 
			r.fill = GridBagConstraints.HORIZONTAL;
			r.gridy++;
			p1.add( new JLabel( "Decoded text" ), r );
			r.gridy++;
			r.fill = GridBagConstraints.BOTH;
			r.weightx = 2;
			r.weighty = 2;
			p1.add( t1, r );
			r.fill = GridBagConstraints.HORIZONTAL;
			r.weightx = 0;
			r.weighty = 0;
			r.gridy++;
			p1.add( b1, r );
			//
			JPanel p2 = new JPanel();
			p2.setLayout( new GridBagLayout() );
			GridBagConstraints r2 = new GridBagConstraints();
			r2.fill = GridBagConstraints.HORIZONTAL;
			r2.gridy++;
			p2.add( new JLabel( "Encoded text" ), r2 );
			r2.gridy++;
			r2.fill = GridBagConstraints.BOTH;
			r2.weightx = 2;
			r2.weighty = 2;
			p2.add( t2, r2 );
			r2.fill = GridBagConstraints.HORIZONTAL;
			r2.weightx = 0;
			r2.weighty = 0;
			r2.gridy++;
			p2.add( b2, r2 );
			//
			final JFrame f = new JFrame();
			f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			f.setTitle( "base64" );
				
			URL url = Main64.class.getResource( "hands.jpg" ).toURI().toURL();
			Image img = ImageIO.read( url );
			f.setIconImage( img );
			
			f.getContentPane().setLayout( new GridBagLayout() );
			GridBagConstraints rx = new GridBagConstraints(); 
			rx.fill = GridBagConstraints.BOTH;
			rx.weightx = 2;
			rx.weighty = 2;
			rx.gridx = 0 ;
			rx.gridy = 0 ;
			f.getContentPane().add( p1, rx );
			rx.gridy ++ ;
			f.getContentPane().add( p2, rx );
			f.setSize( 300, 300 );
			f.setLocation( 100, 100 );
			//
			f.addWindowStateListener(new WindowStateListener(){
	
				public void windowStateChanged( WindowEvent e ){
					if( ( e.getNewState() & Frame.ICONIFIED ) == Frame.ICONIFIED ){
						f.setVisible( false );
					}
				}
	
			});
			if( SystemTray.isSupported() ){
		        try {
					final PopupMenu popup = new PopupMenu();
					//
					final TrayIcon trayIcon = new TrayIcon( img , "Ecatonchiro B64" );
			        final SystemTray tray = SystemTray.getSystemTray();
			        MenuItem b64 = new MenuItem( "B64 show" );
			        b64.addActionListener( new ActionListener() {
	
						@Override
						public void actionPerformed( ActionEvent e ){
							f.setVisible( true );
			    			f.setAlwaysOnTop( true );
			    			f.setState ( Frame.NORMAL );
						}
	
					} );
			        MenuItem exitItem = new MenuItem( "Exit" );
			        exitItem.addActionListener( new ActionListener() {
	
						@Override
						public void actionPerformed( ActionEvent e ){
							System.exit( 0 );
						}
	
					} );
			        popup.add( b64 );
			        popup.add( exitItem );
			        
			        trayIcon.setPopupMenu( popup );
			        ActionListener listener = new ActionListener(){
			        	
			            public void actionPerformed( ActionEvent e ){
			    			f.setVisible( true );
			    			f.setAlwaysOnTop( true );
			    			f.setState ( Frame.NORMAL );
			            }

			        };
			        trayIcon.addActionListener( listener );
			        tray.add( trayIcon );
		        } catch ( Exception e ){
		            System.out.println("TrayIcon could not be added.");
		        }
	        }
			f.setAlwaysOnTop( true );
			f.setVisible( true );
		} catch (Exception  e ){
			e.printStackTrace();
		}
	}

}
