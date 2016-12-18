package ref;
/*
 *     Updated by Matthew Shatley and Chris Hoffman
 *     for Professor Paul Amer (amer@udel.edu)
 *     University of Delaware (2008)
 
 *    Java Applet Demonstration of Selective Repeat Protocol.
 *    Coded by Shamiul Azom as  project assigned by 
 *    Prof. Martin Reisslein, Arizona State University
 *    Course No. EEE-459/591. Spring 2001
 
 
 *     This Applet was designed to be used in conjunction with
 *     "Computer Networking: A Top Down Approach"
 *     by James Kurose & Keith Ross.
 *     Terminology and specifications are based upon their description of the
 *     Selective Repeat protocol in chapter 3, section 4.
 
 
 *     A note on magic numbers: Magic numbers are horrible to have in your code in general.
 *     However, the graphics components of this applet provided no good way to remove the
 *     magic numbers from the code as locations for objects are specified in pixel coordinates. 
 *     We apologize in advance for any confusion this may cause in reading the code. 
 *     
 */

import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SelectiveRepeat extends Applet implements ActionListener, Runnable {
    
    private static final int ADVANCE_PACKET = 5;
    // Default values of parameters for animation
    // sender_window_len_def the sender can have a maximum of 5 outstanding
    // un-acknowledged packets
    final int sender_window_len_def = 5;
    // how many packets the receiver can hold in memory without delivering data
    // in the case of SelectiveRepeat we can hold 1(or the current packet) in
    // memory
    // if another packet arrives the one in memory is discarded
    final int receiver_window_len = 5;
    // GUI components to describe how the Simulation should be drawn
    final int pack_width_def = 10;
    final int pack_height_def = 30;
    final int h_offset_def = 100;
    final int v_offset_def = 50;
    final int v_clearance_def = 300;
    
    // used for timeout values, thread.sleep() is specified in milliseconds
    // so we convert to seconds for timeout processing.(1000 milliseconds = 1
    // second)
    final int TIMEOUT_MULTIPLIER = 1000;
    
    final int MIN_FPS = 3;
    final int FPS_STEP = 2;
    final int DESELECTED = -1;
    final int DEFAULT_FPS = 5;
    // default to 20 Packets if no value is supplied
    final int total_Packet_def = 20;
    // 25 sec default timeout for retransmissions
    final int time_out_sec_def = 25;
    
    // Default colors of different Packets
    // these have been matched as closely to the the text as possible
    // Order of color values Red, Green, Blue
    final Color unack_color = new Color(204, 230, 247);
    final Color ack_color = Color.yellow;
    final Color sel_color = Color.green;
    final Color roam_pack_color = new Color(204, 230, 247);
    final Color roam_ack_color = Color.yellow;
    final Color dest_color = Color.red;
    final Color received_ack = new Color(37, 135, 234);
    
    // base - our sending base - the next expected Packet to be received
    // nextseqnum - the next sequence number that will be given to a newly
    // created Packet
    // selected - the index of the currently selected Packet in transmission
    // lastKnownSucPacket - LAST KNOWN SUCcessful PACKET received by receiving
    // node
    int base, receiver_base, nextseqsum, fps, selected = DESELECTED, timeout,
	timeoutPacket, lastKnownSucPacket;
    boolean timerFlag, timerSleep;
    
    // define our buttons for actions available to be taken by the user
    Button send, stop, fast, slow, kill, reset;
    /*
     * 2 threads run for the applet gbnTread - runs to create our animation and
     * process Packets timerThread - created and sleeps for a specified period
     * of time. On wake up performs timeout processing A timeout causes all of
     * the outstanding Packets to be re-transmitted. NOTE: The text(Computer
     * Networking: A Top Down Approach) specified a per Packet timer, however
     * this is rarely implemented as there is a significant overhead in using
     * that many timers. Logically, the only Packet that would ever timeout is
     * the left most edge of the sending window as this has been in transmission
     * the longest. Since a per Packet timer system is not implemented in
     * practice we have simulated per Packet timers per the books description
     * while using only a single timer.
     */
    Thread gbnThread, timerThread;
    
    TextArea output; // output variable used to write information in the text
    // box
    Dimension offDimension;
    Image offImage; // implements double buffering to proved a smoother
    // animation
    Graphics offGraphics; // graphics component used for drawing
    SelectiveRepeatPacket sender[]; // sender array - holds the Packets being sent
    
    // Declaring properties of our window
    int window_len, pack_width, pack_height, h_offset, v_offset, v_clearance,
	total_Packet, time_out_sec;
    
    /***************************************************************************
     * Method init *
     * ************************************************************************
     * Purpose: init method to set up applet for running - first method called
     * on loading the code. Attempts to load parameters passed from HTML code
     * contained in the website. If there is an error or no parameters are
     * provided then the default values(declared above) are used. Global
     * variables used: sender - array holding the Packets and the corresponding
     * acks for the Packets sent in the applet output - console window for
     * applet activities & messages
     **************************************************************************/
    public void init() {
	
	// prevents layout manager from adjusting components in the applet
	// The buttons made it easier to deal with pixel coordinates
	// than recode for layout manager
	setLayout(null);
	output = new TextArea(150, 150); // setup output box
	// create text area for console output box
	output.setBounds(0, 400, 650, 250); // set bounds for output box
	output.setEditable(false); // prevent user from editing output written
	// to console
	add(output); // tell applet to draw our output box
	
	setupSimulationParams();
	
	base = 0; // Defining our base to be 0 the first Packet number
	// expected
	receiver_base = 0; // Set the receiver base number to 0, which is the
	// first index in the receiver array
	nextseqsum = 0; // Defining next sequence number for next Packet sent.
	fps = 5; // Defining default Frame per Second for our animation
	
	// create a shared array, used for both the sender and the receiver
	// nodes.
	// all Packets will be created and processed from this array
	sender = new SelectiveRepeatPacket[total_Packet];
	
	// Defining the buttons - creates the button and text to go on the
	// button
	send = new Button("Send New");
	// set the command to be performed when button is pressed this command
	// is used
	// to determine which button was pressed in the actionPerformed method
	send.setActionCommand("rdt");
	// on button pushed the actionPerformed method of this class is called
	// and appropriate action is taken depending on the button pressed
	send.addActionListener(this);
	// set the size and location of this button (of form (x, y, width,
	// length) - this is specified in pixel coordinates
	send.setBounds(0, 0, 90, 20);
	
	// same structure as above
	stop = new Button("Pause");
	stop.setActionCommand("stopanim");
	stop.addActionListener(this);
	stop.setBounds(90, 0, 90, 20);
	
	fast = new Button("Faster");
	fast.setActionCommand("fast");
	fast.addActionListener(this);
	fast.setBounds(180, 0, 90, 20);
	
	slow = new Button("Slower");
	slow.setActionCommand("slow");
	slow.addActionListener(this);
	slow.setBounds(270, 0, 90, 20);
	
	kill = new Button("Kill Packet/Ack");
	kill.setActionCommand("kl");
	kill.addActionListener(this);
	kill.setEnabled(false);
	kill.setBounds(360, 0, 90, 20);
	
	reset = new Button("Reset");
	reset.setActionCommand("rst");
	reset.addActionListener(this);
	reset.setBounds(450, 0, 90, 20);
	
	// Adding the buttons to our applet window so they can be rendered and
	// used
	add(send);
	add(stop);
	add(fast);
	add(slow);
	add(kill);
	add(reset);
	
	// print out message about the new authors of the code
	output.append("-- SelectiveRepeat Applet\n");
	output.append("-- Written By Matt Shatley & Chris Hoffman under the advisement of Professor Paul Amer\n");
	output.append("-- University of Delaware, 2008\n\n");
	
	// tell user we are ready to begin demonstrating Go Back N
	output.append("-Ready to run. Press 'Send New' button to start.\n");
	
    }// End init() method
    
    /***************************************************************************
     *                           Method Start                                  *
     * *************************************************************************
     * Purpose: Start method required for implementing multi-threading. Start is
     * the first method called by a thread after creation. Procedures Calling:
     * run Procedures Called: run Global Variables Used: gbnThread - creates new
     * thread for first execution and starts thread(calling run method of
     * thread)
     **************************************************************************/
    public void start() {
	// Creating GBNThread and starting execution. After start method is run
	// the run method of this class is called
	if (gbnThread == null)
	    gbnThread = new Thread(this);
	gbnThread.start();
    }// End start() method
    
    /***************************************************************************
     * Method run *
     * **************************************************************** Purpose:
     * Run method required by runnable interface. Determines which thread is
     * calling and process accordingly. gbnThread produces the animation for the
     * applet. The timerThread sleeps until timeout processing is needed to
     * retransmit the sending window. Original code by Shamiul Azom
     * Procedures/Functions Called: check_upto_n, paint/update(indirectly)
     * Procedures/Functions Calling: main, start Local variables: currentThread -
     * holds the identifier for the currently executing thread i - temporary
     * variable used for loop control Global variables used: sender - array
     * holding the Packets and the corresponding acks for the Packets sent in
     * the applet output - console window to display information about the
     * applet activities.
     * 
     * lastKnownSucPacket - holds the number of the last successful Packet to
     * arrive gbnThread - thread to advance animation
     **************************************************************************/
    public void run() {
	//force garbage collection - depending on garbage collection threads may be left
	//executing even though they have been killed leading to unexpected behavior
	System.gc();
	/*
	 * Figure out which thread called this run method since both the
	 * SelectiveRepeat simulation thread and the timer thread call the same
	 * run method. We must do this because there cannot be 2 run methods in
	 * the same class.
	 */
	boolean stopCheck = false;
	if (sender[total_Packet - 1] != null) {
	    for (int i = total_Packet - window_len; i < total_Packet; i++)
		if (!sender[i].acknowledged) {
		    stopCheck = false;
		    break;
		} else {
		    stopCheck = true;
		}
	    if (stopCheck) {
		output.append("Data Transferred - Simulation completed.\n");
		gbnThread = null;
		return;
	    }
	}
	Thread currenthread = Thread.currentThread();
	while (currenthread == gbnThread)
	    // While the animation is running
	    if (onTheWay(sender)) // Checks if any of the Packets are
		// traveling
		{
		    // Iterates through all of the Packet numbers (in this case from
		    // 0 to 20)
		    for (int i = 0; i < total_Packet; i++) {
			// If the sender array for index[Packet number] is not null,
			// do the following, else do nothing
			if (sender[i] != null) {
			    // If the sender array for index[Packet number] is
			    // marked as roaming, do the following, else do nothing
			    if (sender[i].on_way) {
				// If the sender array for index[Packet number]'s
				// Packet position is not yet at its destination
				// increase the Packets position by 5 and call a
				// repaint to essentially move the Packet.
				if (sender[i].Packet_pos < (v_clearance - pack_height))
				    sender[i].Packet_pos += 5;
				// Otherwise the PACKET reached its destination, in
				// which case do the following:
				else if (sender[i].Packet_ack) {
				    // Set the Packets reached destination attribute
				    // to true
				    sender[i].reached_dest = true;
				    // Check to see if the Packet arrived in order
				    if (check_upto_n(i)) {
					sender[i].Packet_pos = pack_height + 5;
					sender[i].Packet_ack = false;
					
					if (sender[i].buffered
					    || sender[i].acknowledged) {
					    output.append("(R) - Packet " + i + " received. Selective acknowledge for only Packet " + i + " sent.\n");
					    sender[i].received = true;
					} else if (!sender[i].received) {
					    output.append("(R) - Packet " + i + " received. Selective acknowledge for only Packet " + i + " sent. Packet " + i + " delivered to application.\n");
					    sender[i].received = true;
					} else
					    output.append("(R) - Packet " + i + " received out of order. Selective acknowledge for only Packet " + i + " sent again(DUPACK)\n");
					sender[i].received = true;
					deliverBuffer(i);
				    }
				    
				    // if Packet is already acknowledged then we
				    // know its duplicate
				    // in response to a lost ack
				    else if (sender[i].acknowledged) {
					sender[i].Packet_pos = pack_height + 5;
					sender[i].Packet_ack = false;
					output.append("(R) - Packet " + i + " received. Selective acknowledge for only Packet " + i + " sent.\n");
					sender[i].received = true;
					deliverBuffer(i);
					
				    } else {
					sender[i].buffered = true;
					sender[i].Packet_pos = pack_height + 5;
					sender[i].Packet_ack = false;
					
					output.append("(R) - Packet " + i + " received out of order.  Packet buffered. Selective acknowledge for only Packet " + i + " sent.\n");
					sender[i].received = true;
					
					deliverBuffer(i);
					if (i == selected) {
					    selected = -1;
					    kill.setEnabled(false);
					}
				    }
				} else if (!sender[i].Packet_ack) {
				    // End sender[i].Packet_ack
				    // Otherwise the ACK reached its destination, in
				    // which case do the following:
				    output.append("(S) - Selective ACK for only Packet " + i + " received. Timer for Packet " + i + " stopped.\n");
				    sender[i].on_way = false;
				    // In order check
				    if (check_upto_n(i)) {
					sender[i].acknowledged = true;
					sender[i].buffered = false;
				    } else {
					sender[i].acknowledged = true;
					sender[i].buffered = true;
				    }
				    
				    if (i == selected) {
					selected = -1;
					kill.setEnabled(false);
				    }
				    
				    timerThread = null; // resetting timer thread
				    
				    // deliverBuffer();
				    
				    // Iterate from the base value to the end (from
				    // base to 20 in this case)
				    // Checking for buffered Packets, in which case
				    // deliver all of the ones found.
				    for (int k = base; k < total_Packet; k++) {
					if (sender[k] != null) {
					    if (sender[base].acknowledged) {
						sender[base].buffered = false;
						if (k + window_len < total_Packet)
						    base = base + 1;
						
					    }
					} else
					    break;
				    }
				    if (nextseqsum < base + window_len)
					send.setEnabled(true);
				    
				    if (base != nextseqsum) {
					timerThread = new Thread(this);
					timerSleep = true;
					timerThread.start();
				    }
				}// End !sender[i].Packet_ack
			    }// End sender[i] .onway
			}// End sender[i] != null
		    }// End for loop
		    repaint();
		    
		    try {
			Thread.sleep(1000 / fps);
		    } catch (InterruptedException e) {
			System.out.println("Help");
		    }
		    
		} else
		    gbnThread = null;

	// Timer thread restransmission of Packets
	while (currenthread == timerThread)
	    if (timerSleep) {
		timerSleep = false;
		try {
		    Thread.sleep(time_out_sec * 1000);
		} catch (InterruptedException e) {
		    System.out.println("Timer interrupted.");
		}
	    } else
		retransmitOutstandingPackets();
    }
    
    /***************************************************************************
     *                         Method deliverBuffer                            *
     * *************************************************************************
     * Purpose: Handles the delivery of buffered packets at the receiver.
     * calling: run Procedures called: none Global variables used: sender[] -
     * access packet information Local variables used: j, k, l - loop control
     * variables PacketNumber - process up to this index in sender
     **************************************************************************/
    void deliverBuffer(int PacketNumber) {
	int j = 0;
	
	// Find our first buffered Packet in our array
	while (j < PacketNumber) {
	    // error - all Packets up to PacketNumber should be created
	    // if not something has gone horribly wrong
	    if (sender[j] == null)
		return;
	    // if Packet is ackd everythings fine keep looping
	    else if (sender[j].acknowledged) {
		sender[j].buffered = false;
		j++;
		// else it must be buffered or in transmission stop here
		// this is our first possible buffered Packet
	    } else
		break;
	}
	// above loop stops on last acked packet + 1
	// adjust count to make sure we start check at appropriate count
	// test > 0 to prevent index out of bounds
	if (j > 0)
	    j--;
	for (int k = j; k < total_Packet; k++) {
	    // prevent indexing out of bounds
	    if (sender[k] == null)
		break;
	    // if packet is buffered deliver to application and advance window
	    else if (sender[k].buffered) {
		sender[k].buffered = false;
		// sender[k].acknowledged = true;
		output.append("(R) - Buffered Packet " + k + " delivered to application.\n");
		
		// if this packet is ack'd already advance
	    } else if (sender[k].acknowledged) {
		sender[k].acknowledged = true;
		sender[k].buffered = false;
	    } else if (!sender[k].Packet_ack) {
		sender[k].buffered = false;
		// if Packet is buffered deliver to application
		// and increment receiver window
	    } else
		break;
	}
	int count = 0;
	for (int i = 0; i < total_Packet; i++)
	    if (sender[i] != null) {
		if (sender[i].received){
		    if (i + 1 <= (total_Packet - receiver_window_len))
			count = i + 1;
		}else 
		    break;
	    } else
		break;
	receiver_base = count;
    }
    
    /***************************************************************************
     *                  Method retransmitOutstandingPacket                     *
     * *************************************************************************
     * Purpose: handles transmission of Packets when a timeout occurs Procedure
     * calling: run(called by timerThread) Procedures called: none Global
     * variables used: sender[] - to set up params for retransmission timerSleep -
     * to reset timer value GBNThread - set animation thread for retransmission
     * output - output messages to user about retransmission base - number of
     * left-most Packet in the sending window Local variables: n - used as loop
     * control variable
     **************************************************************************/
    private void retransmitOutstandingPackets() {
	int retransmitPacket = 0;
	// after the timerThread wakes up process the Packets in sender
	// array from the base of our window (the leftmost edge)
	for (int n = base; n < base + window_len; n++)
	    if (sender[n] != null)
		if (!sender[n].acknowledged && !sender[n].buffered) {
		    sender[n].on_way = true;
		    sender[n].Packet_ack = true;
		    sender[n].Packet_pos = pack_height + 5;
		    retransmitPacket++;
		} else if (!sender[n].acknowledged && sender[n].buffered) {
		    sender[n].on_way = true;
		    sender[n].Packet_ack = true;
		    sender[n].Packet_pos = pack_height + 5;
		    retransmitPacket++;
		}
	timerSleep = true;
	if (gbnThread == null) {
	    gbnThread = new Thread(this);
	    gbnThread.start();
	}
	if (retransmitPacket == 0) {
	    timerThread = null;
	} else
	    output.append("(S) - Timeout occurred for Packet(s). Timer(s) restarted for Packet(s). \n");
	
    }
    
    /***************************************************************************
     *                     Method setupSimulationParams                        *
     * *************************************************************************
     * Purpose: Extract simulation parameters from the HTML page the applet is
     * being executed from. If the parameter is supplied convert to value to
     * integer and check for greater than 0(less than 0 will throw exceptions)
     * if the value supplied is in range, assign that value to the simulation
     * parameter Global variables used: window_len,pack_widt, pack_height,
     * h_offset, v_offset, v_clearance, total_Packet, time_out_sec
     **************************************************************************/
    private void setupSimulationParams() {
	
	String strWinLen, strPackWd, strPackHt, strHrOff, strVtOff, strVtClr, strTotPack, strTimeout;
	
	// Start collecting parameters from HTML the applet is called from
	strWinLen = getParameter("window_length");
	strPackWd = getParameter("Packet_width");
	strPackHt = getParameter("Packet_height");
	strHrOff = getParameter("horizontal_offset");
	strVtOff = getParameter("vertical_offset");
	strVtClr = getParameter("vertical_clearance");
	strTotPack = getParameter("total_Packets");
	strTimeout = getParameter("timer_time_out");
	
	// try to retrieve the expected parameters we read in from above
	try {
	    // check if current param was supplied in HTML page
	    if (strWinLen != null) {
		// if param was supplied convert value to integer value
		window_len = Integer.parseInt(strWinLen);
		// check if value supplied is greater than 0 (negative or 0 will
		// cause simulation errors)
		// conditional assignment - if window_leng is greater than 0,
		// window_len keeps its current value otherwise the default
		// value(sender_window_len_def) is uesd
		window_len = (window_len > 0) ? window_len: sender_window_len_def;
	    } else
		// if param was not supplied use default value
		window_len = sender_window_len_def;
	    
	    // same structure as above
	    if (strPackWd != null) {
		pack_width = Integer.parseInt(strPackWd);
		pack_width = (pack_width > 0) ? pack_width : pack_width_def;
	    } else
		pack_width = pack_width_def;
	    
	    if (strPackHt != null) {
		pack_height = Integer.parseInt(strPackHt);
		pack_height = (pack_height > 0) ? pack_height : pack_height_def;
	    } else
		pack_height = pack_height_def;
	    
	    if (strHrOff != null) {
		h_offset = Integer.parseInt(strHrOff);
		h_offset = (h_offset > 0) ? h_offset : h_offset_def;
	    } else
		h_offset = h_offset_def;
	    
	    if (strVtOff != null) {
		v_offset = Integer.parseInt(strVtOff);
		v_offset = (v_offset > 0) ? v_offset : v_offset_def;
	    } else
		v_offset = v_offset_def;
	    
	    if (strVtClr != null) {
		v_clearance = Integer.parseInt(strVtClr);
		v_clearance = (v_clearance > 0) ? v_clearance : v_clearance_def;
	    } else
		v_clearance = v_clearance_def;
	    
	    if (strTotPack != null) {
		total_Packet = Integer.parseInt(strTotPack);
		total_Packet = (total_Packet > 0) ? total_Packet : total_Packet_def;
	    } else
		total_Packet = total_Packet_def;
	    
	    if (strTimeout != null) {
		time_out_sec = Integer.parseInt(strTimeout);
		time_out_sec = (time_out_sec > 0) ? time_out_sec : time_out_sec_def;
	    } else
		time_out_sec = (time_out_sec > 0) ? time_out_sec : time_out_sec_def;
	    
	    // exception converting to integer - if a non integer value is
	    // supplied conversion to an integer value will throw an exception
	    // if an exception is thrown, keep supplied values(already checked)
	    // and use default values for rest of params.
	} catch (Exception e) {
	    // if above fails use what values we have and defaults for the rest
	    // should recover more gracefully than previous code
	    window_len = (window_len > 0) ? window_len : sender_window_len_def;
	    pack_width = (pack_width > 0) ? pack_width : pack_width_def;
	    pack_height = (pack_height > 0) ? pack_height : pack_height_def;
	    h_offset = (h_offset > 0) ? h_offset : h_offset_def;
	    v_offset = (v_offset > 0) ? v_offset : v_offset_def;
	    v_clearance = (v_clearance > 0) ? v_clearance : v_clearance_def;
	    total_Packet = (total_Packet > 0) ? total_Packet : total_Packet_def;
	    time_out_sec = (time_out_sec > 0) ? time_out_sec : time_out_sec_def;
	}
	
    }
    
    
    /***************************************************************************
     *                     Method actionPerformed                              * 
     * *************************************************************************
     * Purpose: actionPerformed method required to be an action listener class.
     * Determines which button in the animation is pressed (ie send new, stop
     * animation, kill Packet/ack, ...) Procedures/Functions Called:
     * paint/update i - temporary variable used for loop control Global
     * variables used: sender - array holding the Packets and the corresponding
     * acks for the Packets sent in the applet nextSeq - the next unused
     * sequence number for a Packet
     **************************************************************************/
    
    public void actionPerformed(ActionEvent e) {
	
	// get what button called the method and perform appropriate action
	String cmd = e.getActionCommand();
	
	// user pressed the send new button check if we can send a new Packet
	if (cmd == "rdt" && nextseqsum < base + window_len) {
	    // create our new Packet in the sender array
	    sender[nextseqsum] = new SelectiveRepeatPacket(true, pack_height + ADVANCE_PACKET,nextseqsum);
	    // tell user the Packet was successfully created and sent
	    output.append("(S) - Packet " + nextseqsum + " sent\n");
	    // simulate our per Packet timers
	    output.append("(S) - Timer started for Packet " + nextseqsum + "\n");
	    if (base == nextseqsum) // i.e. the window is empty and new data is
		// comming in
		{
		    // start the timer thread for timeout processing
		    if (timerThread == null)
			timerThread = new Thread(this);
		    timerSleep = true;
		    timerThread.start();
		}
	    
	    repaint();
	    nextseqsum++;
	    if (nextseqsum == base + window_len)
		send.setEnabled(false);
	    start();
	}
	
	// user wants to increase speed of animation
	else if (cmd == "fast") // Faster button pressed
	    {
		fps += FPS_STEP;
		output.append("-Simulation speed increased\n");
	    }
	
	// user wants to decrease speed of animation
	else if (cmd == "slow" && fps > MIN_FPS) {
	    fps -= FPS_STEP;
	    output.append("-Simulation speed decreased\n");
	}
	// pause animation
	
	// stop the animation from running to allow user to read status messages
	// and examine Packets in transmission
	else if (cmd == "stopanim") {
	    output.append("- Simulation paused\n");
	    gbnThread = null;
	    
	    if (timerThread != null) {
		timerFlag = true;
		timerThread = null; // added later
	    }
	    // change our stop button to allow the user to resume the simulation
	    stop.setLabel("Resume");
	    stop.setActionCommand("startanim");
	    
	    // disableing all the buttons we dont allow user to perform actions
	    // during paused sim
	    send.setEnabled(false);
	    slow.setEnabled(false);
	    fast.setEnabled(false);
	    kill.setEnabled(false);
	    
	    repaint();
	}
	
	// resumes animation after it was paused.
	else if (cmd == "startanim") {
	    output.append("-Simulation resumed.\n");
	    stop.setLabel("Pause");
	    stop.setActionCommand("stopanim");
	    
	    if (timerFlag) {
		timerThread = new Thread(this);
		timerSleep = true;
		timerThread.start();
	    }
	    
	    // enabling the buttons
	    send.setEnabled(true);
	    slow.setEnabled(true);
	    fast.setEnabled(true);
	    kill.setEnabled(true);
	    
	    // repaint to show updated simulation
	    repaint();
	    start();
	    
	}
	
	// lose selected Packet in transmisson
	else if (cmd == "kl") {
	    if (sender[selected].Packet_ack) {
		output.append("- Packet " + selected + " lost\n");
	    } else
		output.append("- Selective Ack of Packet " + selected
			      + " lost.\n");
	    
	    sender[selected].on_way = false;
	    kill.setEnabled(false);
	    selected = DESELECTED;
	    repaint();
	}
	
	// reset animation to initial view
	else if (cmd == "rst")
	    reset_app();
    }
    
    /***************************************************************************
     *                             Method mouseDown                            *
     * *************************************************************************
     * Purpose: Determines when the mouse is pressed down and what
     * object(Packet) is currently under the mouse. mouseDown is used to select
     * a Packet in transmission to be killed(possibly) Global variables used:
     * sender - array holding the Packets and the corresponding acks for the
     * Packets sent in the applet output - console window to display information
     * about the applet activities
     **************************************************************************/
    public boolean mouseDown(Event e, int x, int y) {
	int location, xpos, ypos;
	location = (x - h_offset) / (pack_width + 7);
	// for clicking off of currently selected Packet - also prevents index
	// out of bounds exceptions
	if (location >= total_Packet || location < 0) {
	    selected = DESELECTED;
	    return false;
	}
	if (sender[location] != null) {
	    xpos = h_offset + (pack_width + 7) * location;
	    ypos = sender[location].Packet_pos;
	    
	    if (x >= xpos && x <= xpos + pack_width && sender[location].on_way) {
		if ((sender[location].Packet_ack && y >= v_offset + ypos && y <= v_offset + ypos + pack_height) || ((!sender[location].Packet_ack) && y >= v_offset + v_clearance - ypos && y <= v_offset + v_clearance - ypos + pack_height)) {
		    if (sender[location].Packet_ack)
			output.append("- Packet " + location + " selected.\n");
		    else
			output.append("- Selective Ack " + location
				      + " selected.\n");
		    
		    sender[location].selected = true;
		    selected = location;
		    kill.setEnabled(true);
		    repaint();
		    
		} else {
		    output.append("-Click on a moving Packet to select.\n");
		    selected = DESELECTED;
		}
	    } else {
		output.append("-Click on a moving Packet to select.\n");
		selected = DESELECTED;
	    }
	}
	
	return true;
    }
    
    /***************************************************************************
     *                            Method paint                                 *
     * *************************************************************************
     * Purpose: Allows a graphics context to be established for drawing
     * Procedures/Functions Called: update Procedures/Functions Calling: main,
     * start, run Local variables: g - Graphics object for drawing functionality
     **************************************************************************/
    public void paint(Graphics g) // To eliminate flushing, update is
    // overriden
    {
	update(g);
    }
    
    /***************************************************************************
     *                          Method Update                                  *
     * *************************************************************************
     * Purpose: Handles the actual drawing for the applet. Draws the Packets,
     * message boxes, ... Procedures/Functions Called:check_upto_n,
     * paint/update(indirectly) Procedures/Functions Calling: paint Local
     * variables: i - temporary variable used for loop control Global variables
     * used: sender - array holding the Packets and the corresponding acks for
     * the Packets sent in the applet offGraphics - used to create a secondary
     * buffer to draw the necessary components before putting the completed
     * drawing to screen. This prevents "flashing" when viewing the applet on
     * higher frame rates
     **************************************************************************/
    public void update(Graphics g) {
	Dimension d = size();

	// Create the offscreen graphics context, if no good one exists.
	if ((offGraphics == null) || (d.width != offDimension.width)
	    || (d.height != offDimension.height)) {
	    offDimension = d;
	    offImage = createImage(d.width, d.height);
	    offGraphics = offImage.getGraphics();
	}
	
	// Erase the previous image.
	offGraphics.setColor(Color.white);
	offGraphics.fillRect(0, 0, d.width, d.height);
	
	// drawing window
	offGraphics.setColor(Color.black);
	// Sender window defining the top left, and bottom right coordinates of
	// the rectangle.

	offGraphics.draw3DRect(h_offset + base * (pack_width + 7) - 4,v_offset - 3, (window_len) * (pack_width + 7) + 1,pack_height + 6, true);
	// Receiver window. Note: the 222 is used to relocate the box based on
	// the v_offset variable, which is located in the senders box
	offGraphics.draw3DRect(h_offset + receiver_base * (pack_width + 7) - 4,v_offset + 222, ((receiver_window_len) * (pack_width + 7) + 1),pack_height + 6, true);
	
	// walk through our sender array and gather information about how to
	// draw Packets
	for (int i = 0; i < total_Packet; i++) {
	    // print out numbers over our Packets for easy reference
	    offGraphics.setColor(Color.black);
	    offGraphics.drawString("" + i, h_offset + (pack_width + 7) * i, v_offset - 4);
	    offGraphics.drawString("" + i, h_offset + (pack_width + 7) * i, v_offset + v_clearance + 30);
	    
	    // if no Packet has been created at our current index draw the
	    // Packet as a black rectangle
	    if (sender[i] == null) {
		offGraphics.setColor(Color.black);
		offGraphics.draw3DRect(h_offset + (pack_width + 7) * i,v_offset, pack_width, pack_height, true);
		offGraphics.draw3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance, pack_width, pack_height, true);
	    } else {
		// Packet exists at our current index - determine what color to
		// draw the Packet in the animation
		if (sender[i].acknowledged)
		    offGraphics.setColor(received_ack);
		else
		    offGraphics.setColor(unack_color);
		
		offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset, pack_width, pack_height, true);
		if (sender[i].buffered)
		    offGraphics.setColor(Color.GRAY);
		else
		    // drawing the destination Packets
		    offGraphics.setColor(dest_color);
		// if the Packet has reached the destination than draw a filled
		// rectangle in destination row
		
		// else draw a "clear" rectangle in destination row
		if (sender[i].reached_dest)
		    offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance, pack_width, pack_height,true);
		
		else {
		    offGraphics.setColor(Color.black);
		    offGraphics.draw3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance, pack_width, pack_height,true);
		}
		// drawing the moving Packets
		if (sender[i].on_way) {
		    if (i == selected)
			offGraphics.setColor(sel_color);
		    
		    else if (sender[i].Packet_ack)
			offGraphics.setColor(roam_pack_color);
		    else if (sender[i].received)
			//offGraphics.setColor(received_ack);
			offGraphics.setColor(roam_ack_color);
		    else
			offGraphics.setColor(roam_ack_color);
		    
		    if (sender[i].Packet_ack) {
			offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset + sender[i].Packet_pos, pack_width,pack_height, true);
			offGraphics.setColor(Color.black);
			offGraphics.drawString("" + i, h_offset
					       + (pack_width + 7) * i, v_offset
					       + sender[i].Packet_pos);
		    } else {
			offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance - sender[i].Packet_pos,pack_width, pack_height, true);
			if (sender[i].out_of_order) {
			    offGraphics.setColor(Color.black);
			    offGraphics.drawString("" + sender[i].ackFor,h_offset + (pack_width + 7) * i, v_offset+ v_clearance- sender[i].Packet_pos);
			} else {
			    offGraphics.setColor(Color.black);
			    offGraphics.drawString("" + i, h_offset+(pack_width + 7) * i, v_offset+ v_clearance - sender[i].Packet_pos);
			}
		    }
		} // end if sender on way
	    } // end else
	} // for loop ends
	
	// drawing message boxes
	offGraphics.setColor(Color.black);
	int newvOffset = v_offset + v_clearance + pack_height;
	int newHOffset = h_offset;
	
	// draw values of variables on frame
	// offGraphics.drawString(newHOffset,newvOffset+25);
	offGraphics.drawString("(S) - Action at Sender                  (R) - Action at Receiver",newHOffset + 60, newvOffset + 90);
	
	// offGraphics.drawString(strCurrentValues,newHOffset,newvOffset+40);
	offGraphics.drawString("Packet", newHOffset + 15, newvOffset + 60);
	offGraphics.drawString("Ack Received", newHOffset + 225,newvOffset + 60);
	offGraphics.drawString("Ack", newHOffset + 170, newvOffset + 60);
	offGraphics.drawString("Received", newHOffset + 85, newvOffset + 60);
	offGraphics.drawString("Selected", newHOffset + 335, newvOffset + 60);
	offGraphics.drawString("Buffered", newHOffset + 415, newvOffset + 60);
	
	offGraphics.drawString("base = " + base, h_offset + (pack_width + 7)* total_Packet + 10, v_offset + 33);
	offGraphics.drawString("nextseqnum = " + nextseqsum, h_offset+(pack_width + 7) * total_Packet + 10, v_offset + 50);
	
	offGraphics.setColor(Color.blue);
	offGraphics.drawString("Sender (Send Window Size = " + window_len + ")", h_offset + (pack_width + 7) * total_Packet + 10, v_offset + 12);
	offGraphics.drawString("Receiver (Receiver Window Size = " + receiver_window_len + ")", h_offset + (pack_width + 7) * total_Packet + 10, v_offset + v_clearance + 12);
	offGraphics.setColor(Color.gray);
	offGraphics.draw3DRect(newHOffset - 10, newvOffset + 42, 475, 25, true);
	offGraphics.setColor(roam_pack_color);
	offGraphics.fill3DRect(newHOffset, newvOffset + 50, 10, 10, true);
	offGraphics.setColor(roam_ack_color);
	offGraphics.fill3DRect(newHOffset + 155, newvOffset + 50, 10, 10, true);
	offGraphics.setColor(received_ack);
	offGraphics.fill3DRect(newHOffset + 210, newvOffset + 50, 10, 10, true);
	offGraphics.setColor(dest_color);
	offGraphics.fill3DRect(newHOffset + 70, newvOffset + 50, 10, 10, true);
	offGraphics.setColor(sel_color);
	offGraphics.fill3DRect(newHOffset + 320, newvOffset + 50, 10, 10, true);
	offGraphics.setColor(Color.GRAY);
	offGraphics.fill3DRect(newHOffset + 400, newvOffset + 50, 10, 10, true);
	g.drawImage(offImage, 0, 0, this);
    } // method paint ends
    
    /***************************************************************************
     *                         Method onTheWay                                 *
     * *************************************************************************
     * Purpose: checks to see if all of the Packets in an array(in our case the
     * sender array) have been created and are being processed
     * Procedures/Functions Calling: run Local variables: i - temporary variable
     * used for loop control
     **************************************************************************/
    public boolean onTheWay(SelectiveRepeatPacket pac[]) {
	
	for (int i = 0; i < pac.length; i++)
	    if (pac[i] == null)
		return false;
	    else if (pac[i].on_way)
		return true;
	
	return false;
    }
    
    /***************************************************************************
     *                         Method check_upto_n                             *
     * *************************************************************************
     * Purpose: checks the sender array to see if all of the pacekts up to index
     * packno have reached thier destination Procedures/Functions Calling: run
     * Local variables: i - temporary variable used for loop control Global
     * variables used: sender - array holding the Packets and the corresponding
     * acks for the Packets sent in the applet
     **************************************************************************/
    public boolean check_upto_n(int packno) {
	for (int i = 0; i < packno; i++)
	    if (!sender[i].reached_dest)
		return false;
	return true;
    }
    
    /***************************************************************************
     *                         Method reset_app                                *
     * *************************************************************************
     * Purpose: resets the applet to its initial state to allow for a second run
     * without reloading the webpage Local variables: i - temporary variable
     * used for loop control Global variables used: sender - array holding the
     * Packets and the corresponding acks for the Packets sent in the applet
     * base - what number our sending window is set to nextseq - the next
     * sequence number that can be used for a Packet selected - the Packet
     * currently selected fps - how fast shoud the animation run timerFlag -
     * gbnThread - used to process and display the animation timerThread - used
     * to handle timeouts and retransmit the sending window
     **************************************************************************/
    
    public void reset_app() {
	
	for (int i = 0; i < total_Packet; i++)
	    if (sender[i] != null)
		sender[i] = null;
	
	base = 0;
	receiver_base = 0;
	nextseqsum = 0;
	selected = DESELECTED;
	fps = DEFAULT_FPS;
	timerFlag = false;
	timerSleep = false;
	gbnThread = null;
	timerThread = null;
	
	if (stop.getActionCommand() == "startanim") // in case of pause mode,
	    // enable all buttons
	    {
		slow.setEnabled(true);
		fast.setEnabled(true);
	    }
	
	send.setEnabled(true);
	kill.setEnabled(false);
	stop.setLabel("Stop Animation");
	stop.setActionCommand("stopanim");
	output
	    .append("---------------------------------------------------\n\n");
	output.append("-Simulation restarted. Press 'Send New' to start.\n");
	repaint();
    }
    
} // end class SelectiveRepeat

class SelectiveRepeatPacket {
    
    boolean on_way; // is Packet in transit
    boolean reached_dest; // true if Packet reached the destination
    boolean acknowledged; // used by drawing function -false will use Packet
    // color -true will use ack color
    boolean Packet_ack; // is this Packet an ack? if false Packet is assumed to
    // be a message
    boolean selected; // true if Packet was selected by user false otherwise
    boolean received; // true if Packet was received
    boolean out_of_order; // Packet arrived out of order and an ack from the
    // base needs to be sent
    int Packet_pos; // location of Packet in diagram
    int ackFor; // carries the number of the Packet the ack is for
    boolean buffered;
    
    SelectiveRepeatPacket() {
	on_way = false;
	selected = false;
	reached_dest = false;
	acknowledged = false;
	Packet_ack = true;
	received = false;
	out_of_order = false;
	Packet_pos = 0;
	ackFor = 0;
	buffered = false;
    }
    
    SelectiveRepeatPacket(boolean onway, int Packetpos, int nextseq) {
	on_way = onway;
	selected = false;
	reached_dest = false;
	acknowledged = false;
	Packet_ack = true;
	received = false;
	out_of_order = false;
	Packet_pos = Packetpos;
	ackFor = nextseq;
	buffered = false;
	
    }
}

