/**
* Opens connections with peers and writes the file to disk
*
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class RUBTClient {

	private final TorrentInfo2 tInfo;
	private final String outFileName;
	private final byte[] peerId;
	private IncomingListener listener;
	public boolean clientHasFile;		
	public static TorrentInfo2 ti;
	private boolean[] our_bitfield;
	private RandomAccessFile destined_file;

	
	public static void main(String[] args) {
		if(args[0] == null || args[1] == null){
			System.err.println("Arguments are missing");
		}
		ti = parsing(args[0]);
		RUBTClient client = new RUBTClient(ti, args[1]);
		client.start();
	}
	
	public void start(){
		
		//Listens if peers want to connect
		this.listener = new IncomingListener(this);
		this.listener.start();
		 
		//Contact tracker and get peers
		Tracker parse = new Tracker(this.peerId, this.tInfo, listener.getPort(),this);
		List<Peer> peers = parse.contactTracker();
		
		System.out.println("peer list size is:  "+ peers.size());
		Peer p =peers.get(0);
		Thread t = new Thread(p);
		t.start();
	
		//Need to wait to get peers before starting to download
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} 
		
		Download d = new Download(peers,ti,our_bitfield,parse);
		d.onePeerDownload();
		
		try {
			this.listener.join();
			this.listener.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	public static TorrentInfo2 parsing(String filename) {

		try {
			File f = new File(filename);
			DataInputStream dis = new DataInputStream(new FileInputStream(f));

			byte[] fileArray = new byte[(int) f.length()];
			dis.readFully(fileArray);

			TorrentInfo2 ti = new TorrentInfo2(fileArray);

			return ti;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BencodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public RUBTClient(final TorrentInfo2 tInfo, final String outFileName) {
		this.tInfo = tInfo;
		this.outFileName = outFileName;
		this.peerId = genPeerId();
		our_bitfield=new boolean[ti.total_pieces];
		
		//Create instance of RandomAccessFile to write to disk
		File destined = new File(outFileName);
		try {
			destined_file = new RandomAccessFile(destined, "rw");
			destined_file.setLength(tInfo.file_length);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//Print out hashes for reference
		for(int i=0; i<ti.total_pieces;i++){
			System.out.println("Piece hashes  " + new BigInteger(ti.piece_hashes[i].array()).toString(16));
		}
	}

	/**
	 * @return whether the peer has the completed file.
	 */
	public boolean peerHasFile(Peer p){
		//The last 8 elements may be false, and peer still has completed file.
		if(p.bitField!=null){
			for(int x=0;x<p.bitField.length-8;x++){
				if(p.bitField[x]==false){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	//Not used 
	public void handleIncomingClient(final Socket sock) {
		
		System.out.println("Someones trying to connect");
		
		try {
			DataInputStream dis = new DataInputStream(sock.getInputStream());
			byte[] b = new byte[1000];
			dis.readFully(b);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	public static byte[] genPeerId() {
		Random r = new Random(System.currentTimeMillis());
		byte[] id = new byte[20];
		System.arraycopy(new byte[] { 'A', 'X', 'E', 'S' }, 0, id, 0, 4);
		for (int i = 4; i < id.length; ++i) {
			id[i] = (byte) r.nextInt(256);
		}
		return id;
	}
	

	/**
	 * Saves a full piece to disk
	 * @param index of piece to be saved
	 * @param block the complete piece of index
	 */
	public boolean putPiece(int index, byte[] block){
		try{
			System.out.println("start:  "+ destined_file.getFilePointer()+ " block size:  "+ block.length+" file size:  "+ti.file_length);
			
			if(destined_file.getFilePointer()+block.length>ti.file_length){
				byte[] lastPiece = new byte[(int) (ti.file_length - destined_file.getFilePointer())];
			    System.arraycopy(block, 0, lastPiece, 0, (int) (ti.file_length - destined_file.getFilePointer()) );
			    block=lastPiece;
			    System.out.println("Last Piece size " +block.length);
			}
			
			destined_file.write(block);
			
			if(index==ti.total_pieces){
				System.out.println("File closed");
				destined_file.close();
			}
		} 
		catch (IOException e) {
				e.printStackTrace();
				return false;
		}
			return true;
	}
}
