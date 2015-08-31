

import java.io.IOException;

public class InvalidBEncodingException extends IOException
{
  public InvalidBEncodingException(String message)
  {
    super(message);
  }
}
