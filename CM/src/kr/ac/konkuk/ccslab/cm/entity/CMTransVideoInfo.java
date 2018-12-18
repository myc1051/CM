package kr.ac.konkuk.ccslab.cm.entity;

import java.nio.channels.SelectableChannel;

public class CMTransVideoInfo extends Object {
	protected String m_strSenderName;	// the sender name
	protected String m_strReceiverName;// the receiver name
	protected String m_strVideoName; // the name of the transferred Video
	protected String m_strVideoPath;	// the local full path to the sent or received Video
	protected long m_lVideoSize;	  // the size of the transferred Video
	protected int m_nContentID;	  // the identifier of content to which the transferred Video belongs
	protected SelectableChannel m_defaultChannel;	// default socket channel (used for multiple channels)
	
	public CMTransVideoInfo()
	{
		m_strSenderName = "?";
		m_strReceiverName = "?";
		m_strVideoName = "?";
		m_strVideoPath = "?";
		m_lVideoSize = -1;
		m_nContentID = -1;
		m_defaultChannel = null;
	}
	
	public CMTransVideoInfo(String strVideo, long lSize, int nID)
	{
		m_strSenderName = "?";
		m_strReceiverName = "?";
		m_strVideoName = strVideo;
		m_strVideoPath = "?";
		m_lVideoSize = lSize;
		m_nContentID = nID;
		m_defaultChannel = null;
	}
	
	@Override
	public boolean equals(Object o)
	{
		CMTransVideoInfo tfInfo = (CMTransVideoInfo) o;
		String strVideoName = tfInfo.getVideoName();
		int nContentID = tfInfo.getContentID();
		
		if(strVideoName.equals(m_strVideoName) && nContentID == m_nContentID)
			return true;
		
		return false;	
	}
	
	@Override
	public String toString()
	{
		String strInfo = "CMTransVideoInfo: Video("+m_strVideoName+"), content ID("+m_nContentID+")";
		return strInfo;
	}
	
	// get/set methods
	
	public void setSenderName(String strName)
	{
		m_strSenderName = strName;
		return;
	}
	
	public String getSenderName()
	{
		return m_strSenderName;
	}
	
	public void setReceiverName(String strName)
	{
		m_strReceiverName = strName;
		return;
	}
	
	public String getReceiverName()
	{
		return m_strReceiverName;
	}

	public void setVideoName(String strName)
	{
		m_strVideoName = strName;
		return;
	}
	
	public String getVideoName()
	{
		return m_strVideoName;
	}
	
	public void setVideoPath(String strPath)
	{
		m_strVideoPath = strPath;
		return;
	}
	
	public String getVideoPath()
	{
		return m_strVideoPath;
	}
	
	public void setVideoSize(long lSize)
	{
		m_lVideoSize = lSize;
		return;
	}
	
	public long getVideoSize()
	{
		return m_lVideoSize;
	}
	
	public void setContentID(int nID)
	{
		m_nContentID = nID;
		return;
	}
	
	public int getContentID()
	{
		return m_nContentID;
	}
	
	public void setDefaultChannel(SelectableChannel channel)
	{
		m_defaultChannel = channel;
		return;
	}
	
	public SelectableChannel getDefaultChannel()
	{
		return m_defaultChannel;
	}
		
}
