import java.io.DataOutputStream;
import java.io.IOException;

/**
* Have message
*
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

public class HaveMessage extends Message{

	public int index;
	
	protected HaveMessage(int length, byte id, int index) {
		super(length, id);
		this.index = index;
	}
	
	public void send(DataOutputStream dos) throws IOException {

		dos.writeInt(super.length);
		if (length > 0) {
			dos.writeByte(super.id);
		}
		dos.writeInt(index);
		dos.flush();
	}

	
}
