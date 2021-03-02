import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.lang.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.*;

public class Simulation{
	private File[] files;
	private String dbName;

	//<-[ Creates a list media files needed in simulation ]->
	public void getMediaFiles(){
		File f = new File("media");
		this.files = f.listFiles();
	}


	//<-[ Creates SQLite Database ]->
	public void createDatabase(String db_file) {
		this.dbName = db_file;
		String url = "jdbc:sqlite:database/"+db_file;
		try {
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection(url);
			conn.close();
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}


	//<-[ Creates Database Directory ]
	public void createDirectory(String dir){
		File file = new File(dir);
		boolean bool = file.mkdir();
	}


	public File[] getFiles(){
		return this.files;
	}


	public String getDbName(){
		return this.dbName;
	}


	//<-[ Creates Connection to SQLite database ]->
	public Connection createConnection() {
                Connection conn = null;
                try {
                        String url = "jdbc:sqlite:database/" + this.dbName;
                        conn = DriverManager.getConnection(url);
                }
                catch (SQLException e){
                        System.out.println(e.getMessage());
                }

                return conn;
        }


        //<-[ Creates Tables(FLOORS and ELEVATOR) in the database ]->
        public void createTables(){
                String[] drop_table = new String[2];
                drop_table[0] = "DROP TABLE IF EXISTS FLOORS;";
                drop_table[1] = "DROP TABLE IF EXISTS ELEVATOR;";

                String[] table = new String[2];
                table[0] = "CREATE TABLE IF NOT EXISTS FLOORS (\n"
                        +"      NUMBER INTEGER AUTO_INCREMENT PRIMARY KEY,\n"
                        +"      NAME TEXT NOT NULL\n"
                        +");";

                table[1] = "CREATE TABLE IF NOT EXISTS ELEVATOR (\n"
                        +"      ID INTEGER AUTO_INCREMENT PRIMARY KEY,\n"
                        +"      PICTURE_PATH  TEXT NOT NULL,\n"
                        +"      DESCRIPTION TEXT NOT NULL,\n"
                        +"      NAME TEXT NOT NULL\n"
                        +");";

                try(Connection conn = this.createConnection()){
                        Statement query = conn.createStatement();
                        for(String i:drop_table){
                                query.execute(i);
                        }

                        for(String j:table){
                                query.execute(j);
                        }
                        conn.close();
                }catch(SQLException e){
                        System.out.println(e.getMessage());
                }
        }


	//<-[ Inserts information about files in the media folder into the ELEVATOR table ]->
        public void insertElevator(){
                String sql = "INSERT INTO ELEVATOR (ID,PICTURE_PATH,DESCRIPTION,NAME) VALUES(?,?,?,?)";
                try(Connection conn = this.createConnection();PreparedStatement pstmt = conn.prepareStatement(sql)){
			int id = 0;
                        for(File file:this.files){
				String[] splitName = file.getName().split(".png",2);
				pstmt.setInt(1, id+=1);
                                pstmt.setString(2, file.getAbsolutePath());
                                pstmt.setString(3, file.getName()+" is part of the media files needed to run the simulation");
                                pstmt.setString(4, splitName[0]);

                       		pstmt.executeUpdate();
			}
			conn.close();
                }catch(SQLException e){
                        System.out.println(e.getMessage());
                }
	}

	//<-[ Inserts the names of each floor into the FLOORS table ]->
	public void insertFloors(){
		String sql = "INSERT INTO FLOORS (NUMBER,NAME) VALUES(?,?)";
		try(Connection conn = this.createConnection();PreparedStatement pstmt = conn.prepareStatement(sql)){
			int number = 0;
			String[] floors = {"Ground Floor","1st Floor","2nd Floor","3rd Floor","4th FLoor","5th FLoor","6th FLoor","7th FLoor","8th FLoor","9th FLoor","10th FLoor","11th FLoor","12th FLoor","13th FLoor","14th FLoor"};
			for(String floor:floors){
				pstmt.setInt(1, number+=1);
				pstmt.setString(2, floor);

				pstmt.executeUpdate();
			}
			conn.close();
		}catch(SQLException e){
                        System.out.println(e.getMessage());
                }
	}


	//<-[ Returns the absolute path of a picture in the media directory using it's NAME from the FLOORS table ]->
	public String selectPicture(String pic){
        	String sql = "SELECT PICTURE_PATH FROM ELEVATOR WHERE NAME is '"+pic+"'";
		String path = null;
        	try (Connection conn = this.createConnection();
			Statement stmt  = conn.createStatement();
			ResultSet rs    = stmt.executeQuery(sql)){

			path = rs.getString("PICTURE_PATH");
		}catch (SQLException e){
		System.out.println(e.getMessage());
		}

		return path;

	}

	//<-[ Where it all begins ]->
	public static void main(String[] args){
		Simulation app = new Simulation();

                app.createDirectory("database");
                app.createDatabase("simulation.db");
                app.getMediaFiles();
                app.createTables();
                app.insertElevator();
		app.insertFloors();
		new GUI(app).createGUI();
	}
}

//<-[ Class to handle the graphics and the simulations ]->
public class GUI {
	int currentFloor = 0;
	int nextFloor = 0;
	int[] movesArr = new int[] {0,1,2,3,4,5,6,7,8,9,10,11};
	int numberOfButtons = 12;
	Animation anime = new Animation();
	JFrame frame = new JFrame("ELEVATOR SIMULATION");
	JPanel buttonPanel = new JPanel();
	GridLayout grid = new GridLayout(0, 3) {{ setHgap(5); setVgap(5); }};
	JButton button[] = new JButton[numberOfButtons];
	JButton open, close;
	boolean closed = true;
	boolean opened = true;
	Listener listener = new Listener();
	Color buttonColor;
	Simulation appM;
	String sql = "SELECT NAME FROM FLOORS";
	Statement stmt;
	ResultSet rs;
	JButton upbutton = new RoundButton("");
	JButton downbutton = new RoundButton("");
	JLabel up = new JLabel("Up");

	JPanel floorPanel = new JPanel();
	JLabel floors = new JLabel(String.valueOf(currentFloor));
	JPanel img;
	JLabel down = new JLabel("DOWN");
	JPanel pnlButton = new JPanel();


	public GUI(Simulation app) {
		appM = app;
		try{
			stmt = appM.createConnection().createStatement();
			rs    = stmt.executeQuery(sql);
		}catch(SQLException e){

		}

		//Setup frame
		frame.setLayout(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Create button panel
		buttonPanel.setLayout(grid);
		buttonPanel.setPreferredSize(new Dimension(1, 1));;
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		addButtonPanel();
	}


	public void centerFrame() {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation((dim.width - frame.getWidth()) / 2,
		(dim.height - frame.getHeight()) / 2);
	}


	public void addButtonPanel() {

		for (int i=0; i < button.length; i++){
			try{
				rs.next();
				button[i] = new JButton();
				button[i].addActionListener(listener.new ButtonListener());
				button[i].setText(rs.getString("NAME"));
				button[i].setName(new Integer(i).toString());
				buttonPanel.add(button[i]);
			}catch(SQLException e){

			}
		}
		open = new JButton();
                open.addActionListener(listener.new ButtonListener());
                open.setText("OPEN");
                buttonPanel.add(open);

                close = new JButton();
                close.addActionListener(listener.new ButtonListener());
                close.setText("CLOSE");
                buttonPanel.add(close);
		buttonColor = button[0].getBackground();

		upbutton.addActionListener(listener.new ButtonListener());
		downbutton.addActionListener(listener.new ButtonListener());
		pnlButton.setLayout(null);
		pnlButton.add(upbutton);
		pnlButton.add(downbutton);
		pnlButton.add(up);
		pnlButton.add(down);

		upbutton.setBounds(0,0,20,20);
		downbutton.setBounds(0,21,20,20);

		up.setBounds(20,0,20,20);
		down.setBounds(20,21,50,20);

		floorPanel.add(floors);
                floors.setBounds(0,0,20,20);
                frame.getContentPane().add(floorPanel);
                floorPanel.setBounds(295,0,20,20);
	}

	//<-[ ]->
	public class RoundButton extends JButton {
		public RoundButton(String label) {
			super(label);
			super.setBackground(Color.gray);
			Dimension size = getPreferredSize();
			size.width = size.height = 30;
			setPreferredSize(size);

			setContentAreaFilled(false);
		}

		protected void paintComponent(Graphics g) {
			if (getModel().isArmed()) {
				g.setColor(Color.lightGray);
			} else {
				g.setColor(getBackground());
			}
			g.fillOval(0, 0, getSize().width-1,getSize().height-1);
			super.paintComponent(g);
		}

		protected void paintBorder(Graphics g) {
			g.setColor(getForeground());
			g.drawOval(0, 0, getSize().width-1,     getSize().height-1);
		}

		Shape shape;
		public boolean contains(int x, int y) {
			if (shape == null || !shape.getBounds().equals(getBounds())) {
				shape = new Ellipse2D.Float(0, 0, getWidth(), getHeight());
			}
			return shape.contains(x, y);
		}
	}

	//<-[ Method to setup the UI of the simulation ]->
	public void createGUI(){
		frame.add(anime);
		anime.setBounds(0,0,600,300);

		frame.add(buttonPanel,BorderLayout.SOUTH);
		buttonPanel.setBounds(0,300,690,270);

		frame.getContentPane().add(pnlButton);
		pnlButton.setBounds(600,150,90,90);

		frame.setPreferredSize(new Dimension(700, 600));
		frame.pack();
		centerFrame();
		buttonPanel.setVisible(true);
		frame.setVisible(true);
	}


	//<-[ Listener for each button in the simulation ]->
	public class Listener {

		public class ButtonListener implements ActionListener {
			public void actionPerformed(ActionEvent event){
				final JButton cbutton = (JButton) event.getSource();
				Thread thread = new Thread(){
					public void run(){
						OpenOrClose(cbutton);
						BlinkThread(cbutton, Color.red);
						UpDown(cbutton);
					}
				};
				thread.start();
			}
		}

	}

	//<-[ Method to implement the pattern movement of the button panel ]->
	private void BlinkThread(JButton currentButton, Color color){
		ArrayList<Integer> movement = UpOrDown(currentButton);
		if (movement.size() > 1 ){
			for(int i: movement){
				anime.close();
				labelFloor(i);
				if(button[i]==currentButton){
					try{
						System.out.print("\n<--[ Destined Floor: "+i+" ]-->\n");
						Thread.sleep(400);
						button[i].setBackground(Color.green);
						currentFloor = i;
						anime.open();
						labelFloor(i);
						break;
					}catch(Exception e){

					}
				}
				try{
					if(movement.get(1) > movement.get(0)){
						paintUp();
					}else{
						paintDown();
					}
					Thread.sleep(400);
					button[i].setBackground(color);
					System.out.print("\nCurrent Floor: "+i+"\n");
					Thread.sleep(400);
					button[i].setBackground(buttonColor);
				}catch(Exception ex){
					System.out.println("There is an error ooo!");
				}
			}
		}
	}

	//<-[ Method in the listener to handle the open and close buttons ]->
	private void OpenOrClose(JButton currentButton){
		if(open == currentButton){
			anime.open();
			labelFloor(currentFloor);
				    }
		if(close == currentButton){
			anime.close();
			labelFloor(currentFloor);
		}
	}

	//<-[ Method in the listener to handle the up and down buttons ]->
	private void UpDown(JButton currentButton){
		if (upbutton == currentButton){
			anime.open();
			paintUp();
		}
		if (downbutton == currentButton){
			anime.open();
			paintDown();
		}
	}


	//<-[ Method to fix up arrow in the simulation ]->
	private void paintUp(){
		try{
			img = new ImagePanel(ImageIO.read(new File(appM.selectPicture("up"))));
			frame.getContentPane().add(img);
			img.setBounds(320,0,20,20);
		}catch(Exception e){
		}
	}


	//<-[ Method to indicate the current floor number ]->
	private void labelFloor(int floor){
		floors.setText(String.valueOf(floor));
	}


	//<-[ Method to fix down arrow in the simulation ]->
	private void paintDown(){
                try{
                        img = new ImagePanel(ImageIO.read(new File(appM.selectPicture("down"))));
                        frame.getContentPane().add(img);
                        img.setBounds(320,0,20,20);
                }catch(Exception e){
                }
        }


	//<-[ Method to return an ArrayList containing the floor numbers the elevator is supposed to move to after the press of a button ]->
	private ArrayList UpOrDown(JButton cb){
		int movement[] = new int[] {};
		ArrayList<Integer> move = new ArrayList<>();
		for(int i: movesArr){
			if(button[i]==cb){
				nextFloor = i;
				break;
			}
		}
		if(nextFloor > currentFloor){
			for(int i = currentFloor; i <= nextFloor; i++){
				move.add(i);
			}
		}else{
			for(int i = currentFloor; i >= nextFloor; i--){
				move.add(i);
			}
		}
		return move;
	}

	//<-[ Class to load and resize image unto a panel ]->
	public class ImagePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private Image img;
		private Image scaled;

		public ImagePanel(String img) {
			this(new ImageIcon(img).getImage());
		}

		public ImagePanel(Image img) {
			this.img = img;
		}

		@Override
		public void invalidate() {
			super.invalidate();
			int width = getWidth();
			int height = getHeight();

			if (width > 0 && height > 0) {
				scaled = img.getScaledInstance(20, 20,
				Image.SCALE_SMOOTH);
			}
		}


		@Override
		public Dimension getPreferredSize() {
			return img == null ? new Dimension(200, 200) : new Dimension(
			img.getWidth(this), img.getHeight(this));
		}


		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(scaled, 0, 0, null);
		}
	}

	//<-[ Class to draw elevator components onto the frame and to create an animation]->
	public class Animation extends JPanel{
		private int x = 0, y = 450, vel = 2;

		public Animation(){
			setVisible(true);
		}

		public void paintComponent(Graphics g){
			super.paintComponent(g);
			g.setColor(Color.DARK_GRAY);
			g.fillRect(x, 30, 152, 270);

		        // RIGHT DOOR
			g.setColor(Color.DARK_GRAY);
			g.fillRect(y, 30, 152, 270);

			// LEFT STRUCTURE
			g.setColor(Color.GRAY);
			g.fillRect(0, 0, 150, 300);

        		// RIGHT STRUCTURE
			g.setColor(Color.GRAY);
			g.fillRect(450, 0, 150, 300);

			floors.setText(String.valueOf(currentFloor));
		}

		//<-[ Method to create opening animation ]->
		public void open(){
			while(closed){
				try{
	    				Thread.sleep(50);
					if(x >= 0){
						x -= vel;
					}
					repaint();
					pnlButton.setBounds(600,150,90,50);
					floors.setText(String.valueOf(currentFloor));

					if (y <= 450){
						y += vel;
					}
					floors.setText(String.valueOf(currentFloor));
					pnlButton.setBounds(600,150,90,50);
					repaint();

					if (x <= 1){
						opened = true;
						closed = false;
					}
				}catch(Exception e){

				}
			}
			floors.setText(String.valueOf(currentFloor));
		}

		//<-[ Method to create closing animation ]->
		public void close(){
			while(opened){
				try{
					Thread.sleep(50);
					if(x <= 146){
						x += vel;
					}
					floors.setText(String.valueOf(currentFloor));
					pnlButton.setBounds(600,150,90,90);
					repaint();

					if (y >= 300){
						y -= vel;
					}
					repaint();
					pnlButton.setBounds(600,150,90,90);
					floors.setText(String.valueOf(currentFloor));

					if(y <= 301){
						closed = true;
						opened = false;
					}
				}catch(Exception e){

				}
				floors.setText(String.valueOf(currentFloor));
			}

		}
	}
}

/*
                               ,-""  `.      <--[ J.O-S ]-->
                             ,'  _   e )`-._
                            /  ,' `-._<.===-'
                           /  /
                          /  ;
              _          /   ;
 (`._    _.-"" ""--..__,'    |
 <_  `-""                     \\
  <`-                          :
   (__   <__.                  ;
     `-.   '-.__.      _.'    /
        \      `-.__,-'    _,'
         `._    ,    /__,-'
         *  ""._\__,'< <____
        *        | |  `----.`.
       *         | |        \ `.
[  Group 18 ]  ; |___      \-``
                 \   --<
                  `.`.<
                    `-'
	*/
