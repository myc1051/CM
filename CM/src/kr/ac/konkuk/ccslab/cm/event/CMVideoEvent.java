package kr.ac.konkuk.ccslab.cm.event;
import java.nio.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMVideoEvent extends CMEvent{
	
	// events for the Video transfer with the default channel
	public static final int REQUEST_VIDEO_STREAMING = 1;		// c -> s
	public static final int REPLY_VIDEO_STREAMING = 2;		// s -> c
	public static final int START_VIDEO_STREAMING = 3;		// s -> c
	public static final int START_VIDEO_STREAMING_ACK = 4;	// c -> s
	public static final int CONTINUE_VIDEO_STREAMING = 5;		// s -> c
	public static final int CONTINUE_VIDEO_STREAMING_ACK = 6;	// c -> s
	public static final int END_VIDEO_STREAMING = 7;			// s -> c
	public static final int END_VIDEO_STREAMING_ACK = 8;		// c -> s
	public static final int REQUEST_DIST_VIDEO_PROC = 9;		// c -> s (for distributed Video processing)
	
	// events for the Video transfer with the separate channel and thread
	public static final int REQUEST_VIDEO_STREAMING_CHAN = 10;		// c -> s
	public static final int REPLY_VIDEO_STREAMING_CHAN = 11;		// s -> c
	public static final int START_VIDEO_STREAMING_CHAN = 12;		// s -> c
	public static final int START_VIDEO_STREAMING_CHAN_ACK = 13;	// c -> s
	public static final int CONTINUE_VIDEO_STREAMING_CHAN = 14;		// s -> c
	public static final int CONTINUE_VIDEO_STREAMING_CHAN_ACK = 15;	// c -> s
	public static final int END_VIDEO_STREAMING_CHAN = 16;			// s -> c
	public static final int END_VIDEO_STREAMING_CHAN_ACK = 17;		// c -> s

	
	private String m_strUserName;	// target name
	private String m_strSenderName;	// sender name
	private String m_strVideoName;
	private long m_lVideoSize;
	private long m_lReceivedVideoSize;
	private int m_nReturnCode;
	private byte[] m_cVideoBlock;
	private int m_nBlockSize;
	private int m_nContentID;	// associated content ID (a Video as an attachment of SNS content)
	
	public CMVideoEvent()
	{
		m_nType = CMInfo.CM_VIDEO_EVENT;
		m_nID = -1;
		m_strUserName = "?";
		m_strSenderName = "?";
		m_strVideoName = "?";
		m_lVideoSize = 0;
		m_lReceivedVideoSize = 0;
		m_nReturnCode = -1;
		m_nBlockSize = -1;
		m_cVideoBlock = new byte[CMInfo.VIDEO_BLOCK_LEN];
		m_nContentID = -1;
	}
	
	public CMVideoEvent(ByteBuffer msg)
	{
		m_nType = CMInfo.CM_VIDEO_EVENT;
		m_nID = -1;
		m_strUserName = "?";
		m_strSenderName = "?";
		m_strVideoName = "?";
		m_lVideoSize = 0;
		m_lReceivedVideoSize = 0;
		m_nReturnCode = -1;
		m_nBlockSize = -1;
		m_cVideoBlock = new byte[CMInfo.VIDEO_BLOCK_LEN];
		m_nContentID = -1;
		
		unmarshallHeader(msg);
		unmarshallBody(msg);
	}
	
	public CMVideoEvent unmarshall(ByteBuffer msg)
	{
		unmarshallHeader(msg);
		unmarshallBody(msg);
		
		return this;
	}
	
	// set/get methods
	public void setUserName(String uName)
	{
		m_strUserName = uName;
	}
	public String getUserName()
	{
		return m_strUserName;
	}
	
	public void setSenderName(String sName)
	{
		m_strSenderName = sName;
	}
	
	public String getSenderName()
	{
		return m_strSenderName;
	}
	
	public void setVideoName(String fName)
	{
		m_strVideoName = fName;
	}
	
	public String getVideoName()
	{
		return m_strVideoName;
	}
	
	public void setVideoSize(long fSize)
	{
		m_lVideoSize = fSize;
	}
	
	public long getVideoSize()
	{
		return m_lVideoSize;
	}
	
	public void setReceivedVideoSize(long fSize)
	{
		m_lReceivedVideoSize = fSize;
	}
	
	public long getReceivedVideoSize()
	{
		return m_lReceivedVideoSize;
	}
	
	public void setReturnCode(int code)
	{
		m_nReturnCode = code;
	}
	
	public int getReturnCode()
	{
		return m_nReturnCode;
	}
	
	public void setVideoBlock(byte[] fBlock)
	{
		System.arraycopy(fBlock, 0, m_cVideoBlock, 0, CMInfo.VIDEO_BLOCK_LEN);
	}
	
	public byte[] getVideoBlock()
	{
		return m_cVideoBlock;
	}
	
	public void setBlockSize(int bSize)
	{
		m_nBlockSize = bSize;
	}
	
	public int getBlockSize()
	{
		return m_nBlockSize;
	}
	
	public void setContentID(int id)
	{
		m_nContentID = id;
	}
	
	public int getContentID()
	{
		return m_nContentID;
	}
	
	//////////////////////////////////////////////////////////
	
	protected int getByteNum()
	{		
		int nByteNum = 0;
		nByteNum = super.getByteNum();
		
		switch(m_nID)
		{
		case REQUEST_VIDEO_STREAMING:
		case REQUEST_VIDEO_STREAMING_CHAN:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strVideoName.getBytes().length;
			break;
		case REPLY_VIDEO_STREAMING:
		case REPLY_VIDEO_STREAMING_CHAN:
			nByteNum += 3*Integer.BYTES + m_strVideoName.getBytes().length;
			break;
		case START_VIDEO_STREAMING:
		case START_VIDEO_STREAMING_CHAN:
			nByteNum += 3*Integer.BYTES + m_strSenderName.getBytes().length + m_strVideoName.getBytes().length
					+ Long.BYTES;
			break;
		case START_VIDEO_STREAMING_ACK:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strVideoName.getBytes().length;
			break;
		case START_VIDEO_STREAMING_CHAN_ACK:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strVideoName.getBytes().length 
					+ Long.BYTES;
			break;
		case CONTINUE_VIDEO_STREAMING:
			nByteNum += 4*Integer.BYTES + m_strSenderName.getBytes().length + m_strVideoName.getBytes().length 
					+ CMInfo.VIDEO_BLOCK_LEN;
			break;
		case CONTINUE_VIDEO_STREAMING_ACK:
			nByteNum += 3*Integer.BYTES + m_strUserName.getBytes().length + m_strVideoName.getBytes().length 
					+ Long.BYTES;
			break;
		case END_VIDEO_STREAMING:
		case END_VIDEO_STREAMING_CHAN:
			nByteNum += 3*Integer.BYTES + m_strSenderName.getBytes().length + m_strVideoName.getBytes().length 
					+ Long.BYTES;
			break;
		case END_VIDEO_STREAMING_ACK:
		case END_VIDEO_STREAMING_CHAN_ACK:
			nByteNum += 4*Integer.BYTES + m_strUserName.getBytes().length + m_strVideoName.getBytes().length;
			break;
		case REQUEST_DIST_VIDEO_PROC:
			nByteNum += 2*Integer.BYTES + m_strUserName.getBytes().length;
			break;
		default:
			nByteNum = -1;
			break;
		}
		
		return nByteNum;
	}
	
	protected void marshallBody()
	{
		switch(m_nID)
		{
		case REQUEST_VIDEO_STREAMING:
		case REQUEST_VIDEO_STREAMING_CHAN:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case REPLY_VIDEO_STREAMING:
		case REPLY_VIDEO_STREAMING_CHAN:
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case START_VIDEO_STREAMING:
		case START_VIDEO_STREAMING_CHAN:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putLong(m_lVideoSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case START_VIDEO_STREAMING_ACK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case START_VIDEO_STREAMING_CHAN_ACK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.putLong(m_lReceivedVideoSize);
			m_bytes.clear();			
			break;
		case CONTINUE_VIDEO_STREAMING:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.putInt(m_nBlockSize);
			m_bytes.put(m_cVideoBlock);
			m_bytes.clear();
			break;
		case CONTINUE_VIDEO_STREAMING_ACK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putLong(m_lReceivedVideoSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case END_VIDEO_STREAMING:
		case END_VIDEO_STREAMING_CHAN:
			m_bytes.putInt(m_strSenderName.getBytes().length);
			m_bytes.put(m_strSenderName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putLong(m_lVideoSize);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case END_VIDEO_STREAMING_ACK:
		case END_VIDEO_STREAMING_CHAN_ACK:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_strVideoName.getBytes().length);
			m_bytes.put(m_strVideoName.getBytes());
			m_bytes.putInt(m_nReturnCode);
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		case REQUEST_DIST_VIDEO_PROC:
			m_bytes.putInt(m_strUserName.getBytes().length);
			m_bytes.put(m_strUserName.getBytes());
			m_bytes.putInt(m_nContentID);
			m_bytes.clear();
			break;
		default:
			System.out.println("CMVideoEvent.marshallBody(), unknown event id("+m_nID+").");
			m_bytes = null;
			break;
		}		
	}
	
	protected void unmarshallBody(ByteBuffer msg)
	{
		switch(m_nID)
		{
		case REQUEST_VIDEO_STREAMING:
		case REQUEST_VIDEO_STREAMING_CHAN:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case REPLY_VIDEO_STREAMING:
		case REPLY_VIDEO_STREAMING_CHAN:
			m_strVideoName = getStringFromByteBuffer(msg);
			m_nReturnCode = msg.getInt();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case START_VIDEO_STREAMING:
		case START_VIDEO_STREAMING_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_lVideoSize = msg.getLong();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case START_VIDEO_STREAMING_ACK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case START_VIDEO_STREAMING_CHAN_ACK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_lReceivedVideoSize = msg.getLong();
			msg.clear();
			break;
		case CONTINUE_VIDEO_STREAMING:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			m_nBlockSize = msg.getInt();
			msg.get(m_cVideoBlock);
			msg.clear();
			break;
		case CONTINUE_VIDEO_STREAMING_ACK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_lReceivedVideoSize = msg.getLong();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case END_VIDEO_STREAMING:
		case END_VIDEO_STREAMING_CHAN:
			m_strSenderName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_lVideoSize = msg.getLong();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case END_VIDEO_STREAMING_ACK:
		case END_VIDEO_STREAMING_CHAN_ACK:
			m_strUserName = getStringFromByteBuffer(msg);
			m_strVideoName = getStringFromByteBuffer(msg);
			m_nReturnCode = msg.getInt();
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		case REQUEST_DIST_VIDEO_PROC:
			m_strUserName = getStringFromByteBuffer(msg);
			m_nContentID = msg.getInt();
			msg.clear();
			break;
		default:
			System.out.println("CMVideoEvent.unmarshallBody(), unknown event id("+m_nID+").");
			break;
		}		
		
	}
}
