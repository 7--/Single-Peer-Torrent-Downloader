/**
* Request Message
*
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

import java.io.DataOutputStream;
import java.io.IOException;

public class RequestMessage extends Message {
	
	public int pIndex, bOffset, bLength;
	
	protected RequestMessage(int length, byte id, int pIndex, int bOffset, int bLength){
		super(length, id);
		this.pIndex = pIndex;
		this.bOffset = bOffset;
		this.bLength = bLength;
	}
	public void send(DataOutputStream dos) throws IOException {

		dos.writeInt(super.length);
		if (length > 0) {
			dos.writeByte(super.id);
		}
		dos.writeInt(pIndex);
		dos.writeInt(bOffset);
		dos.writeInt(bLength);
		dos.flush();
	}
	
}
