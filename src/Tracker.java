/**
* Communicates with the tracker
*
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Tracker {
	private static final ByteBuffer KEY_FAILURE = ByteBuffer.wrap(new byte[] {
			'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o',
			'n' });
	private static final ByteBuffer INTERVAL = ByteBuffer.wrap(new byte[] {
			'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });
	private static final ByteBuffer COMPLETE = ByteBuffer.wrap(new byte[] {
			'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' });
	private static final ByteBuffer INCOMPLETE = ByteBuffer.wrap(new byte[] {
			'i', 'n', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' });
	private static final ByteBuffer MIN_INTERVAL = ByteBuffer.wrap(new byte[] {
			'm', 'i', 'n', ' ', 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });
	private static final ByteBuffer PEERS = ByteBuffer.wrap(new byte[] { 'p',
			'e', 'e', 'r', 's' });
	private static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] { 'i',
			'p' });
	private static final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', ' ', 'i', 'd' });
	private static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {
			'p', 'o', 'r', 't' });

	public static final String CHECK_IP1 = "128.6.171.130";
	public static final String CHECK_IP2 = "128.6.171.131";

	private int interval;
	private int complete;
	private int incomplete;
	private int min_interval;
	private int port;
	private byte[] info_hash;
	private byte[] peerId;
	private String announceUrl;
	private TorrentInfo2 ti;
	private RUBTClient rubt;
	private String status;
	
	public Tracker(byte[] peerId,TorrentInfo2 ti, int port,RUBTClient rubt) {
		this.rubt=rubt;
		this.peerId = peerId;
		this.info_hash = ti.info_hash.array();
		this.announceUrl = ti.announce_url.toString();
		this.ti = ti;
		this.port=port;		
	}


	public void updateTracker(int bytesRecieved){
		try {
			URL url = new URL(this.announceUrl + "?" + "info_hash="
					+ urlencode(this.info_hash) + "&peer_id="
					+ urlencode(this.peerId)  +"&port="
					+ port
					+ "&uploaded=0" + "&downloaded=" + bytesRecieved
					+ "&left="+(ti.file_length-bytesRecieved)
					+"&event=started");
		
		HttpURLConnection con;

		con = (HttpURLConnection) url.openConnection();

		con.connect();
		
		byte[] response = new byte[con.getContentLength()];
		DataInputStream din = new DataInputStream(con.getInputStream());
		din.readFully(response);
		din.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	/**
	 * Contacts the tracker and get the peer list. 
	 * For initial connection. 
	 * @param ti
	 * 
	 *            the TorrentInfo for this torrent
	 * @return the peer list of acceptable peers, or NULL if none were received
	 */
	public List<Peer> contactTracker() {
		//The peers that we want to connect to
		
		ArrayList<Peer> returnedPeers = new ArrayList<Peer>();
		try {
			System.out.println(urlencode(this.info_hash));
			
			//Let tracker know we started
			URL url = new URL(this.announceUrl + "?" + "info_hash="
					+ urlencode(this.info_hash) + "&peer_id="
					+ urlencode(this.peerId)  +"&port="
					+ port
					+ "&uploaded=0" + "&downloaded=0" + "&left="+ti.file_length+"&event=started");
			// HttpURLConnection GET Connect
			HttpURLConnection con;

			con = (HttpURLConnection) url.openConnection();

			con.connect();
			
			byte[] response = new byte[con.getContentLength()];
			DataInputStream din = new DataInputStream(con.getInputStream());
			din.readFully(response);
			din.close();
			System.out.println(new String(response, "ASCII"));

			Map<ByteBuffer, Object> respDict = (Map<ByteBuffer, Object>) Bencoder2
					.decode(response);
			ToolKit.print(respDict);

			// Returns the value to which the specified key is mapped
			if (respDict.get(KEY_FAILURE) != null) {
				System.out.print(respDict.get(KEY_FAILURE));
				return null;
			}
			interval = (Integer) respDict.get(INTERVAL);

			List<Map<ByteBuffer, Object>> peerDicts = (List<Map<ByteBuffer, Object>>) respDict
					.get(PEERS);

			for (Map<ByteBuffer, Object> pDict : peerDicts) {
				// Check if IP is correct
				if (!pDict.containsKey(KEY_IP)) {
					System.err
							.println("Tracker peer dictionary is missing IP address.");
					continue;
				}
				String ip = new String(
						((ByteBuffer) pDict.get(KEY_IP)).array(), "ASCII");
				if (CHECK_IP1.equals(ip) || CHECK_IP2.equals(ip)) {
					// This peer is ok, so add it
					if (!pDict.containsKey(KEY_PEER_ID)
							|| !pDict.containsKey(KEY_PORT)) {
						System.err
								.println("Tracker peer dictionary is missing peer ID or port!");
						continue;
					}
					byte[] peerId = ((ByteBuffer) pDict.get(KEY_PEER_ID))
							.array();
					int port = ((Integer) pDict.get(KEY_PORT)).intValue();
					returnedPeers.add(new Peer(ip, port, peerId,
							this.info_hash, this.peerId, false, this.ti,rubt));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (BencodingException be) {
			be.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error reading the file");
			e.printStackTrace();
		}
		// Return null if no peers, else peers list
		if (returnedPeers.isEmpty()) {
			return null;
		}
		
		System.out.println("Peers returned:\n"+returnedPeers);
		return returnedPeers;
	}

	// URL encoder
	static String urlencode(byte[] bs) {
		StringBuilder sb = new StringBuilder(bs.length * 3);
		for (int i = 0; i < bs.length; i++) {

			sb.append('%').append(String.format("%02x", bs[i] & 0xFF));
		}

		return sb.toString();
	}
}
