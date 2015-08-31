/**
* Extension of TorrentInfo with some more info
*
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

import java.nio.ByteBuffer;


public class TorrentInfo2 extends TorrentInfo {
	
	/**
	 * The number of pieces. ceil(file_length/piece_length) = total pieces = 18 
	 */
	public final int total_pieces;		
	/**
	 * The recommended block size 16384
	 */
	public final int block_size = 16384;
	/**
	 * The size of the last block in the piece
	 */
	public final int last_block_size;	//init
	/**
	 * The SHA-1 hashes of each piece of the file. 
	 */
	public final ByteBuffer[] piece_hashes;	//here
	
	/**
	 * The number of blocks in a given piece
	 */
	public final int num_blocks_in_piece;	//Math.ceil(piece_size/block_size);

	public TorrentInfo2(byte[] torrent_file_bytes) throws BencodingException {
		super(torrent_file_bytes);
		total_pieces = (int) Math.ceil(file_length/piece_length);
		 last_block_size = piece_length % block_size;
		 num_blocks_in_piece = (int) Math.ceil(piece_length/block_size);
		
		// Extract the piece hashes from the info dictionary
		ByteBuffer all_hashes = (ByteBuffer)this.info_map.get(TorrentInfo.KEY_PIECES);
		if(all_hashes == null)
			throw new BencodingException("Could not extract piece hashes from info dictionary.  Corrupt file?");
		byte[] all_hashes_array = all_hashes.array();

		// Verify that the length of the array is a multiple of 20 bytes (160 bits)
		if(all_hashes_array.length % 20 != 0)
			throw new BencodingException("Piece hashes length is not a multiple of 20.  Corrupt file?");
		int num_pieces = all_hashes_array.length / 20;

		// Copy the values of the piece hashes into the local field
		this.piece_hashes = new ByteBuffer[num_pieces];
		for(int i = 0; i < num_pieces; i++)
		{
			byte[] temp_buff = new byte[20];
			System.arraycopy(all_hashes_array,i*20,temp_buff,0,20);
			this.piece_hashes[i] = ByteBuffer.wrap(temp_buff);
		}
	}
}
