/**
* Message class that all other messages inherit from
*
* @author Philip Rego, Jess Calabretta, Jane Mehlig 
*  
*/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Message {

	public static final Message INTERESTED = new Message(1, (byte) 2);
	public static final Message CHOKE = new Message(1, (byte) 0);
	public static final Message UNCHOKE = new Message(1, (byte) 1);
	public static final Message UNINTERESTED = new Message(1, (byte) 3);
	public static final Message KEEP_ALIVE = new Message(0, (byte) 0);

	protected final int length;
	protected final byte id;

	protected Message(int length, byte id) {
		this.length = length;
		this.id = id;
	}

	public void send(DataOutputStream dos) throws IOException {

		dos.writeInt(this.length);
		if (length > 0) {
			dos.writeByte(this.id);
		}
		dos.flush();
	}

	public static Message read(DataInputStream din) throws IOException {
		int length = din.readInt();
		if (length == 0) {
			return Message.KEEP_ALIVE;
		}
		byte id = din.readByte();
		switch (id) {
		case 0:
			return Message.CHOKE;
		case 1:
			return Message.UNCHOKE;
		case 2:
			return Message.INTERESTED;
		case 3:
			return Message.UNINTERESTED;
		case 4:
			int index = din.readInt();
			HaveMessage HaveMess = new HaveMessage(5,(byte)4,index);
			return HaveMess;
		case 5:
			byte[] bits = new byte[length-1];	
			din.readFully(bits);			//TODO Does this read what was already read?
			return new BitfieldMessage(length,(byte)5,bits);
		case 6:
			return new RequestMessage(13,(byte)6,din.readInt(),din.readInt(),din.readInt());
		case 7:
			int pIndex = din.readInt();
			int bOffset = din.readInt();
			byte[] bits2 = new byte[length-9];
			din.readFully(bits2);			//TODO Does this read what was already read?
			return new PieceMessage(length,(byte)7,pIndex,bOffset,bits2);
		case 8:
			//Cancel message same as request just different id
			return new RequestMessage(13,(byte)8,din.readInt(),din.readInt(),din.readInt());
		default:
			throw new IOException("Unknown message type: " + id);

		}
	}
	public int getLength(){
		return length;
	}
	public int getId(){
		return (int) id;
	}
}
