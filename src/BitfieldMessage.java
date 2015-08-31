/**
* BitField Message
*
* @author  Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

public class BitfieldMessage extends Message {

	public byte[] bitfield;
	
	protected BitfieldMessage(int length, byte id, byte[] bitfield) {
		super(length, id);
		this.bitfield = bitfield;
	}

}
