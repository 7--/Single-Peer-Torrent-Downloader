/**
* 
* Reads and reacts to messages from peers. 
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Peer implements Runnable{

	private static final byte[] PROTOCOL_STRING = { 19, 'B', 'i', 't', 'T',
			'o', 'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c',
			'o', 'l' };
	
	byte[] peerId;
	String ip;
	int port;
	byte[] infoHash;
	byte[] clientId;
	Socket sock;
	boolean run = true;
	//The bits representing the pieces the peer has by bitfield messages or haves
	boolean[] bitField;			
	boolean[] piecesHas;
	Map<Integer,Integer> recieved;
	//Pieces client already has
	boolean complete = false; 	
	TorrentInfo2 ti;
	DataOutputStream dos;
	DataInputStream dis;
	RUBTClient rubt;
	//If true client is attempting to request pieces from this peer.
	private boolean downState; 
	//[has complete piece][has block (length is num of blocks)]
	private boolean[][] recievedBlocks;
	int piecesRecieved=0;
	
	int tempI=0;
	byte[] tempPiece;
	byte[] completePiece;
	
	/**
	 * The local client is choking the peer.
	 */
	boolean	amChoked = true;
	/**
	 * The local client is interested in the remote peer's pieces.
	 */
	boolean	amInterested = false;
	/**
	 * The remote peer is chocking the local client 
	 */
	boolean	theyChoked = true;
	/**
	 * The remote peer is interested in the local clients pieces
	 */
	boolean theyInterested = false;

	/**
	 * Creates a new peer
	 * 
	 * @param ip
	 *            the IP address of the remote peer
	 * @param port
	 *            the port number of the remote peer
	 * @param peerId
	 *            the peer ID of the remote peer
	 * @param infoHash
	 *            the info hash for the torrent
	 * @param clientId
	 *            the peer id for THIS client (local)
	 * @param complete
	 *  		  is the file downloaded         
	 */
	public Peer(String ip, int port, byte[] peerId, byte[] infoHash,
			byte[] clientId, boolean complete, TorrentInfo2 ti, RUBTClient rubt) {
		this.ip = ip;
		this.port = port;
		this.peerId = peerId;
		this.infoHash = infoHash;
		this.clientId = clientId;
		this.complete = complete;
		this.ti = ti;
		this.rubt=rubt;
		tempPiece = new byte[ti.piece_length];
		recieved = new HashMap<Integer,Integer>();
		completePiece= new byte[ti.piece_length];
		recievedBlocks = new boolean[ti.total_pieces][ti.num_blocks_in_piece];
	}
	
	@Override
	public String toString() {
		return "Peer (" + this.ip + ":" + this.port + ")";
	}

	/**
	 * Returns true if the handshake succeeds, else false.
	 * 
	 * @return true if the handshake succeeds
	 */
	public boolean handshake() throws IOException {
		// Already connected, why call again???
		if (this.sock != null && this.sock.isConnected()
				&& !this.sock.isClosed()) {
			System.err.println("Trying to connect to " + this + " twice.");
			return true;
		}
		this.sock = new Socket(this.ip, this.port);

		// Data input/output of socket connection
		dis = new DataInputStream(this.sock.getInputStream());
		dos = new DataOutputStream(this.sock.getOutputStream());

		// Write 19
		// Protocol string length and protocol string
		dos.write(PROTOCOL_STRING);

		dos.write(new byte[8]);

		dos.write(this.infoHash);

		dos.write(this.clientId);
		dos.flush();

		byte[] hs = new byte[68];
		dis.readFully(hs);

		byte[] handshakePeerId = Arrays.copyOfRange(hs, 48, 68);
		if(Arrays.equals(this.peerId, handshakePeerId)){
			System.out.print("Handshake valid");
				return true;
		}
		else{
			System.out.print("Handshake invalid");
			this.sock.close();
			return false;
		}	
	}
	
	@Override
	/**
	 * create thread to read messages
	 */
	public void run() {
		try {
			if(handshake()==true){
				
				//Create a new thread for reading the stream
				Thread readThread = new Thread(new Runnable() {
				     public void run()
				     {
				          while(run){
				        	  read();
				        	  try {
								Thread.sleep(0);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
				          }
				     }});  
				readThread.start();
			}
		}	
			 catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads the messages from the peer
	 */
	public void read(){
		try {
		
			Message readMess = Message.read(dis);
			//System.out.println(readMess);
			//Keep alive
			if(readMess.getLength() == 0){
				System.out.print("Keep alive");
			}
			else if(readMess.getId() == 0){
				theyChoked = true;
				System.out.println(peerId +" chocked the client");	

			}
			//Peer sent unchoke message
			else if(readMess.getId() == 1){
				theyChoked = false;
				System.out.println(peerId +" unchocked the client");		
				}
			//Peer is intrested 
			else if(readMess.getId() == 2){
				theyInterested = true;
				System.out.println(peerId +" is intrested");
				//Peer is interested so send unchoke
				Message.UNCHOKE.send(dos);
			}
			//Peer is not intrested
			else if(readMess.getId() == 3){
				System.out.println(peerId +" isn't intrested");	
				theyInterested = false;
			}
			//Have message
			else if(readMess.getId() == 4){
				 HaveMessage have = (HaveMessage) readMess;
				 if(bitField==null){
					 bitField = new boolean[(int) Math.ceil(ti.file_length/ti.piece_length)+1];
					 System.out.println("Have Message" + Math.ceil(ti.file_length/ti.piece_length));
				 }
				 System.out.println(have.index);
				 bitField[have.index] = true; 
			}
			//If read message is bitfield
			else if(readMess instanceof BitfieldMessage){
				BitfieldMessage bm = (BitfieldMessage) readMess;
				
				//System.out.println(Arrays.toString(bm.bitfield));
				String bfString = toBinary(bm.bitfield);
				bitField = new boolean[bfString.length()];
				for(int i=0; i!=bfString.length(); i++){
					if(bfString.charAt(i)== '1'){
						bitField[i] = true;
					}
					else{
						bitField[i] = false;
					}
				}	
				//Print the boolean values for each bit
				System.out.println(Arrays.toString(bitField) + "Peer ID: " + peerId);
				
			}
			//Request message
			else if(readMess.getId()==6){
				RequestMessage rm = (RequestMessage) readMess;
				//TODO Send back the piece
				if(theyInterested){
					//The below params need to be set correctly
					PieceMessage pm = new PieceMessage(0, (Byte) null, rm.pIndex, 0, null);
					pm.send(dos);
				}
			}
			//Piece Message
			else if(readMess.getId() ==7){
				PieceMessage pm = (PieceMessage) readMess;
				
				//Updated temp piece that needs to be saved
				
				//If its a new piece
				if(pm.bOffset==0){
					//If its not the first piece
					if(pm.pIndex!=0)
						//Then check hash and save the last piece
						savePiece(completePiece,pm.pIndex);
					tempPiece=pm.block;
				}
				//Add block to tempPiece yet to be saved
				//c is the array that should have the full piece
				else{
					System.arraycopy(tempPiece, 0, completePiece, 0, tempPiece.length);
					System.arraycopy(pm.block, 0, completePiece, tempPiece.length, pm.block.length);
				}
				

				//If the piece message is the last piece and block of file
				if(pm.pIndex==ti.total_pieces-1&&pm.bOffset==ti.block_size*(ti.num_blocks_in_piece-1)){
					//Then save the last piece
					System.out.println("AAALast Piece, pm.pIndex "+ pm.pIndex + "pm.boffset: "
					+ pm.bOffset+" otherside "+ ti.block_size*(ti.num_blocks_in_piece-1)
					+" total pieces: " + ti.total_pieces);
					savePiece(completePiece,pm.pIndex+1);
				}
				
				recievedBlocks[pm.pIndex][pm.bOffset/16384]=true;
				System.out.println(peerId + "  sent piece " + pm.pIndex +"  Offset: "+ pm.bOffset);
			}
		} catch (IOException e) {
		}
	}
	
	/**
	 * Called when the peer gets a full piece
	 * @param Fullpiece the piece 
	 * @param pieceIndex the piece that's complete
	 */
	public void savePiece(byte[] Fullpiece, int pieceIndex){
		System.out.println("Got FULL PIECE");
		
		if(!checkPiece(pieceIndex,Fullpiece,ti.piece_length)){
			System.out.println("Hash Check unsucesful.");
		}
		else{
			//if its not the last piece
			if(pieceIndex!=ti.total_pieces-1){
				if(!rubt.putPiece(pieceIndex, Fullpiece))
					System.out.println("Error saving piece "+ pieceIndex);
			}
			else{
				if(!rubt.putPiece(pieceIndex-1, Fullpiece))
					System.out.println("Error saving piece "+ pieceIndex);
			}
			sendHave(pieceIndex);
			piecesRecieved++;
		}
		
	}
	
	/**
	 * Checks the hash of a complete piece
	 * @param piece the piece to check
	 * @param bs the full piece
	 * @param length
	 * @return
	 */
	public boolean checkPiece(int piece, byte[] bs, int length)
	 {
	    // Check digest
	    MessageDigest sha1;
	    try
	      {
	        sha1 = MessageDigest.getInstance("SHA-1");
	      }
	    catch (NoSuchAlgorithmException nsae)
	      {
	        throw new InternalError("SHA1 digest not available " + nsae);
	      }
	   
	   System.out.println("Piece hashes  " + new BigInteger(ti.piece_hashes[piece-1].array()).toString(16));
	    sha1.update(bs, 0, length);
	    byte[] hash = sha1.digest();
	    
	    System.out.println("Digest:  "+ new BigInteger(hash).toString(16));
	    for (int i = 0; i < 20; i++)
	      if (hash[i] != ti.piece_hashes[piece-1].get(i))
	        return false;
	    return true;
	  }
	
	
	/**
	 * Builds a list with the block requests it needs to get the whole piece 
	 * @param piece that this peer should donwload
	 */
	public void blockListForPiece(int pieceIndex){
		downState=true;
		ArrayList<RequestMessage> reqL = new ArrayList<RequestMessage>();
		try{
				//Download each piece in the file (just one peer)
				//Get blocks
				for(int i=0;i<ti.num_blocks_in_piece;i++){
					RequestMessage rm = new RequestMessage(13, (byte)6, pieceIndex, i*ti.block_size, ti.block_size);
					
					//Make list of request messages that another function will request pieces using iterator and while
					System.out.print("  Blocks: "+ rm.pIndex+"  "+rm.bOffset);
					reqL.add(rm);
				}
				System.out.println();
				Thread.sleep(400);
			} catch (InterruptedException e) {
				downState=false;
				e.printStackTrace();
			}
		requests(reqL);
	}
	
	
	/**
	 * Attempts to download all the blocks needed for a piece from requestmessage2's pieceIndex
	 * @param req -list that holds all the requests messages that should be sent for a piece 
	 */
	public void requests(ArrayList<RequestMessage> req){
		Iterator<RequestMessage> it = req.iterator();
	    while (it.hasNext())
	    {  
	    	RequestMessage curMes = it.next();
	    	
	    		if(theyChoked){
	    			System.out.println("Chocked so can't get anymore packets");
	    		}
	    		else{
		    	    try {
					curMes.send(dos);
					System.out.println("Requesting: " +peerId + " Piece:  "+ curMes.pIndex + " at block: " + curMes.bOffset);
					boolean gotBlockResp =false;
					boolean timeExpired=false;
					long startTime = System.currentTimeMillis();
					//If gotPieceResp or timeExpired is true exit loop
					while(!gotBlockResp&&!timeExpired){
						if(recievedBlocks[curMes.pIndex][curMes.bOffset/16384]==true){
								gotBlockResp=true;
						}
						if((System.currentTimeMillis()-startTime)>20000){
							timeExpired=true;
							System.out.println("Timeout 20seconds no piece resp."+ " Peer:  " + peerId+ " Piece: " + curMes.pIndex + " block: "+ curMes.bOffset);
						}
						Thread.sleep(1);
					}
					//Thread.sleep(7000);
					//needs to wait for response
				} catch (IOException e) {
					//downState=false;
					//Getting errors here sometimes when sending messages
					e.printStackTrace();
					} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		}
	      }
	}
	
	/**
	 * Send interested message. Return true if peer is not choking the client.
	 * @return peer has client unchoked
	 */
	public boolean sendIntrested(){
		System.out.println("Sending intrested");
		try{
			//dos = new DataOutputStream(this.sock.getOutputStream());
			Message.INTERESTED.send(dos);
			Thread.sleep(1000);		//Wait to see  if the peers unchoke.
			if(!theyChoked){
				return true;
			}
		}
		catch (IOException e){
			return false;
		} catch (InterruptedException e){
			return false;
		}
		return false;
	}
	
	/**
	 * Tell peer you got piece
	 */
	public void sendHave(int piece){
		HaveMessage hm = new HaveMessage(5,(byte)4,piece);
		try {
			hm.send(dos);
			System.out.println("have sent " +piece);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Closes all peer connections
	 */
	public void close(){
		try {
			dis.close();
			dos.close();
			sock.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String toBinary(byte[] bytes){
	    StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
	    for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
	        sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
	    return sb.toString();
	}
	
	public boolean gotCompletePiece(int piece){
		
		for(int j=0; j < recievedBlocks[piece].length; j++) {
			if(recievedBlocks[piece][j]==false)
		    	  return false;			  
		}
		return true;
	}

	public int getPiecesRecieved(){
		return piecesRecieved;
	}
	
}
