package kr.ac.konkuk.ccslab.cm.entity;

import java.io.RandomAccessFile;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Future;

public class CMRecvVideoInfo extends CMTransVideoInfo {
	private long m_lRecvSize;
	private RandomAccessFile m_writeVideo;		// for writing the received Video block to the new Video
	private SocketChannel m_recvChannel;	// the dedicated channel for receiving the Video
	private Future<CMRecvVideoInfo> m_recvTaskResult;	// the result of the submitted receiving task to the thread pool 

	public CMRecvVideoInfo()
	{
		super();
		m_lRecvSize = -1;
		m_writeVideo = null;
		m_recvChannel = null;
		m_recvTaskResult = null;
	}
	
	public CMRecvVideoInfo(String strVideo, long lSize)
	{
		super(strVideo, lSize, -1);
		m_lRecvSize = -1;
		m_writeVideo = null;
		m_recvChannel = null;
		m_recvTaskResult = null;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!super.equals(o)) return false;
		
		CMRecvVideoInfo rfInfo = (CMRecvVideoInfo) o;
		String strSenderName = rfInfo.getSenderName();
		
		if(strSenderName.equals(m_strSenderName))
			return true;
		return false;
	}

	@Override
	public String toString()
	{
		String str = super.toString();
		str += "; CMRecvVideoInfo: sender("+m_strSenderName+")";
		return str;
	}
	
	// set/get methods
	
	public void setRecvSize(long lSize)
	{
		m_lRecvSize = lSize;
		return;
	}
	
	public long getRecvSize()
	{
		return m_lRecvSize;
	}
	
	public void setWriteVideo(RandomAccessFile acf)
	{
		m_writeVideo = acf;
	}
	
	public RandomAccessFile getWriteVideo()
	{
		return m_writeVideo;
	}
	
	public void setRecvChannel(SocketChannel channel)
	{
		m_recvChannel = channel;
		return;
	}
	
	public SocketChannel getRecvChannel()
	{
		return m_recvChannel;
	}
	
	public void setRecvTaskResult(Future<CMRecvVideoInfo> result)
	{
		m_recvTaskResult = result;
		return;
	}
	
	public Future<CMRecvVideoInfo> getRecvTaskResult()
	{
		return m_recvTaskResult;
	}
	
}
