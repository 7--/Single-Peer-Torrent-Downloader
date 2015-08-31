/**
* PieceMessage
*
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

public class PieceMessage extends Message{

	//piece: <len=0009+X><id=7><index><begin><block>
	int pIndex, bOffset; 
	byte[] block;
	
	protected PieceMessage(int length, byte id, int pIndex, int bOffset, byte[] block) {
		super(length, id);
		this.pIndex = pIndex;
		this.bOffset = bOffset;
		this.block = block;
	}
}
