/**
* Finds which peers can be used to download from
* and tells the peers what download.
* Also can calculate the rarity of pieces 
* @author  Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Download {
	
	private TorrentInfo2 ti;
	private List<Peer> peers;
	//The peers that unchoked us, and we're able to download from
	private List<Peer> useablePeers;	
	//a[piece index, rarest is lowest][0 Actual piece index (bitfield)] or a[piece# rarest lowest][1 Num peers that has piece]
	private int[][] a;
	private Tracker tracker;
	
	public Download(List<Peer> peers, TorrentInfo2 ti, 
			boolean[] clientPieces,Tracker tracker){
		this.peers=peers;
		useablePeers= new ArrayList<Peer>();
		this.ti=ti;
		this.tracker=tracker;
		if(clientPieces==null){
			clientPieces = new boolean[ti.total_pieces];
		}
		if(!areAllTrue(clientPieces)){
			FindPeersToDownloadFrom();
		}
	}
	
	/**
	 * Tells one peer to download pieces
	 */
	public void onePeerDownload(){
		//Stating to download here
		System.out.println("Num blocks in piece: "+ti.num_blocks_in_piece+ " block size: "+ti.block_size);
		Peer p = peers.get(0);
		for(int i=0; i<ti.total_pieces;i++){
			p.blockListForPiece(i);
		}
		//Finished downloading here
		
		if(p.getPiecesRecieved()==ti.total_pieces-1)
			//Peer has all the pieces
			tracker.updateTracker(ti.file_length);
		else
			//At least one piece missing
			tracker.updateTracker(p.getPiecesRecieved()*ti.piece_length);
		//Close all peer connections 
		p.close();
	}
	
	/*
	 * Adds peers that can be used to download from to the list useablePeers 
	 */
	public void FindPeersToDownloadFrom(){
		for(int i=0;peers.size()>i&&useablePeers.size()<=0;i++){
			if(peers.get(i).sendIntrested()==true){
				useablePeers.add(peers.get(i));
				System.out.println("peer added to usablepeers");
			}
		}
		findRarity();
	}
	
	public void setNewPeerList(List<Peer> peers,boolean[] clientPieces){
		this.peers=peers;
		findRarity();
	}
	
	private void findRarity(){
		a = new int[ti.total_pieces][2];
		System.out.println("Pieces:  " + ti.total_pieces);
		Map<Integer, Integer> rare = new HashMap<Integer,Integer>(ti.total_pieces);
			for(Peer p : useablePeers){
				System.out.println("Current peers bitfield:   "+ Arrays.toString(p.bitField) + p.peerId);
				for(int i=0; i<ti.total_pieces;i++){
					if(p.bitField!= null && p.bitField[i]==true){
						a[i][0]=i;	
						a[i][1]++;
					}
				}
			}
			
			System.out.println("Arraysss" +a[0][0]+"="+a[0][1]+" "+a[1][0]+"="+a[1][1]+" "+a[2][0]+"="+a[2][1]+" "+a[3][0]+"="+a[3][1]+" "+a[4][0]+"="+a[4][1]+" "+a[5][0]+"="+a[5][1]+" "+a[6][0]+"="+a[6][1]);
			
			//Sort array to have the lowest seeded piece at the first array's dimension's lowest index
			Arrays.sort(a, new Comparator<int[]>() {
			    @Override
			    public int compare(int[] o1, int[] o2) {
			    	//TODO randomly place the pieces with equal rarity
			        return o1[1]-o2[1];
			    }
			});
			
			System.out.println("Arraysss" +a[0][0]+"="+a[0][1]+" "+a[1][0]+"="+a[1][1]+" "+a[2][0]+"="+a[2][1]+" "+a[3][0]+"="+a[3][1]+" "+a[4][0]+"="+a[4][1]+" "+a[5][0]+"="+a[5][1]+" "+a[6][0]+"="+a[6][1]);		
	}

	public static boolean areAllTrue(boolean[] array)
	{
	    for(boolean b : array) if(!b) return false;
	    return true;
	}
}
